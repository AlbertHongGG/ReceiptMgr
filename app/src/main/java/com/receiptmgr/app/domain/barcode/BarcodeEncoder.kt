package com.receiptmgr.app.domain.barcode

import com.receiptmgr.app.domain.barcode.model.BarcodePattern

/**
 * 條碼編碼器抽象介面 (符合開閉原則 OCP)。
 */
interface BarcodeEncoder {
    /**
     * 檢查內容是否符合此條碼協定的合法格式。
     */
    fun isValid(content: String): Boolean

    /**
     * 將輸入字串編碼為幾何條碼模型。
     * @throws IllegalArgumentException 當輸入包含非法字元時拋出
     */
    fun encode(content: String): BarcodePattern
}
