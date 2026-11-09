"""
einvoice_cli.client.exceptions
==============================
電子發票領域專屬例外階層。
"""

from typing import Any


class EInvoiceError(Exception):
    """電子發票客戶端基礎例外"""
    pass


class EInvoiceNetworkError(EInvoiceError):
    """網路連線、超時或傳輸層例外"""
    pass


class EInvoiceSSLError(EInvoiceNetworkError):
    """SSL 憑證校驗失敗例外"""
    pass


class EInvoiceApiError(EInvoiceError):
    """
    財政部伺服器回應業務錯誤 (code != 200)。
    包含伺服器原始錯誤代碼與錯誤訊息。
    """
    def __init__(self, code: str, msg: str, raw_data: dict[str, Any] | None = None):
        super().__init__(f"財政部 API 拒絕 [{code}]: {msg}")
        self.code = code
        self.msg = msg
        self.raw_data = raw_data or {}
