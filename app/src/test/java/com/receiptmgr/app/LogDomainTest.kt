package com.receiptmgr.app

import com.receiptmgr.app.domain.model.log.ApiCategoryType
import com.receiptmgr.app.domain.model.log.ApiLogModel
import com.receiptmgr.app.domain.model.log.DefaultApiLogConfigs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LogDomainTest {

    @Test
    fun testApiCategoryType_fromEndpointClassification() {
        assertEquals(ApiCategoryType.AUTH, ApiCategoryType.fromEndpoint("POST /mid/v1/login"))
        assertEquals(ApiCategoryType.AUTH, ApiCategoryType.fromEndpoint("/APIService/authCheck"))
        assertEquals(ApiCategoryType.INVOICE_LIST, ApiCategoryType.fromEndpoint("/APIService/carrierInvChk"))
        assertEquals(ApiCategoryType.INVOICE_DETAIL, ApiCategoryType.fromEndpoint("/APIService/carrierInvDetail"))
        assertEquals(ApiCategoryType.CARRIER, ApiCategoryType.fromEndpoint("/APIService/carrierList"))
        assertEquals(ApiCategoryType.AWARD, ApiCategoryType.fromEndpoint("/APIService/awardList"))
        assertEquals(ApiCategoryType.ALL, ApiCategoryType.fromEndpoint("/APIService/unknownEndpoint"))
    }

    @Test
    fun testDefaultApiLogConfigs_containsDeterministicActiveTabs() {
        val tabs = DefaultApiLogConfigs.allTabs
        assertEquals(2, tabs.size)
        assertEquals("all", tabs[0].id)
        assertEquals("auth", tabs[1].id)

        tabs.forEach { tab ->
            assertTrue(tab.tabName.isNotBlank())
            assertTrue(tab.emptyStateTitle.isNotBlank())
            assertTrue(tab.emptyStateDescription.isNotBlank())
            assertNotNull(tab.tabIcon)
            assertNotNull(tab.emptyStateIcon)
        }
    }

    @Test
    fun testApiLogModel_lazyResolution() {
        val tempFile = File.createTempFile("test_log", ".json").apply {
            deleteOnExit()
            writeText("""{"status":200,"message":"ok"}""")
        }

        // 當 rawJson 已提供時，優先回傳 rawJson
        val modelWithMemory = ApiLogModel(
            id = "test_1",
            file = tempFile,
            title = "Test",
            endpoint = "/test",
            rawJson = """{"cached":true}"""
        )
        assertEquals("""{"cached":true}""", modelWithMemory.resolveRawJson())

        // 當 rawJson 為空時，延遲自磁碟讀取
        val modelLazy = ApiLogModel(
            id = "test_2",
            file = tempFile,
            title = "Test",
            endpoint = "/test",
            rawJson = ""
        )
        assertEquals("""{"status":200,"message":"ok"}""", modelLazy.resolveRawJson())

        // 磁碟檔案不存在時優雅回退
        val missingFile = File("non_existent_file.json")
        val modelMissing = ApiLogModel(
            id = "test_3",
            file = missingFile,
            title = "Test",
            endpoint = "/test",
            rawJson = ""
        )
        assertEquals("{}", modelMissing.resolveRawJson())
    }
}
