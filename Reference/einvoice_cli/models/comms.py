"""
einvoice_cli.models.comms
=========================
通訊認證上下文 (comms) 模型與時效視窗校驗。
"""

import time
from typing import Self
from pydantic import BaseModel, Field, model_validator
from ..config import settings


class CarrierComms(BaseModel):
    """
    UPI 協定通訊上下文 (comms)。
    財政部要求 comms 內之 exp - iat 必須嚴格等於 40 秒。
    """
    iat: int = Field(..., description="發起時間戳 (Unix Epoch 秒)")
    exp: int = Field(..., description="到期時間戳 (通常為 iat + 40 秒)")
    liat: int = Field(..., description="登入時間戳 (Login Issued At)")
    appid: str = Field(..., description="客戶端 App ID (如 EINV6202512184519)")
    barcode: str = Field(..., description="手機條碼載具 (如 /8J722KT)")
    keyType: str = Field(default="2", description="密鑰體系類型代碼 (固定為 2)")

    @model_validator(mode="after")
    def validate_expiry_window(self) -> Self:
        window = self.exp - self.iat
        if window != 40:
            raise ValueError(f"無效的有效時間視窗: exp - iat = {window}s (必須嚴格為 40 秒)")
        return self

    @classmethod
    def create_fresh(
        cls,
        appid: str = settings.default_appid,
        barcode: str = settings.default_barcode,
        liat: int | None = None,
        window: int = 40,
        key_type: str = "2"
    ) -> "CarrierComms":
        """建構新鮮之通訊上下文"""
        now = int(time.time())
        actual_liat = liat if liat is not None else settings.default_liat
        return cls(
            iat=now,
            exp=now + window,
            liat=actual_liat,
            appid=appid,
            barcode=barcode,
            keyType=key_type
        )
