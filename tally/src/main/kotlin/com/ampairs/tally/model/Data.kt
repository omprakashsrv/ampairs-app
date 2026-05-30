package com.ampairs.tally.model

import com.ampairs.tally.model.client.Collection
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Data(
    @XmlElement(true)
    @XmlSerialName("COLLECTION")
    var collection: Collection? = null,
)