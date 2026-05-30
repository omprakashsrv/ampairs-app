package com.ampairs.tally.model.client


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class TDLMessage(
    @XmlElement(true)
    @XmlSerialName("COLLECTION")
    var collection: Collection = Collection(),
)