package com.ampairs.imagesearch

import com.ampairs.imagesearch.scrape.ImageResultParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageResultParserTest {

    @Test
    fun parsesFullResAndThumbnail() {
        val json = """
            [{"t":"https://cdn/thumb.jpg","f":"https://cdn/full.jpg","h":"cdn","w":800,"e":600}]
        """.trimIndent()
        val results = ImageResultParser.parse(json)
        assertEquals(1, results.size)
        val r = results.first()
        assertEquals("https://cdn/thumb.jpg", r.thumbnailUrl)
        assertEquals("https://cdn/full.jpg", r.fullResUrl)
        assertEquals("cdn", r.sourceHost)
        assertEquals(800, r.width)
        assertEquals(600, r.height)
        assertNull(r.thumbnailBytes)
    }

    @Test
    fun skipsEntriesWithNoUsableImage() {
        val json = """[{"t":"","f":"","h":"","w":0,"e":0}]"""
        assertTrue(ImageResultParser.parse(json).isEmpty())
    }

    @Test
    fun malformedPayloadYieldsEmptyList() {
        assertTrue(ImageResultParser.parse("not json").isEmpty())
        assertTrue(ImageResultParser.parse("").isEmpty())
    }

    @Test
    fun decodesBase64DataUriThumbnail() {
        // "aGVsbG8=" == "hello"
        val json = """[{"t":"data:image/png;base64,aGVsbG8=","f":"","h":"","w":0,"e":0}]"""
        val r = ImageResultParser.parse(json).single()
        assertTrue("hello".encodeToByteArray().contentEquals(r.thumbnailBytes))
    }

    @Test
    fun decodeDataUriReturnsNullForPlainUrl() {
        assertNull(ImageResultParser.decodeDataUri("https://cdn/x.jpg"))
    }

    @Test
    fun dataUriContentTypeParsed() {
        assertEquals("image/png", ImageResultParser.dataUriContentType("data:image/png;base64,aGk="))
        assertNull(ImageResultParser.dataUriContentType("https://cdn/x.jpg"))
    }
}
