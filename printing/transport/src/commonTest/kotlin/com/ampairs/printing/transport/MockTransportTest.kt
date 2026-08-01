package com.ampairs.printing.transport

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.transport.mock.MockTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockTransportTest {

    private val profile = PrinterProfile(
        id = "mock", name = "Mock", printerClass = PrinterClass.THERMAL,
        connectionType = ConnectionType.NETWORK, paper = PaperSpec.THERMAL_80,
    )

    @Test
    fun capturesSentBytesAfterOpen() = runTest {
        val t = MockTransport()
        t.open(profile)
        t.send(RenderedOutput.Bytes("HELLO".encodeToByteArray()))
        assertEquals("HELLO", t.lastText())
        t.close()
    }

    @Test
    fun sendBeforeOpenFails() = runTest {
        val t = MockTransport()
        val r = t.send(RenderedOutput.Bytes(byteArrayOf(1, 2, 3)))
        assertTrue(r.isFailure)
    }
}
