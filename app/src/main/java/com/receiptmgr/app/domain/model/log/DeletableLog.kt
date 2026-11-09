package com.receiptmgr.app.domain.model.log

import java.io.File

interface DeletableLog {
    val id: String
    val file: File
    val timestampMillis: Long
}
