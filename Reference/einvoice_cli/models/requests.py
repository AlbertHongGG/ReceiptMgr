"""
einvoice_cli.models.requests
============================
發票表頭清單 (carrierInvChk) 與品項明細 (carrierInvDetail) 請求參數模型。
"""

import re
from typing import Literal, Self
from pydantic import BaseModel, Field, model_validator


DATE_REGEX = re.compile(r"^\d{4}/(0[1-9]|1[0-2])/(0[1-9]|[12]\d|3[01])$")
INV_NUM_REGEX = re.compile(r"^[A-Z]{2}\d{8}$")


class InvoiceHeaderReqData(BaseModel):
    """
    發票表頭/清單查詢業務參數 (carrierInvChk)。
    """
    version: str = Field(default="1.0", description="API 規格版本")
    cardType: str = Field(default="3J0002", description="載具卡別 (手機條碼為 3J0002)")
    cardNo: str = Field(..., description="載具卡號 (手機條碼，如 /8J722KT)")
    action: str = Field(default="carrierInvChk", description="API 動作代碼")
    startDate: str = Field(..., description="查詢起始日期 YYYY/MM/DD")
    endDate: str = Field(..., description="查詢結束日期 YYYY/MM/DD")
    onlyWinningInv: Literal["Y", "N", "A"] = Field(
        default="A",
        description="中獎發票篩選 ('Y': 僅中獎, 'N': 僅未中獎, 'A': 全部)"
    )
    page: int = Field(default=1, ge=1, description="查詢頁碼 (從 1 開始)")

    @model_validator(mode="after")
    def validate_dates(self) -> Self:
        if not DATE_REGEX.match(self.startDate):
            raise ValueError(f"startDate 格式錯誤: '{self.startDate}'，預期格式為 YYYY/MM/DD")
        if not DATE_REGEX.match(self.endDate):
            raise ValueError(f"endDate 格式錯誤: '{self.endDate}'，預期格式為 YYYY/MM/DD")
        if self.startDate > self.endDate:
            raise ValueError(f"startDate ({self.startDate}) 不能晚於 endDate ({self.endDate})")
        return self


class InvoiceDetailReqData(BaseModel):
    """
    發票消費品項明細查詢業務參數 (carrierInvDetail)。
    """
    version: str = Field(default="1.0", description="API 規格版本")
    cardType: str = Field(default="3J0002", description="載具卡別 (手機條碼為 3J0002)")
    cardNo: str = Field(..., description="載具卡號 (手機條碼，如 /8J722KT)")
    action: str = Field(default="carrierInvDetail", description="API 動作代碼")
    invNum: str = Field(..., description="發票字軌號碼 (2碼英文 + 8碼數字，如 GW38470545)")
    invDate: str = Field(..., description="發票開立日期 YYYY/MM/DD (如 2026/11/05)")

    @model_validator(mode="after")
    def validate_inputs(self) -> Self:
        if not INV_NUM_REGEX.match(self.invNum):
            raise ValueError(f"invNum 格式錯誤: '{self.invNum}'，預期為 2 碼大寫英文字母 + 8 碼數字")
        if not DATE_REGEX.match(self.invDate):
            raise ValueError(f"invDate 格式錯誤: '{self.invDate}'，預期格式為 YYYY/MM/DD")
        return self
