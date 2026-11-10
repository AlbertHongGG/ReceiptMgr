package com.receiptmgr.app.data.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerReceiptPayloadDto
import com.receiptmgr.app.domain.service.ReceiptTransport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 透過 Android Intent (ACTION_SEND 定向鎖定 package) 傳輸至 EquityTracker。
 */
class AndroidEquityTrackerTransport(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : ReceiptTransport {

    companion object {
        const val TARGET_PACKAGE = "com.jasonhong.equitytracker"
    }

    override fun send(payload: EquityTrackerReceiptPayloadDto): Boolean {
        return try {
            val jsonString = json.encodeToString(payload)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TEXT, jsonString)
                setPackage(TARGET_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
