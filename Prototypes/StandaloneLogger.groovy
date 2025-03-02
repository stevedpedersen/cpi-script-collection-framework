package collections.ilcd

import com.sap.gateway.ip.core.customdev.util.Message
import java.util.HashMap
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Map
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.transform.Field
import java.io.File
import java.nio.file.OpenOption
import java.nio.file.Path
import javax.xml.transform.TransformerFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import org.w3c.dom.Node
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import static java.util.UUID.randomUUID


@Field String IFLOW_NAME = 'com.sap.scenarios'
@Field String MPL_LOGGING_MODE = 'PROPERTY'   // ALWAYS, NEVER, PROPERTY
@Field String LOG_HEADERS = 'YES'   // YES / NO
@Field String LOG_PROPERTIES = 'YES'   // YES / NO
@Field String LOG_BODY_INFO = 'YES'   // YES / NO
@Field String LOG_EXCEPTION = 'YES'   // YES / NO
@Field String LOG_OTHER = 'YES'   // YES / NO
@Field String LOG_BODY = 'PROPERTY'   // YES / NO / PROPERTY
@Field String LOG_ATTACHMENTS_INFO = 'YES'   // YES / NO
@Field String LOG_ATTACHMENTS_BODY = 'YES'   // YES / NO
@Field String LOG_SOAP_HEADERS_INFO = 'YES'   // YES / NO
@Field String MODIFY_EXCHANGE = 'YES'   // YES / NO

@Field String PROPERTY_LOG_ID = 'SAP_LOG_ID'
@Field String PROPERTY_ENABLE_MPL_LOGGING = 'ENABLE_MPL_LOGGING'
@Field String PROPERTY_ENABLE_PAYLOAD_LOGGING = 'ENABLE_PAYLOAD_LOGGING'
@Field String STR_FORMAT_WIDTH = 40


Message logInitialInputIDOC(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputIDOC_info',message)
        processBody('00.logInitialInputIDOC', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logInitialInputUBL(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputUBL_info',message)
        processBody('00.logInitialInputUBL', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logInitialInputCXML(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputCXML_info',message)
        processBody('00.logInitialInputCXML', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logInitialInputDocument(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputDocument_info',message)
        processBody('00.logInitialInputDocument', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logInitialInputX12(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputX12_info',message)
        processBody('00.logInitialInputX12', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logInitialInputEdifact(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputEdifact_info',message)
        processBody('00.logInitialInputEdifact', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message logInitialInputEdi(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputEdi_info',message)
        processBody('00.logInitialInputEdi', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message logInitialInputeExact(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputeExact_info',message)
        processBody('00.logInitialInputeExact', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message logInitialInputIUNGO(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputIUNGO_info',message)
        processBody('00.logInitialInputIUNGO', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message logInitialInputIUNGOSdDataSlice(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputIUNGOSdDataSlice_info',message)
        processBody('00.logInitialInputIUNGOSdDataSlice', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message logInitialInputxCBL(Message message) {
    try {
        // processHeadersAndProperties('logInitialInputxCBL_info',message)
        processBody('00.logInitialInputxCBL', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logInvalidPayload(Message message) {
    try {
        // processHeadersAndProperties('logInvalidPayload_info',message)
        processBody('logInvalidPayload', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logResponse(Message message) {
    try {
        // processHeadersAndProperties('logResponse_info',message)
        processBody('logResponse', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logErpResponse(Message message) {
    try {
        // processHeadersAndProperties('logErpResponse_info',message)
        processBody('logErpResponse', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}

Message logOutput(Message message) {
    try {
        // processHeadersAndProperties('logOutput_info',message)
        processBody('logOutput', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message log_error(Message message) {
    try {
        processHeadersAndProperties('log_error_info', message)
        processBody('log_error_payload', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}


Message processData(Message message) {
    // throw new IllegalStateException("yyyyy not provided.")

    try {
        processHeadersAndProperties('log', message)
        processBody('log_payload', message)
    } catch (Exception ex) {
        log.error("processData error", ex)
    }
    return message
}



void processHeadersAndProperties(String prefix_with_vars, Message message) {
    try {
        StringBuffer sb_html = new StringBuffer()
        StringBuffer sb_text = new StringBuffer()
        
        // Headers
        def map = message.getHeaders()
        if ('YES'.equalsIgnoreCase(LOG_HEADERS)) {
            dumpProperties_TEXT_escaped("Headers", map, sb_html)
            dumpProperties_TEXT("Headers", map, sb_text)
        }

        // Properties
        map = message.getProperties()
        if ('YES'.equalsIgnoreCase(LOG_PROPERTIES)) {
            dumpProperties_TEXT_escaped("Properties", map, sb_html)
            dumpProperties_TEXT("Properties", map, sb_text)
        }

        // Exception Details
        if ('YES'.equalsIgnoreCase(LOG_EXCEPTION)) {
            def ex = map.get("CamelExceptionCaught")
            if (ex != null) {
	            def canonicalName = ex.getClass().getCanonicalName() ?: ""
	        	def exMap = new HashMap()
				StringWriter swe = new StringWriter()
				ex.printStackTrace(new PrintWriter(swe))
				exMap.put("stacktrace", swe.toString())
				exMap.put("exception", ex)
				exMap.put("getCanonicalName", canonicalName)
				exMap.put("getMessage", ex.getMessage())

                if (canonicalName.equals("org.apache.camel.component.ahc.AhcOperationFailedException")) {
                    exmap.put("responseBody", safeEscapeXml(ex.getResponseBody()))
                    exmap.put("responseBody.className", safeClassName(ex))
                    exmap.put("getStatusText", ex.getStatusText())
                    exmap.put("getStatusCode", ex.getStatusCode())
                }
                if (ex instanceof org.apache.cxf.interceptor.Fault) {
                    exmap.put("getDetail", safeEscapeXml(ex.getDetail()))
                    exmap.put("getDetail.className", safeClassName(ex.getDetail()))
                    exmap.put("getFaultCode", ex.getFaultCode())
                    exmap.put("getMessage", ex.getMessage())
                    exmap.put("getStatusCode", "" + ex.getStatusCode())
                    exmap.put("hasDetails", "" + ex.hasDetails())
                    exmap.put("getCause", "" + ex.getCause())
                    
                    if (ex.getCause() != null) {
                        def cause_message = ex.getCause().getMessage()
                        if (ex.getCause() instanceof org.apache.cxf.transport.http.HTTPException) {
                            cause_message = ex.getCause().getResponseMessage()
                        }
                        exmap.put("getCause.getResponseMessage", "" + cause_message)
                        message.getHeaders().put("SoapFaultMessage", ex.getMessage() + ": " + ex.getCause().getResponseMessage())
                    }
                }
                dumpProperties_TEXT_escaped("property.CamelExceptionCaught", exmap, sb_html)
                dumpProperties_HTML("property.CamelExceptionCaught", exmap, sb_text)
            }
        }

        // SOAP Headers
        enable = false
        if ('YES'.equalsIgnoreCase(LOG_SOAP_HEADERS_INFO)) {
            enable = true
        }
        if (enable) {
            def infomap = new HashMap()
            def headers = message.getHeaders()
            def list = headers.get("org.apache.cxf.headers.Header.list")
            if (list != null) {
                infomap.put("\${header.org.apache.cxf.headers.Header.list}", list)
                infomap.put("size", list.size())
                list.each { header ->
                    // elements of this head: com.sun.org.apache.xerces.internal.dom.ElementNSImpl
                    // infomap.put("header["+header.getName()+"].object.clazz", header.getObject().getClass());
                    infomap.put("header[" + header.getName() + "]", header)
                    org.w3c.dom.Node element = (Node) header.getObject()
                    def document = element.getOwnerDocument()
                    // conversion using Transformer class |
                    def str1 = printXML(element)
                    infomap.put("header[" + header.getName() + "].value", safeEscapeXml(str1))
                }
            }
            dumpProperties_TEXT_escaped("SOAP Headers", infomap, sb_html)
            dumpProperties_TEXT("SOAP Headers", infomap, sb_text)
        }
        enable = true
        if ('YES'.equalsIgnoreCase(LOG_ATTACHMENTS_INFO)) {
            enable = true
        }
        if (enable) {
            def infomap = new HashMap()
            def attachments = message.getAttachments()
            if ((attachments != null) && (!attachments.isEmpty())) {
                infomap.put("attachments", attachments)
                infomap.put("attachments.clazz.name", attachments.getClass().getName())
                infomap.put("attachments.keys", attachments.getOriginalMap().keySet())
                attachments.each { key -> infomap.put("attachments[" + key.getKey() + "]", attachments.get(key.getKey())) }
                dumpProperties_TEXT_escaped("Attachments", infomap, sb_html)
                dumpProperties_TEXT("Attachments", infomap, sb_text)
            }
        }
        enable = false
        if ('YES'.equalsIgnoreCase(LOG_BODY_INFO)) {
            enable = true
        }
        if (enable) {
            def body_test = message.getBody()
            def bodymap = new HashMap()

            if (body_test instanceof String) {
                if (body_test.size() > 100) {
                    bodymap.put("Body", body_test.substring(0, 100) + "...")
                } else {
                    bodymap.put("Body", body_test)
                }
            } else {
                bodymap.put("Body", body_test)
            }
            if (body_test != null) {
                bodymap.put("body.clazz.name", body_test.getClass().getCanonicalName())
            }
            dumpProperties_TEXT_escaped("Body", bodymap, sb_html)
            dumpProperties_TEXT("Body", bodymap, sb_text)
        }
        enable = true
        if ('YES'.equalsIgnoreCase(LOG_OTHER)) {
            enable = true
        }
        if (enable) {
            def othermap = new HashMap()
            def currentDate = new Date()
            def timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(currentDate)
            othermap.put("CurrentTimestamp", timestamp)
            if (isTrue(MODIFY_EXCHANGE)) {
                message.setHeader("LAST_TIMESTAMP", currentDate)
            }
            dumpProperties_TEXT_escaped("Other", othermap, sb_html)
            dumpProperties_TEXT("Other", othermap, sb_text)
        }
        def props = message.getProperties()
        // def property_ENABLE_MPL_LOGGING = props.get(PROPERTY_ENABLE_MPL_LOGGING)
        def property_ENABLE_MPL_LOGGING = "yes"

        def mpl_enabled = false
        if (isTrue(MPL_LOGGING_MODE)) {
            mpl_enabled = true
        } else if ("PROPERTY".equalsIgnoreCase(MPL_LOGGING_MODE)) {
            if (isTrue(property_ENABLE_MPL_LOGGING)) {
                mpl_enabled = true
            }
        }
        def tmp_string = org.apache.commons.lang.StringEscapeUtils.escapeXml(sb_html.toString())
        sb_html.setLength(0)
        // sb_html.append("<pre>")
        sb_html.append(tmp_string)
        // sb_html.append("</pre>")
        if (mpl_enabled) {
            def messageLog = messageLogFactory.getMessageLog(message)
            messageLog.addAttachmentAsString(prefix_with_vars, "${sb_text.toString()}", "text/plain")
        }
    } catch (Throwable ex01) {
        log.error("processHeadersAndProperties: " + prefix_with_vars + " ", ex01)
        StringWriter sw = new StringWriter()
        ex01.printStackTrace(new PrintWriter(sw))
        log.info(sw.toString())
    }
}




boolean isTrue(String str) {
    if ('ALWAYS'.equalsIgnoreCase(str)) {
        return true
    } else if ('TRUE'.equalsIgnoreCase(str)) {
        return true
    } else if ('YES'.equalsIgnoreCase(str)) {
        return true
    } else if ('ON'.equalsIgnoreCase(str)) {
        return true
    }
    return false
}


void processBody(String prefix_with_vars, Message message) {
    byte[] body_bytes = null
    try {
        def props = message.getProperties()
        // def property_ENABLE_MPL_LOGGING = props.get(PROPERTY_ENABLE_MPL_LOGGING)
        // def property_ENABLE_PAYLOAD_LOGGING = props.get(PROPERTY_ENABLE_PAYLOAD_LOGGING)

        def property_ENABLE_MPL_LOGGING = "yes"
        def property_ENABLE_PAYLOAD_LOGGING = "no"

        def enable = false
        if ('YES'.equalsIgnoreCase(LOG_BODY)) {
            enable = true
        } else if ('PROPERTY'.equalsIgnoreCase(LOG_BODY)) {
            if (isTrue(property_ENABLE_PAYLOAD_LOGGING)) {
                enable = true
            }
        }
        if (!enable) return

        if (message == null) {
            body_bytes = new byte[0]
        } else if (message.getBody() == null) {
            body_bytes = new byte[0]
        } else {
            body_bytes = message.getBody(byte[].class)
        }
        def mpl_enabled = false
        if (isTrue(MPL_LOGGING_MODE)) {
            mpl_enabled = true
        } else if ("PROPERTY".equalsIgnoreCase(MPL_LOGGING_MODE)) {
            if (isTrue(property_ENABLE_MPL_LOGGING)) {
                mpl_enabled = true
            }
        }
        if (mpl_enabled) {
            def messageLog = messageLogFactory.getMessageLog(message)
            messageLog.addAttachmentAsString(prefix_with_vars, new String(body_bytes, "UTF-8"), "text/plain")
        }
        // message.setBody(new String(body_bytes, "UTF-8"))
    } catch (Exception ex01) {
        log.error("cannot save body", ex01)
        StringWriter sw = new StringWriter()
        ex01.printStackTrace(new PrintWriter(sw))
        log.info(sw.toString())
    }
}


boolean skipNL

String printXML(org.w3c.dom.Node rootNode) {
    String tab = ""
    skipNL = false
    return (printXML(rootNode, tab))
}

String printXML(org.w3c.dom.Node rootNode, String tab) {
    String print = ""
    if (rootNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        print += "\n" + tab + "<" + rootNode.getNodeName() + ">"
    }
    org.w3c.dom.NodeList nl = rootNode.getChildNodes()
    if (nl.getLength() > 0) {
        for (int i = 0; i < nl.getLength(); i++) {
            print += printXML(nl.item(i), tab + "  ")    //  \t
        }
    } else {
        if (rootNode.getNodeValue() != null) {
            print = rootNode.getNodeValue()
        }
        skipNL = true
    }
    if (rootNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        if (!skipNL) {
            print += "\n" + tab
        }
        skipNL = false
        print += "</" + rootNode.getNodeName() + ">"
    }
    return (print)
}

String safeClassName(Object obj) {
    if (obj == null) return ""
    return obj.getClass().getName()
}


Object safeEscapeXml(Object payload) {
    if (payload instanceof java.lang.String) {
        // return payload
        return org.apache.commons.lang.StringEscapeUtils.escapeXml(ex.getResponseBody());
    } else if (payload instanceof byte[]) {
        // return payload
        return org.apache.commons.lang.StringEscapeUtils.escapeXml(ex.getResponseBody());
    } else if (payload instanceof Node) {
        // return printXML(payload)
        return org.apache.commons.lang.StringEscapeUtils.escapeXml(printXML(payload));
    }
    return payload
}


void dumpProperties(String title, Map<String, Object> map, StringBuffer sb) {
    sb.append(title + "\n")
    map.each { key, value ->
        sb.append("$key\t$value\n")
    }
}


void dumpProperties_HTML(String title, Map<String, Object> map, StringBuffer sb) {
    sb.append("<h1>$title</h1><br>\n")
    sb.append("<table>\n")
    map.each { key, value ->
        sb.append("<tr>\n")
        sb.append("<td>$key</td><td>$value</td>\n")
        sb.append("</tr>\n")
    }
    sb.append("</table>\n")
}


void dumpProperties_TEXT_escaped(String title, Map<String, Object> map, StringBuffer sb) {
    sb.append(safeEscapeXml(title) + "\n")
    for (String key : map.keySet()) {
        sb.append(String.format(" %-40s: %-40s\n", safeEscapeXml(key), safeEscapeXml(map.get(key))))
    }
    sb.append("\n")
}


void dumpProperties_TEXT(String title, Map<String, Object> map, StringBuffer sb) {
    sb.append(title + "\n")
    map.each { key, value ->
        sb.append(String.format(" %-40s: %-40s\n", key, value))
    }
    sb.append("\n")
}
