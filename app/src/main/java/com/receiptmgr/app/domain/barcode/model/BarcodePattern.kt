package com.receiptmgr.app.domain.barcode.model

/**
 * 條碼幾何模型值物件 (Value Object)。
 * 封裝條碼的原始內容、所有元素序列與模組總寬度。
 */
data class BarcodePattern(
    val rawContent: String,
    val elements: List<BarcodeElement>,
    val quietZoneUnits: Int = 10
) {
    /**
     * 包含左右靜區 (Quiet Zone) 在內的所有模組單位總數。
     */
    val totalUnits: Int = elements.sumOf { it.unitWidth } + (quietZoneUnits * 2)
}
