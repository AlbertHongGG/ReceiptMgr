"""
einvoice_cli.client.client
==========================
財政部電子發票 (UPI 體系) API 客戶端核心通訊類別。
提供雙端口 query-invoices-header 與 query-invoices-details 之發送、簽署與自動審計日誌持久化。
"""

import time
import ssl
from typing import Any
import httpx

from ..config import settings
from ..crypto.signer import EInvoiceJwtSigner
from ..crypto.session import SessionKeyManager
from ..models.comms import CarrierComms
from ..models.requests import InvoiceHeaderReqData, InvoiceDetailReqData
from ..models.responses import InvoiceHeaderResponse, InvoiceDetailResponse
from ..audit.logger import RuntimeLogger
from .exceptions import EInvoiceNetworkError, EInvoiceSSLError, EInvoiceApiError


class EInvoiceClient:
    """
    財政部電子發票官方端口 API 客戶端。
    """

    def __init__(
        self,
        session_key: bytes | str,
        session_key_id: int | str = 1,
        base_url: str = settings.upi_host,
        ssl_verify: bool = False,
        timeout: float = settings.timeout_seconds,
        logger: RuntimeLogger | None = None,
        user_agent: str = settings.user_agent,
    ):
        self.session_key = (
            session_key.encode("utf-8") if isinstance(session_key, str) else session_key
        )
        self.session_key_id = session_key_id
        self.base_url = base_url.rstrip("/")
        self.ssl_verify = ssl_verify
        self.timeout = timeout
        self.logger = logger or RuntimeLogger()
        self.user_agent = user_agent
        self.last_audit_file: str | None = None

    def _execute_jwt_request(
        self,
        endpoint_path: str,
        api_name: str,
        action: str,
        req_data_dict: dict[str, Any],
        comms: CarrierComms | None = None,
        command_desc: str = "CLI Execution",
    ) -> dict[str, Any]:
        """
        統一 JWT 請求發送管線。
        負責自動補齊時效 comms、計算簽署、發送 HTTP POST 表單、捕捉回應與記錄原生 JSON 審計日誌。
        """
        target_url = f"{self.base_url}{endpoint_path}"

        # 若未提供 comms，依據 req_data_dict 自動生成標準 40 秒新鮮上下文
        if comms is None:
            barcode = req_data_dict.get("cardNo", settings.default_barcode)
            comms = CarrierComms.create_fresh(
                appid=settings.default_appid,
                barcode=barcode,
            )

        payload_dict = {
            "comms": comms.model_dump(),
            "reqdata": req_data_dict,
        }

        # 數位簽名
        jwt_token = EInvoiceJwtSigner.sign(
            payload_dict=payload_dict,
            secret_key=self.session_key,
        )
        hdr_dict, _, sig_str = EInvoiceJwtSigner.parse(jwt_token)

        headers = {
            "Host": "upi.einvoice.nat.gov.tw",
            "Content-Type": "application/x-www-form-urlencoded; charset=utf-8",
            "User-Agent": self.user_agent,
            "Accept-Encoding": "gzip, deflate",
        }
        form_data = {"einvoiceJwt": jwt_token}

        start_time = time.time()
        elapsed_sec = 0.0
        http_status = 0
        resp_headers: dict[str, str] = {}
        resp_body: dict[str, Any] | list[Any] | None = None
        error_msg: str | None = None
        is_success = False

        try:
            with httpx.Client(
                verify=self.ssl_verify,
                timeout=self.timeout,
                follow_redirects=True
            ) as client:
                response = client.post(
                    target_url,
                    data=form_data,
                    headers=headers,
                )
                elapsed_sec = time.time() - start_time
                http_status = response.status_code
                resp_headers = dict(response.headers)

                # 解析原生 JSON 回應
                try:
                    resp_body = response.json()
                except Exception:
                    resp_body = {"raw_text": response.text}

                if response.is_success:
                    # 檢查業務代碼是否為 200 (自動相容 carrierQueryList 包覆)
                    if isinstance(resp_body, dict):
                        target_dict = (
                            resp_body.get("carrierQueryList")
                            if isinstance(resp_body.get("carrierQueryList"), dict)
                            else resp_body
                        )
                        b_code = str(target_dict.get("code", ""))
                        if b_code == "200":
                            is_success = True
                        else:
                            error_msg = f"業務拒絕: code={b_code}, msg={target_dict.get('msg')}"
                    else:
                        is_success = True
                else:
                    error_msg = f"HTTP 狀態異常: {response.status_code}"

        except ssl.SSLCertVerificationError as e:
            elapsed_sec = time.time() - start_time
            error_msg = f"SSL 憑證校驗錯誤: {e}"
            resp_body = {"ssl_error": str(e)}
            # 寫入審計後拋出領域例外
            self._write_audit(
                api_name, action, target_url, command_desc, elapsed_sec,
                http_status, False, hdr_dict, payload_dict, sig_str,
                jwt_token, headers, resp_headers, resp_body, error_msg
            )
            raise EInvoiceSSLError(error_msg) from e

        except (httpx.RequestError, httpx.TimeoutException) as e:
            elapsed_sec = time.time() - start_time
            error_msg = f"網路通訊異常: {e}"
            resp_body = {"network_error": str(e)}
            self._write_audit(
                api_name, action, target_url, command_desc, elapsed_sec,
                http_status, False, hdr_dict, payload_dict, sig_str,
                jwt_token, headers, resp_headers, resp_body, error_msg
            )
            raise EInvoiceNetworkError(error_msg) from e

        # 正常寫入審計日誌
        audit_path = self._write_audit(
            api_name, action, target_url, command_desc, elapsed_sec,
            http_status, is_success, hdr_dict, payload_dict, sig_str,
            jwt_token, headers, resp_headers, resp_body, error_msg
        )
        self.last_audit_file = str(audit_path)

        if not isinstance(resp_body, dict):
            raise EInvoiceApiError(
                code=str(http_status),
                msg="回傳資料非標準 JSON 物件",
                raw_data={"body": resp_body}
            )

        return resp_body

    def _write_audit(
        self,
        api_name: str,
        action: str,
        endpoint: str,
        command: str,
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
        error_message: str | None,
    ):
        """將本次請求寫入 .runtime 原生 JSON 檔"""
        masked_key = SessionKeyManager.mask_key(self.session_key)
        return self.logger.create_and_record(
            api_name=api_name,
            action=action,
            endpoint=endpoint,
            command=command,
            session_key_id=self.session_key_id,
            session_key_masked=masked_key,
            elapsed_seconds=elapsed_seconds,
            http_status=http_status,
            success=success,
            jwt_header=jwt_header,
            jwt_payload=jwt_payload,
            jwt_signature=jwt_signature,
            raw_token=raw_token,
            req_headers=req_headers,
            resp_headers=resp_headers,
            resp_body=resp_body,
            error_message=error_message,
        )

    def query_headers(
        self,
        req_data: InvoiceHeaderReqData,
        comms: CarrierComms | None = None,
        command_desc: str = "CLI query header",
    ) -> InvoiceHeaderResponse:
        """
        端口 1：POST /einvoice/carriers/query-invoices-header
        查詢手機條碼載具之發票表頭清單。
        """
        raw_response = self._execute_jwt_request(
            endpoint_path=settings.header_endpoint,
            api_name="query-invoices-header",
            action=req_data.action,
            req_data_dict=req_data.model_dump(),
            comms=comms,
            command_desc=command_desc,
        )
        return InvoiceHeaderResponse.model_validate(raw_response)

    def query_details(
        self,
        req_data: InvoiceDetailReqData,
        comms: CarrierComms | None = None,
        command_desc: str = "CLI query details",
    ) -> InvoiceDetailResponse:
        """
        端口 2：POST /einvoice/carriers/query-invoices-details
        查詢單筆發票之消費品項明細清單。
        """
        raw_response = self._execute_jwt_request(
            endpoint_path=settings.detail_endpoint,
            api_name="query-invoices-details",
            action=req_data.action,
            req_data_dict=req_data.model_dump(),
            comms=comms,
            command_desc=command_desc,
        )
        return InvoiceDetailResponse.model_validate(raw_response)
