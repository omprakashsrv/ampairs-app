package com.ampairs.tally.model

import com.ampairs.tally.model.client.Desc
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Body(
    @XmlElement(true) @XmlSerialName("IMPORTDATA") var importData: ImportData? = null,
    @XmlElement(true) @XmlSerialName("EXPORTDATA") var exportData: ExportData? = null,
    @XmlElement(true) @XmlSerialName("DATA") var data: Data? = null,
    @XmlElement(true) @XmlSerialName("DESC") var desc: Desc? = Desc(),
)
