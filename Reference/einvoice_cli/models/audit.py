"""
einvoice_cli.models.audit
=========================
.runtime 審計日誌之嚴格資料契約。
保證 request 與 response 保留原始原生 JSON 架構，杜絕雙重字串序列化。
"""

from typing import Any
from pydantic import BaseModel, Field


class AuditMetadata(BaseModel):
    """執行審計基本中繼資料"""
    run_id: str = Field(..., description="執行識別碼 (yyyymmdd_hhmmss_api名稱)")
    timestamp_iso: str = Field(..., description="執行時間 ISO-8601 字串")
    command: str = Field(..., description="觸發之 CLI 指令或調用方式")
    endpoint: str = Field(..., description="呼叫之 API 完整端點 URL")
    api_name: str = Field(..., description="API 規格名稱 (如 query-invoices-header)")
    action: str = Field(..., description="業務動作代碼 (carrierInvChk / carrierInvDetail)")
    session_key_id: int | str = Field(default=1, description="所採用之 Session Key 識別 (1 或 2)")
    session_key_masked: str = Field(..., description="掩蔽後之 Session Key (例: VsE82...Tq9g)")
    elapsed_seconds: float = Field(..., description="API 請求耗時 (秒)")
    http_status: int = Field(..., description="HTTP 狀態碼")
    success: bool = Field(..., description="請求是否成功")
    error_message: str | None = Field(default=None, description="失敗時之錯誤描述")


class AuditRequest(BaseModel):
    """API 請求詳細資料 (原生 JSON 架構)"""
    url: str = Field(..., description="請求目標 URL")
    method: str = Field(default="POST", description="HTTP 方法")
    headers: dict[str, str] = Field(default_factory=dict, description="HTTP 請求標頭")
    jwt_header: dict[str, Any] = Field(..., description="JWT 表頭原始物件 (alg, typ, ver)")
    jwt_payload: dict[str, Any] = Field(..., description="JWT 內容原始物件 (comms, reqdata 原生字典)")
    jwt_signature: str = Field(..., description="計算出之 HMAC-SHA256 簽名 (Base64URL)")
    raw_token: str | None = Field(default=None, description="完整 einvoiceJwt 字串")


class AuditResponse(BaseModel):
    """API 回應詳細資料 (原生 JSON 架構)"""
    http_status: int = Field(..., description="HTTP 狀態碼")
    headers: dict[str, str] = Field(default_factory=dict, description="HTTP 回應標頭")
    body: dict[str, Any] | list[Any] | None = Field(
        ...,
        description="伺服器回傳之原生 JSON 資料物件 (非跳脫字串)"
    )


class AuditRecord(BaseModel):
    """
    完整審計日誌根模型。
    寫入 .runtime 時直接呼叫 model_dump(mode='json') 即可產出高保真之純淨 JSON。
    """
    metadata: AuditMetadata
    request: AuditRequest
    response: AuditResponse
