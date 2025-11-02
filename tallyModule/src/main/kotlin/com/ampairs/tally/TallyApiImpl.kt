package com.ampairs.tally

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
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

const val TALLY_END_POINT = "http://192.168.1.76:9000"

class TallyApiImpl(engine: HttpClientEngine) : TallyApi {

    @OptIn(ExperimentalXmlUtilApi::class)
    private val client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            xml(contentType = ContentType.Text.Xml,
                format = XML {
                    repairNamespaces = true
                    xmlDeclMode = XmlDeclMode.None
                    indentString = ""
                    autoPolymorphic = true
                    this.xmlDeclMode
                    policy = DefaultXmlSerializationPolicy(
                        pedantic = false,
                        autoPolymorphic = true,
                        unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
                    )
                }
            )
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("message = ${message}")
                }
            }

            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            val timeout = 30000L
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }


    override suspend fun post(tallyXML: com.ampairs.tally.model.TallyXML): com.ampairs.tally.model.TallyXML {
        client.responsePipeline.intercept(HttpResponsePipeline.Receive) { (type, content) ->
            if (content !is ByteReadChannel) return@intercept
            val inputStream = content.toInputStream()
//            val response = String(inputStream.readAllBytes())
//            println("response = ${response}")
            val replacingInputStream = ReplacingInputStream(inputStream, "&#4;", "")
            val byteBuffer: ByteBuffer = ByteBuffer.wrap(replacingInputStream.readAllBytes())
            val byteReadChannel = ByteReadChannel(byteBuffer)
            proceedWith(HttpResponseContainer(type, byteReadChannel))
        }
        return client.post {
            url(TALLY_END_POINT)
            contentType(ContentType.Text.Xml)
            setBody(
                tallyXML
            )
        }.body()
    }
}