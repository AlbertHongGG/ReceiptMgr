package com.receiptmgr.app.data.repository

import com.receiptmgr.app.data.storage.ExportRecordLocalDataSource
import com.receiptmgr.app.domain.model.export.ExportTarget
import com.receiptmgr.app.domain.repository.ExportRecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExportRecordRepositoryImpl(
    private val localDataSource: ExportRecordLocalDataSource,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ExportRecordRepository {

    private val _exportedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val exportedAt: StateFlow<Map<String, Long>> = _exportedAt.asStateFlow()

    init {
        scope.launch {
            val records = localDataSource.getAllExportRecords(ExportTarget.EQUITY_TRACKER)
            _exportedAt.value = records
        }
    }

    override suspend fun markExported(
        invNum: String,
        target: ExportTarget,
        timestamp: Long
    ) {
        localDataSource.upsertExportRecord(invNum, target, timestamp)
        if (target == ExportTarget.EQUITY_TRACKER) {
            _exportedAt.update { current ->
                current + (invNum to timestamp)
            }
        }
    }

    override fun isExported(invNum: String, target: ExportTarget): Boolean {
        return if (target == ExportTarget.EQUITY_TRACKER) {
            _exportedAt.value.containsKey(invNum)
        } else {
            false
        }
    }

    override suspend fun clearAll() {
        localDataSource.clearAll()
        _exportedAt.value = emptyMap()
    }
}
