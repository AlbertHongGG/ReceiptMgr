"""
einvoice_cli.audit.logger
=========================
.runtime 審計日誌持久化引擎。
保證每次 API 執行均輸出格式為 "yyyymmdd_hhmmss_api名稱.json" 之獨立原生 JSON 檔案。
"""

import os
import json
import tempfile
from pathlib import Path
from datetime import datetime
from typing import Any
from ..config import RUNTIME_DIR
from ..models.audit import AuditRecord, AuditMetadata, AuditRequest, AuditResponse


class RuntimeLogger:
    """
    執行時審計記錄器。
    負責將 API 調用之完整中繼資料、原始請求及伺服器回應寫入 .runtime。
    """

    def __init__(self, target_dir: Path | str | None = None):
        self.target_dir = Path(target_dir) if target_dir else RUNTIME_DIR
        self._ensure_dir()

    def _ensure_dir(self) -> None:
        """確保目標目錄存在"""
        self.target_dir.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def generate_filename(api_name: str, dt: datetime | None = None) -> str:
        """
        生成規格命名：yyyymmdd_hhmmss_api名稱.json
        """
        now = dt or datetime.now()
        timestamp_prefix = now.strftime("%Y%m%d_%H%M%S")
        clean_api_name = api_name.strip("/").replace("/", "-")
        return f"{timestamp_prefix}_{clean_api_name}.json"

    def record(self, record: AuditRecord) -> Path:
        """
        原子化寫入審計記錄至 .runtime。
        保證 Request/Response 完整維持巢狀原生 JSON 結構 (絕非跳脫字串)。
        """
        self._ensure_dir()
        filename = f"{record.metadata.run_id}.json"
        target_path = self.target_dir / filename

        # 轉為原生 Python dict 結構
        data_dict = record.model_dump(mode="json")

        # 原子寫入：先寫入同目錄暫存檔，完成後原子重命名，防止寫入中斷產生損壞檔
        tmp_fd, tmp_path = tempfile.mkstemp(
            prefix=".tmp_audit_",
            suffix=".json",
            dir=self.target_dir
        )
        try:
            with os.fdopen(tmp_fd, "w", encoding="utf-8") as f:
                json.dump(data_dict, f, indent=2, ensure_ascii=False)
            os.replace(tmp_path, target_path)
        except Exception:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)
            raise

        return target_path

    def create_and_record(
        self,
        api_name: str,
        action: str,
        endpoint: str,
        command: str,
        session_key_id: int | str,
        session_key_masked: str,
        elapsed_seconds: float,
        http_status: int,
        success: bool,
        jwt_header: dict[str, Any],
        jwt_payload: dict[str, Any],
        jwt_signature: str,
        raw_token: str | None,
        req_headers: dict[str, str],
        resp_headers: dict[str, str],
        resp_body: dict[str, Any] | list[Any] | None,
        error_message: str | None = None,
        execution_time: datetime | None = None,
    ) -> Path:
        """
        快捷建構並持久化審計紀錄。
        檔名嚴格遵循: yyyymmdd_hhmmss_api名稱.json
        """
        now = execution_time or datetime.now()
        clean_api_name = api_name.strip("/").split("/")[-1]
        run_id = f"{now.strftime('%Y%m%d_%H%M%S')}_{clean_api_name}"

        metadata = AuditMetadata(
            run_id=run_id,
            timestamp_iso=now.isoformat(),
            command=command,
            endpoint=endpoint,
            api_name=clean_api_name,
            action=action,
            session_key_id=session_key_id,
            session_key_masked=session_key_masked,
            elapsed_seconds=round(elapsed_seconds, 3),
            http_status=http_status,
            success=success,
            error_message=error_message,
        )

        request_data = AuditRequest(
            url=endpoint,
            method="POST",
            headers=req_headers,
            jwt_header=jwt_header,
            jwt_payload=jwt_payload,
            jwt_signature=jwt_signature,
            raw_token=raw_token,
        )

        response_data = AuditResponse(
            http_status=http_status,
            headers=resp_headers,
            body=resp_body,
        )

        audit_record = AuditRecord(
            metadata=metadata,
            request=request_data,
            response=response_data,
        )

        return self.record(audit_record)
