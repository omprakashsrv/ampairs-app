package com.ampairs.tally.model

import com.ampairs.tally.model.report.DSPACCNAME
import com.ampairs.tally.model.report.DSPSTKINFO
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@XmlSerialName("ENVELOPE")
@Serializable
data class TallyXML(
    @XmlElement(false)
    @XmlSerialName("Action")
    val action: String? = "",

    @XmlElement(true)
    @XmlSerialName("HEADER")
    var header: Header? = Header(),

    @XmlElement()
    @XmlSerialName("BODY")
    var body: Body? = Body(),

    @XmlElement(true)
    @XmlSerialName("DSPACCNAME")
    var dspaccname: List<DSPACCNAME>? = null,


    @XmlElement(true)
    @XmlSerialName("DSPSTKINFO")
    var stockInfo: List<DSPSTKINFO>? = null,

    )