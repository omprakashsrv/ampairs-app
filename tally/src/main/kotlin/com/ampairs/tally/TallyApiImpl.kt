package com.ampairs.tally

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
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
}
