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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import java.nio.ByteBuffer

private val log = Logger.withTag("TallyApi")

class TallyApiImpl(engine: HttpClientEngine, private val baseUrl: String = "http://localhost:9008") : TallyApi {

    @OptIn(ExperimentalXmlUtilApi::class)
    private val client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            xml(
                contentType = ContentType.Text.Xml,
                format = XML {
                    repairNamespaces = true
                    xmlDeclMode = XmlDeclMode.None
                    indentString = ""
                    autoPolymorphic = true
                    policy = DefaultXmlSerializationPolicy(
                        pedantic = false,
                        autoPolymorphic = true,
                        unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
                    )
                }
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
        // Strip Tally's &#4; control characters once at init — not on every call
        client.responsePipeline.intercept(HttpResponsePipeline.Receive) { (type, content) ->
            if (content !is ByteReadChannel) return@intercept
            val replacing = ReplacingInputStream(content.toInputStream(), "&#4;", "")
            val byteBuffer = ByteBuffer.wrap(replacing.readAllBytes())
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
