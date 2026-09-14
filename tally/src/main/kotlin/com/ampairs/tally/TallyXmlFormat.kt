package com.ampairs.tally

import com.ampairs.tally.model.TallyXML
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig

/**
 * The single XML format used both for the Ktor content-negotiation (see [TallyApiImpl]) and for
 * rendering an envelope to a string ([renderTallyXml]). Sharing one instance means the debug
 * "request XML" shown in the sync log is byte-for-byte what gets posted to Tally.
 */
@OptIn(ExperimentalXmlUtilApi::class)
val tallyXmlFormat: XML = XML {
    repairNamespaces = true
    xmlDeclMode = XmlDeclMode.None
    indentString = ""
    autoPolymorphic = true
    policy = DefaultXmlSerializationPolicy(
        pedantic = false,
        autoPolymorphic = true,
        unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER,
    )
}

/** Serializes a [TallyXML] envelope to its wire string (for logging / debugging the push). */
fun renderTallyXml(envelope: TallyXML): String =
    tallyXmlFormat.encodeToString(TallyXML.serializer(), envelope)
