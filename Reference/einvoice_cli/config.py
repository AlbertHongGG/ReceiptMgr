"""
einvoice_cli.config
===================
全域組態設定與環境路徑管理。
所有路徑皆以 Reference 工具目錄與 ReceiptMgr 專案根目錄為基準，完全封閉獨立。
"""

import os
from pathlib import Path
from pydantic import BaseModel, Field

# 專案路徑推導
# Reference/einvoice_cli/config.py -> Reference -> ReceiptMgr
PACKAGE_DIR = Path(__file__).resolve().parent
REFERENCE_DIR = PACKAGE_DIR.parent
PROJECT_ROOT = REFERENCE_DIR.parent

# 唯一合法之 .env 設定檔路徑 (限定於 Reference/.env)
ENV_FILE_PATH = REFERENCE_DIR / ".env"

# 審計日誌目錄 (.runtime 位於 ReceiptMgr 專案根目錄)
RUNTIME_DIR = PROJECT_ROOT / ".runtime"


class Settings(BaseModel):
    """應用程式設定模型"""
    upi_host: str = Field(
        default="https://upi.einvoice.nat.gov.tw",
        description="財政部電子發票雲端服務 (UPI) 基礎網址"
    )
    header_endpoint: str = Field(
        default="/einvoice/carriers/query-invoices-header",
        description="發票表頭清單查詢 API 端口"
    )
    detail_endpoint: str = Field(
        default="/einvoice/carriers/query-invoices-details",
        description="單筆發票明細查詢 API 端口"
    )
    user_agent: str = Field(
        default="okhttp/5.3.0",
        description="模擬客戶端 User-Agent"
    )
    default_appid: str = Field(
        default="EINV6202512184519",
        description="預設客戶端 App ID"
    )
    default_barcode: str = Field(
        default="/8J722KT",
        description="預設手機條碼"
    )
    default_card_type: str = Field(
        default="3J0002",
        description="手機條碼載具類別代碼 (3J0002)"
    )
    default_liat: int = Field(
        default=1794134995,
        description="預設登入時間戳 (Login Issued At)"
    )
    jwt_expiry_seconds: int = Field(
        default=40,
        description="UPI JWT Nonce 嚴格有效視窗 (秒)"
    )
    timeout_seconds: float = Field(
        default=10.0,
        description="HTTP 連線與讀取逾時時間 (秒)"
    )
    runtime_dir: Path = Field(
        default=RUNTIME_DIR,
        description=".runtime 審計檔案目錄"
    )
    env_file: Path = Field(
        default=ENV_FILE_PATH,
        description="唯一限定讀取之 .env 檔案路徑"
    )

    class Config:
        arbitrary_types_allowed = True


# 全域單例組態
settings = Settings()
