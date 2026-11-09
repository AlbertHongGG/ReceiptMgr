"""
einvoice_cli.crypto.signer
==========================
財政部 UPI einvoiceJwt 封包編解碼與 HMAC-SHA256 數位簽章引擎。
"""

import json
import base64
import hmac
import hashlib
from typing import Any


class EInvoiceJwtSigner:
    """
    UPI einvoiceJwt 協定安全引擎。
    負責標準 Base64URL 處理、緊湊 JSON 序列化與 HMAC-SHA256 簽名運算。
    """

    DEFAULT_HEADER = {
        "alg": "HS256",
        "typ": "JWT",
        "ver": "1.0"
    }

    @staticmethod
    def b64url_encode(data: bytes) -> str:
        """Base64URL 編碼 (去除 '=' 補齊字元)"""
        return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")

    @staticmethod
    def b64url_decode(s: str) -> bytes:
        """Base64URL 解碼 (自動計算並補齊 '=' 填充字元)"""
        padded = s + "=" * ((4 - len(s) % 4) % 4)
        return base64.urlsafe_b64decode(padded)

    @classmethod
    def serialize_json(cls, obj: Any) -> str:
        """產生無空格之標準緊湊 JSON 字串"""
        return json.dumps(obj, separators=(",", ":"), ensure_ascii=False)

    @classmethod
    def sign(
        cls,
        payload_dict: dict[str, Any],
        secret_key: bytes,
        header_dict: dict[str, Any] | None = None
    ) -> str:
        """
        以給定對稱密鑰 (Session Key) 生成標準 einvoiceJwt。
        """
        hdr = header_dict or cls.DEFAULT_HEADER
        hdr_json = cls.serialize_json(hdr)
        payload_json = cls.serialize_json(payload_dict)

        hdr_b64 = cls.b64url_encode(hdr_json.encode("utf-8"))
        payload_b64 = cls.b64url_encode(payload_json.encode("utf-8"))

        signing_input = f"{hdr_b64}.{payload_b64}".encode("ascii")
        sig_bytes = hmac.new(secret_key, signing_input, hashlib.sha256).digest()
        sig_b64 = cls.b64url_encode(sig_bytes)

        return f"{hdr_b64}.{payload_b64}.{sig_b64}"

    @classmethod
    def parse(cls, jwt_token: str) -> tuple[dict[str, Any], dict[str, Any], str]:
        """
        解析 einvoiceJwt 字串。
        回傳: (header_dict, payload_dict, signature_b64)
        """
        parts = jwt_token.strip().split(".")
        if len(parts) != 3:
            raise ValueError(f"無效的 JWT 結構: 預期 3 段由 '.' 分隔之字串，實得 {len(parts)} 段")

        header_bytes = cls.b64url_decode(parts[0])
        payload_bytes = cls.b64url_decode(parts[1])

        header = json.loads(header_bytes.decode("utf-8"))
        payload = json.loads(payload_bytes.decode("utf-8"))
        signature = parts[2]

        return header, payload, signature

    @classmethod
    def verify(cls, jwt_token: str, secret_key: bytes) -> bool:
        """驗證 JWT 簽章正確性"""
        try:
            parts = jwt_token.strip().split(".")
            if len(parts) != 3:
                return False
            signing_input = f"{parts[0]}.{parts[1]}".encode("ascii")
            expected_sig = cls.b64url_encode(
                hmac.new(secret_key, signing_input, hashlib.sha256).digest()
            )
            return hmac.compare_digest(parts[2], expected_sig)
        except Exception:
            return False
