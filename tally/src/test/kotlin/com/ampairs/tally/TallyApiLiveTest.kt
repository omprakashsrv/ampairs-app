package com.ampairs.tally

import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Live integration tests against a running Tally ERP server.
 *
 * Run with: ./gradlew tally:test -Dtally.host=192.168.1.17 -Dtally.port=9000
 *
 * Tests skip automatically if tally.host is not provided (CI-safe).
 */
@Tag("live")
class TallyApiLiveTest {

    private val host = System.getProperty("tally.host") ?: System.getenv("TALLY_HOST")
    private val port = System.getProperty("tally.port")?.toIntOrNull()
        ?: System.getenv("TALLY_PORT")?.toIntOrNull()
        ?: 9008

    private fun createRepo(): TallyRepository {
        assumeTrue(
            host != null,
            "Skipping live test — set -Dtally.host=<ip> to run against a real Tally server"
        )
        val baseUrl = "http://$host:$port"
        println("Connecting to Tally at $baseUrl")
        return TallyRepository(TallyApiImpl(CIO.create(), baseUrl))
    }

    @Test
    fun getStockGroupsReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getStockGroups()
            val groups = result.body?.data?.collection?.stockGroups
            println("StockGroups count: ${groups?.size ?: 0}")
            groups?.take(5)?.forEach { println("  Group: ${it.name} (alterId=${it.alterId})") }
        }
    }

    @Test
    fun getStockCategoriesReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getStockCategories()
            val categories = result.body?.data?.collection?.stockCategories
            println("StockCategories count: ${categories?.size ?: 0}")
            categories?.take(5)?.forEach { println("  Category: ${it.name} (alterId=${it.alterId})") }
        }
    }

    @Test
    fun getStockItemsReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getStockItems()
            val items = result.body?.data?.collection?.stockItems
            println("StockItems count: ${items?.size ?: 0}")
            items?.take(5)?.forEach {
                println("  Item: ${it.name} | group=${it.parent} | unit=${it.baseUnits} | alterId=${it.alterId}")
            }
        }
    }

    @Test
    fun getUnitsReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getUnits()
            val units = result.body?.data?.collection?.units
            println("Units count: ${units?.size ?: 0}")
            units?.take(5)?.forEach { println("  Unit: ${it.name} (alterId=${it.alterId})") }
        }
    }
}
