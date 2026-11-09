"""
einvoice_cli.models.responses
=============================
發票表頭清單與品項明細 API 回傳資料模型。
"""

from typing import Any
from pydantic import BaseModel, Field, ConfigDict


class BaseApiResponse(BaseModel):
    """API 基礎回傳格式"""
    model_config = ConfigDict(extra="allow")

    version: str = Field(default="1.0", description="規格版本")
    code: str = Field(..., description="回應代碼 (如 200 代表執行成功)")
    msg: str = Field(..., description="回應訊息 (如 執行成功)")

    @property
    def is_success(self) -> bool:
        return self.code == "200"


class AwardInfo(BaseModel):
    """中獎資訊模型"""
    model_config = ConfigDict(extra="allow")

    prizeType: str | None = Field(default=None, description="中獎獎別代碼")
    prizeAmt: str | None = Field(default=None, description="中獎金額字串")
    awardMk: str | None = Field(default=None, description="中獎註記 (1: 中獎)")
    awardDate: str | None = Field(default=None, description="開獎日期")
    depositMk: str | None = Field(default=None, description="獎金匯入註記")


class InvoiceHeaderItem(BaseModel):
    """單筆發票表頭資訊"""
    model_config = ConfigDict(extra="allow")

    rowNum: str = Field(..., description="序號")
    invPeriod: str = Field(..., description="發票期別 (民國年月，如 11506)")
    invNum: str = Field(..., description="發票字軌號碼 (如 BK22807399)")
    invoiceTime: str | None = Field(default=None, description="開立時間 (HH:MM:SS)")
    invStatus: str | None = Field(default=None, description="發票狀態 (如 開立已確認)")
    cardType: str | None = Field(default="3J0002", description="載具卡別")
    cardNo: str | None = Field(default=None, description="載具號碼")
    sellerBan: str | None = Field(default=None, description="賣方統一編號")
    sellerName: str | None = Field(default=None, description="賣方營業人名稱")
    sellerAddress: str | None = Field(default=None, description="賣方營業地址")
    buyerBan: str | None = Field(default=None, description="買方統一編號")
    invDonatable: bool | None = Field(default=None, description="是否可捐贈")
    amount: str = Field(default="0", description="發票總金額")
    currency: str | None = Field(default="", description="幣別")
    donateMark: str | None = Field(default="0", description="捐贈註記")
    invDate: dict[str, Any] | str | None = Field(default=None, description="開立日期時間詳細資訊")
    awardInfo: AwardInfo | dict[str, Any] | None = Field(default=None, description="中獎資訊")

    @property
    def formatted_inv_date(self) -> str:
        """格式化開立日期為 YYYY-MM-DD"""
        if isinstance(self.invDate, dict):
            y = self.invDate.get("year", "")
            m = str(self.invDate.get("month", "")).zfill(2)
            d = str(self.invDate.get("date", "")).zfill(2)
            if y and y.isdigit():
                ad_year = int(y) + 1911 if int(y) < 1000 else int(y)
                return f"{ad_year}-{m}-{d}"
        if isinstance(self.invDate, str):
            return self.invDate
        return ""


class InvoiceDetailItem(BaseModel):
    """發票品項明細單行資訊"""
    model_config = ConfigDict(extra="allow")

    rowNum: str = Field(..., description="品項序號")
    description: str = Field(..., description="品名描述")
    quantity: str = Field(..., description="購買數量")
    unitPrice: str = Field(..., description="單價")
    amount: str = Field(..., description="小計金額")


class InvoiceHeaderResponse(BaseApiResponse):
    """發票表頭清單查詢 API 完整回傳模型"""
    onlyWinningInv: str | None = Field(default=None, description="中獎篩選註記")
    details: list[InvoiceHeaderItem] = Field(default_factory=list, description="發票清單")

    @classmethod
    def model_validate(cls, obj: Any, *args, **kwargs) -> "InvoiceHeaderResponse":
        # 兼容財政部在非中獎篩選時外層包覆 carrierQueryList 之多型結構
        if isinstance(obj, dict) and "carrierQueryList" in obj:
            inner = obj["carrierQueryList"]
            if isinstance(inner, dict):
                return super().model_validate(inner, *args, **kwargs)
        return super().model_validate(obj, *args, **kwargs)


class InvoiceDetailResponse(BaseApiResponse):
    """單筆發票明細查詢 API 完整回傳模型"""
    invPeriod: str | None = Field(default=None, description="發票期別")
    invNum: str | None = Field(default=None, description="發票號碼")
    invDate: str | None = Field(default=None, description="開立日期 (YYYYMMDD)")
    invoiceTime: str | None = Field(default=None, description="開立時間 (HH:MM:SS)")
    invStatus: str | None = Field(default=None, description="發票狀態")
    sellerBan: str | None = Field(default=None, description="賣方統一編號")
    sellerName: str | None = Field(default=None, description="賣方營業人名稱")
    sellerAddress: str | None = Field(default=None, description="賣方營業地址")
    buyerBan: str | None = Field(default=None, description="買方統一編號")
    amount: str | None = Field(default=None, description="總金額")
    currency: str | None = Field(default="", description="幣別")
    details: list[InvoiceDetailItem] = Field(default_factory=list, description="品項明細清單")
