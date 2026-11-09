package com.receiptmgr.app.domain.barcode.model

/**
 * 代表條碼中的單一條 (Bar) 或空 (Space) 幾何元素。
 * @property isBar 是否為黑色條紋 (true = Bar 黑線, false = Space 白間隙)
 * @property unitWidth 此元素佔用的相對模組單位寬度 (窄元素為 1 單位，寬元素為 2 或 3 單位)
 */
data class BarcodeElement(
    val isBar: Boolean,
    val unitWidth: Int
)
