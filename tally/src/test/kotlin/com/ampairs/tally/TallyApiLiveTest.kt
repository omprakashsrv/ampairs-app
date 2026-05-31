package com.ampairs.tally

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

    private fun rawXmlBody(typeId: String): String = """
        <ENVELOPE>
          <HEADER>
            <VERSION>1</VERSION>
            <TALLYREQUEST>EXPORT</TALLYREQUEST>
            <TYPE>COLLECTION</TYPE>
            <ID>$typeId</ID>
          </HEADER>
          <BODY>
            <DESC>
              <TDL>
                <TDLMESSAGE>
                  <COLLECTION NAME="$typeId" ISMODIFY="No" ISFIXED="No" ISINITIALIZE="No" ISOPTION="No" ISINTERNAL="No">
                    <TYPE>${typeId.removeSuffix("COL")}</TYPE>
                    <NATIVEMETHOD>*</NATIVEMETHOD>
                  </COLLECTION>
                </TDLMESSAGE>
              </TDL>
            </DESC>
          </BODY>
        </ENVELOPE>
    """.trimIndent()

    @Test
    fun printRawGroupsXml() {
        runBlocking {
            assumeTrue(host != null, "Skipping live test — set -Dtally.host=<ip> to run")
            val client = HttpClient(CIO) {
                install(HttpTimeout) { requestTimeoutMillis = 120_000; socketTimeoutMillis = 120_000; connectTimeoutMillis = 10_000 }
            }
            val response = client.post {
                url("http://$host:$port")
                contentType(ContentType.Text.Xml)
                setBody(rawXmlBody("CUSTOMGROUPCOL"))
            }
            val text = response.bodyAsText()
            println("=== RAW GROUPS XML (first 4000 chars) ===")
            println(text.take(4000))
            println("=== END ===")
            client.close()
        }
    }

    @Test
    fun printRawLedgersXml() {
        runBlocking {
            assumeTrue(host != null, "Skipping live test — set -Dtally.host=<ip> to run")
            val client = HttpClient(CIO) {
                install(HttpTimeout) { requestTimeoutMillis = 120_000; socketTimeoutMillis = 120_000; connectTimeoutMillis = 10_000 }
            }
            val response = client.post {
                url("http://$host:$port")
                contentType(ContentType.Text.Xml)
                setBody(rawXmlBody("CUSTOMLEDGERCOL"))
            }
            val text = response.bodyAsText()
            println("=== RAW LEDGERS XML (first 5000 chars) ===")
            println(text.take(5000))
            println("=== END ===")
            client.close()
        }
    }

    @Test
    fun getGroupsReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getGroups()
            val groups = result.body?.data?.collection?.groups
            println("Groups count: ${groups?.size ?: 0}")
            groups?.take(10)?.forEach {
                println(
                    "  Group: name=${it.name} | parent=${it.parent} | " +
                        "guid=${it.guid} | alterId=${it.alterId} | " +
                        "isBillwiseOn=${it.isBillwiseOn} | isSubLedger=${it.isSubLedger}"
                )
            }
        }
    }

    @Test
    fun getLedgersReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getLedgers()
            val ledgers = result.body?.data?.collection?.ledgers
            println("Ledgers count: ${ledgers?.size ?: 0}")
            ledgers?.take(10)?.forEach {
                val mailing = it.mailingDetailList?.firstOrNull()
                val gstReg = it.gstRegDetailList?.firstOrNull()
                println(
                    "  Ledger: name=${it.name} | parent=${it.parent} | " +
                        "guid=${it.guid} | alterId=${it.alterId}\n" +
                        "    phone=${it.ledgerMobile} | landline=${it.ledgerPhone}\n" +
                        "    gstin(reg)=${gstReg?.gstin} | gstin(top)=${it.partyGstin} | gstType=${gstReg?.gstRegistrationType}\n" +
                        "    state(mailing)=${mailing?.stateName} | state(top)=${it.stateName}\n" +
                        "    pincode(mailing)=${mailing?.pinCode} | country(mailing)=${mailing?.countryName}\n" +
                        "    address(mailing)=${mailing?.addressList?.joinToString(" / ")} | address(top)=${it.addressList?.joinToString(" / ")}"
                )
            }
        }
    }

    @Test
    fun getStockBalancesReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getStockBalances()
            val items = result.body?.data?.collection?.stockItems
            println("StockBalances count: ${items?.size ?: 0}")
            items?.filter { !it.closingBalance.isNullOrBlank() }
                ?.take(10)
                ?.forEach {
                    println("  Item: ${it.name} | closing=${it.closingBalance} | value=${it.closingValue} | unit=${it.baseUnits}")
                }
        }
    }

    @Test
    fun getStockItemsWithPriceFieldsReturnsAResponse() {
        runBlocking {
            val repo = createRepo()
            val result = repo.getStockItems()
            val items = result.body?.data?.collection?.stockItems
            println("StockItems count: ${items?.size ?: 0}")
            // Show price/cost fields for first 10 items with non-zero prices
            items?.filter { it.standardPrice?.rate != null || it.standardCost?.rate != null }
                ?.take(10)
                ?.forEach {
                    println(
                        "  Item: ${it.name}\n" +
                            "    standardPrice.rate=${it.standardPrice?.rate}  (→ mrp / dp / selling_price)\n" +
                            "    standardCost.rate=${it.standardCost?.rate}    (→ buying_price)\n" +
                            "    baseUnits=${it.baseUnits} | additionalUnits=${it.additionalUnits}\n" +
                            "    gstApplicable=${it.gstApplicable} | taxability=${it.taxability}\n" +
                            "    gstTypeOfSupply=${it.gstTypeOfSupply} | gstRepUOM=${it.gstRepUOM}\n" +
                            "    gstDetails=${it.gstDetailList?.firstOrNull()?.let { d -> "rate=${d.taxability}" }}"
                    )
                }
        }
    }
}
