package com.receiptmgr.app.domain.model.auth

/**
 * 財政部官方 App 協定規格字典 (Target App Protocol Specification)
 * 嚴格對齊官方 classes2.dex 逆向提取之規格標準，非宿主 App 之版本與代碼。
 */
data class TargetAppSpec(
    val appId: String = DEFAULT_APP_ID,
    val appver: String = DEFAULT_TARGET_APP_VER,
    val appbn: Int = DEFAULT_TARGET_APP_BN,
    val lang: String = DEFAULT_LANG,
    val ccs: String = DEFAULT_CCS,
    val type: Int = DEFAULT_LOGIN_TYPE
) {
    companion object {
        const val DEFAULT_APP_ID = "EINV6202512184519"
        const val DEFAULT_TARGET_APP_VER = "6.800.11"
        const val DEFAULT_TARGET_APP_BN = 91
        const val DEFAULT_LANG = "zh"
        const val DEFAULT_CCS = "tw"
        const val DEFAULT_LOGIN_TYPE = 0

        /**
         * 官方合法 152 碼 Firebase Cloud Messaging (FCM) 推播權杖
         * 格式為: 22字元實例ID + ":APA91b" + 123字元Base64URL亂數
         */
        const val DEFAULT_PTOKEN =
            "cn69FHuITYq80ORYXwgHt-:APA91bFW966IJVtx6HPh8mEEtXmxtj51WbuwWaoGfcOLDqphN1vuvJ7mzjFaX4o_bl8dMCadCoKvCnLONNw9KUYrvYf4wpKSK2GVJg_whW7gUyL0LwHEa2k"
    }
}
