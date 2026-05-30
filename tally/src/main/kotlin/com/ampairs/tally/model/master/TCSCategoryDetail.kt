package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class TCSCategoryDetail(
    @XmlElement(true)
    @XmlSerialName("CATEGORYDATE")
    var categoryDate: String? = null,

    @XmlElement(true)
    @XmlSerialName("CATEGORYNAME")
    var categoryName: String? = null,
)
