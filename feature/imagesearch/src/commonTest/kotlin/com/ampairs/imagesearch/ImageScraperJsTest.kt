package com.ampairs.imagesearch

import com.ampairs.imagesearch.domain.SearchKeyword
import com.ampairs.imagesearch.domain.toQuery
import com.ampairs.imagesearch.scrape.ImageScraperJs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageScraperJsTest {

    @Test
    fun searchUrlEncodesQueryAndTargetsImages() {
        val url = ImageScraperJs.searchUrl("Nike running shoes")
        assertTrue(url.startsWith("https://www.google.com/search?"), url)
        assertTrue(url.contains("tbm=isch"), url)
        // spaces encoded as '+'
        assertTrue(url.contains("q=Nike+running+shoes"), url)
    }

    @Test
    fun bootstrapPrependsShim() {
        val shim = "window.__ampairsPost = function(j){};"
        val boot = ImageScraperJs.bootstrap(shim)
        assertTrue(boot.startsWith(shim))
        assertTrue(boot.contains("__ampairsPost"))
        assertTrue(boot.contains("imgres"))
    }

    @Test
    fun toQueryJoinsOnlyEnabledKeywords() {
        val keywords = listOf(
            SearchKeyword("Nike", "Nike", enabled = true),
            SearchKeyword("Shoes", "Shoes", enabled = true),
            SearchKeyword("Red", "Red", enabled = false),
        )
        assertEquals("Nike Shoes", keywords.toQuery())
    }
}
