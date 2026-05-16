package com.ampairs.network.security

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/**
 * Desktop-specific HTTP client engine with certificate pinning
 */
class DesktopSecureEngineFactory(
    private val certificatePinningService: DesktopCertificatePinningService
) {
    
    fun createEngine(): HttpClientEngine {
        val okHttpClient = certificatePinningService.getOkHttpClient()
        
        return if (okHttpClient != null) {
            // Use the certificate pinned OkHttpClient
            OkHttp.create {
                preconfigured = okHttpClient.newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            }
        } else {
            // Fallback to default engine (should not happen in production)
            OkHttp.create {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
        }
    }
}