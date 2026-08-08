package com.ampairs.tally

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import com.ampairs.tally.model.ImportResponse
import com.ampairs.tally.model.ImportResult
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.ByteBuffer

private val log = Logger.withTag("TallyApi")

class TallyApiImpl(engine: HttpClientEngine, private val baseUrl: String = "http://localhost:9008") : TallyApi {

    private val client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            xml(
                contentType = ContentType.Text.Xml,
                format = tallyXmlFormat,
            )
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    log.d { message }
                }
            }
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            val timeout = 30_000L
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    init {
        // Sanitize Tally's response once at init — not on every call:
        //  1. Strip &#4; control characters Tally injects into text.
        //  2. Strip the undeclared "UDF:" namespace prefix from User Defined Field elements.
        //     Tally emits <UDF:Foo>…</UDF:Foo> without ever declaring the UDF namespace, which
        //     makes the strict XML reader throw "undefined prefix: UDF". Dropping the prefix turns
        //     them into plain <Foo> elements that IGNORING_UNKNOWN_CHILD_HANDLER then skips.
        client.responsePipeline.intercept(HttpResponsePipeline.Receive) { (type, content) ->
            if (content !is ByteReadChannel) return@intercept
            val noControlChars = ReplacingInputStream(content.toInputStream(), "&#4;", "")
            val noUdfPrefix = ReplacingInputStream(noControlChars, "UDF:", "")
            val byteBuffer = ByteBuffer.wrap(noUdfPrefix.readAllBytes())
            proceedWith(HttpResponseContainer(type, ByteReadChannel(byteBuffer)))
        }
    }

    override suspend fun post(tallyXML: com.ampairs.tally.model.TallyXML): com.ampairs.tally.model.TallyXML {
        return client.post {
            url(baseUrl)
            contentType(ContentType.Text.Xml)
            setBody(tallyXML)
        }.body()
    }

    override suspend fun postImport(tallyXML: com.ampairs.tally.model.TallyXML): ImportResult? {
        // Read the reply as text (already sanitized by the response-pipeline interceptor above) and
        // decode it root-agnostically: Tally's import reply is a bare <RESPONSE>, but tolerate the
        // <ENVELOPE><BODY><DATA><IMPORTRESULT> shape too.
        val text = client.post {
            url(baseUrl)
            contentType(ContentType.Text.Xml)
            setBody(tallyXML)
        }.bodyAsText().trim()
        return parseImportReply(text)
    }

    private fun parseImportReply(text: String): ImportResult? {
        if (text.isBlank()) return null
        val looksLikeResponse = text.contains("<RESPONSE", ignoreCase = true) &&
            !text.contains("<ENVELOPE", ignoreCase = true)
        return runCatching {
            if (looksLikeResponse) {
                tallyXmlFormat.decodeFromString(ImportResponse.serializer(), text).toImportResult()
            } else {
                tallyXmlFormat.decodeFromString(
                    com.ampairs.tally.model.TallyXML.serializer(), text,
                ).body?.data?.importResult
            }
        }.getOrElse {
            // Neither shape parsed — surface Tally's raw reply (truncated) as a line error so the
            // push log shows what Tally actually said instead of a deserialization stack trace.
            ImportResult(errors = 1, lineError = text.take(300))
        }
    }
}
