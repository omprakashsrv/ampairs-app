package com.ampairs.tally

import com.ampairs.tally.model.ImportResult
import com.ampairs.tally.model.TallyXML

interface TallyApi {
    suspend fun post(tallyXML: TallyXML): TallyXML

    /**
     * Posts an IMPORTDATA envelope and parses the reply into an [ImportResult]. Tally returns a bare
     * `<RESPONSE>` root here (not `<ENVELOPE>`), so this reads the body as text and decodes it
     * root-agnostically instead of going through the `<ENVELOPE>`-typed [post]. Returns null only if
     * the body was empty.
     */
    suspend fun postImport(tallyXML: TallyXML): ImportResult?
}
