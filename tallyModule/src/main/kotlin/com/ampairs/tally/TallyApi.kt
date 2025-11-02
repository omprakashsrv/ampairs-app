package com.ampairs.tally

import com.ampairs.tally.model.TallyXML

interface TallyApi {
    suspend fun post(tallyXML: TallyXML): TallyXML
}