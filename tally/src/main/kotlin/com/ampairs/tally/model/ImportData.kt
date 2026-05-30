package com.ampairs.tally.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class ImportData(
    @XmlElement(true)
    @XmlSerialName("REQUESTDESC")
    var requestDesc: RequestDesc? = null,

    @XmlElement(true)
    @XmlSerialName("REQUESTDATA")
    var requestData: RequestData? = null,
)