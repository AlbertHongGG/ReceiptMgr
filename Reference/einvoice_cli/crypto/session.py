"""
einvoice_cli.crypto.session
===========================
Session Key 密鑰倉儲模組 (SessionKeyRepository)。
嚴格且唯一限定自 Reference/.env 讀取，完全隔離與外部目錄之相依。
"""

import os
from pathlib import Path
from typing import Any
from ..config import ENV_FILE_PATH


class SessionKeyRepository:
    """
    Session Key 密鑰倉儲。
    專責載入、檢驗、遮蔽自官方 APP 登入取得之對稱金鑰 (43 字元 Base64URL 字串)。
    """

    def __init__(self, keys: list[str] | None = None, source_path: Path | None = None):
        self._keys: list[str] = keys or []
        self._source_path: Path | None = source_path

    @property
    def keys(self) -> list[str]:
        return self._keys

    @property
    def source_path(self) -> Path | None:
        return self._source_path

    @classmethod
    def load(cls, custom_path: Path | str | None = None) -> "SessionKeyRepository":
        """
        載入 Session Keys。
        若未指定 custom_path，則嚴格僅從 Reference/.env 讀取。
        絕不搜尋或 Fallback 到任何外部逆向專案目錄。
        """
        target_path = Path(custom_path) if custom_path else ENV_FILE_PATH

        if not target_path.exists() or not target_path.is_file():
            return cls(keys=[], source_path=target_path)

        loaded_keys: list[str] = []

        try:
            with open(target_path, "r", encoding="utf-8") as f:
                for line in f:
                    cleaned = line.strip()
                    if not cleaned or cleaned.startswith("#"):
                        continue
                    # 支援 KEY=VALUE 格式
                    if "=" in cleaned:
                        k, val = cleaned.split("=", 1)
                        val = val.strip().strip("'\"")
                        if ("SESSION_KEY" in k.upper() or len(val) == 43) and val:
                            if val not in loaded_keys:
                                loaded_keys.append(val)
                    else:
                        # 支援直接純金鑰行
                        if len(cleaned) == 43 and cleaned not in loaded_keys:
                            loaded_keys.append(cleaned)
        except Exception as e:
            raise RuntimeError(f"讀取金鑰設定檔失敗 ({target_path}): {e}") from e

        return cls(keys=loaded_keys, source_path=target_path)

    def get_key(self, index_or_key: int | str = 1) -> bytes:
        """
        取得指定之 Session Key 位元組序列。
        支援傳入 1-based index (1, 2) 或直接傳入金鑰字串。
        """
        if isinstance(index_or_key, int):
            idx = index_or_key - 1
            if idx < 0 or idx >= len(self._keys):
                raise ValueError(
                    f"Session Key 編號 {index_or_key} 超出範圍，目前於 {self._source_path} 僅載入 {len(self._keys)} 把金鑰"
                )
            key_str = self._keys[idx]
        elif isinstance(index_or_key, str):
            if index_or_key.isdigit():
                return self.get_key(int(index_or_key))
            key_str = index_or_key.strip()
        else:
            raise TypeError("index_or_key 必須為整數編號或金鑰字串")

        if not key_str:
            raise ValueError("取得之 Session Key 為空")

        return key_str.encode("utf-8")

    def get_key_str(self, index_or_key: int | str = 1) -> str:
        """取得原始字串格式之金鑰"""
        return self.get_key(index_or_key).decode("utf-8")

    @staticmethod
    def mask_key(key: str | bytes) -> str:
        """產生遮蔽安全字串 (如: VsE82...pTq9g)"""
        s = key.decode("utf-8") if isinstance(key, bytes) else str(key)
        if len(s) <= 10:
            return "***"
        return f"{s[:5]}...{s[-5:]}"

    @staticmethod
    def validate_format(key: str | bytes) -> bool:
        """校驗金鑰格式是否為合法 43 字元 Base64URL"""
        s = key.decode("utf-8") if isinstance(key, bytes) else str(key)
        return len(s) == 43 and not any(c in s for c in "+/=")

    def list_keys_info(self) -> list[dict[str, Any]]:
        """列出當前所有金鑰之摘要資訊"""
        result = []
        for i, k in enumerate(self._keys, start=1):
            result.append({
                "id": i,
                "masked": self.mask_key(k),
                "length": len(k),
                "valid_format": self.validate_format(k),
            })
        return result


# 提供別名以維持乾淨的介面相容
SessionKeyManager = SessionKeyRepository
