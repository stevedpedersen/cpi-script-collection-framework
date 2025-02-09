package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants
import src.main.resources.script.Framework_ValueMaps
import groovy.json.JsonSlurper
import java.net.URLEncoder

class Framework_Notifications {
    Message message
    MessageLog messageLog
    Framework_Logger logger

    Framework_Notifications(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
    }

    // Define the processData method
    public void formatHtmlBody() {
        // Get the incoming message payload as a string
        def payload = message.getBody(java.lang.String) as String
        def jsonObject = new JsonSlurper().parseText(payload)
        def headers = message.getHeaders()
        def properties = message.getProperties()
        
        def projectName = properties.getOrDefault("projectName", jsonObject?.projectName) ?: ""
        def integrationID = properties.getOrDefault("integrationID", jsonObject?.integrationID) ?: ""

        def meta_environment = Framework_ValueMaps.frameworkVM("meta_environment", System.getenv("TENANT_NAME"), message, messageLog)
        def meta_integrationName = interfaceVM("meta_integrationName", projectName, integrationID, integrationID)
        def emailRecipients = interfaceVM("emailRecipients", projectName, integrationID, 
            interfaceVM("emailRecepients", projectName, integrationID, ""))                          
        message.setProperty("emailRecipients", emailRecipients)

        String emailSubject = "BTP CI - ${meta_environment} - ${meta_integrationName}"
        message.setProperty("emailSubject", emailSubject)

        // Generate an HTML table row containing a link to the MPL/cALM dashboard for this message
        def correlationID = jsonObject?.correlationID ?: headers.get(Constants.Header.SAP_CORRELATION_ID)
        def messageID = jsonObject?.messageID ?: properties.get(Constants.Property.SAP_MPL_ID)
        
        // Function to convert JSON to HTML table
        def labelStyle = "style='width:25%;text-align:left;'"
        def monitorLinks = getLinksForObservabilityTools(message, correlationID ?: messageID ?: "UnknownID", labelStyle)
        def jsonToHtmlTable = { jsonObj ->
            def tableHtml = new StringBuilder()
            tableHtml.append("<table border='1'>${monitorLinks}")
            jsonObj.each { key, value ->
                key = formatPropertyName(key)
                def content = key.equalsIgnoreCase("messages") ? createMessagesHtml(value) : value
                tableHtml.append("<tr><th ${labelStyle}><b>${key}</b></th><td>${content}</td></tr>")
            }
            tableHtml.append("</table>")
            tableHtml.toString()
        }
        
        // Filter specified emailFilterValues logs only
        def emailFilterValues = interfaceVM("emailFilterValues", projectName, integrationID, "ERROR")
        def filterValues = emailFilterValues.toUpperCase().split(",").collect {
            Framework_Logger.normalizeLogLevels(it.trim())
        }
        def filteredItems = jsonObject.messages.findAll { 
            def itemLevel = Framework_Logger.normalizeLogLevels(it.logLevel)
            filterValues.contains(itemLevel)
        }
        jsonObject.messages = filteredItems

        // Build the HTML table
        def htmlTable = jsonToHtmlTable(jsonObject)
        message.setBody(htmlTable)

        return message
    }

    def interfaceVM(String key, String projectName, String integrationID, def defaultValue = "") {
        Framework_ValueMaps.interfaceVM(key, projectName, integrationID, defaultValue, message, messageLog)
    }

    /**
     * Takes a List of Map and produces one-dimensional HTML description lists. 
     */
    def createMessagesHtml(List<Map> messages) {
        def error = "style='color:${Constants.Style.JNJ_RED_HEX}!important'"
        def sb = new StringBuilder()
        messages.each { msgMap ->
            sb.append("<dl style='font-size:0.8rem'>")
            msgMap.each { k, v ->
                def style = k.equalsIgnoreCase("loglevel") ? error : ""
                sb.append("<dt><b ${style}>${k}</b></dt>")
                sb.append("<dd ${style}>${v}</dd>")
            }
            sb.append("</dl>")
        }
        return sb.toString()
    }

    /**
     * Constructs a URL to the MPL & cALM dashboards, if either is available
     */
    def getLinksForObservabilityTools(Message message, String searchID, String labelStyle) {
        def tenantName = System.getenv("TENANT_NAME")
        def domain = System.getenv("IT_TENANT_UX_DOMAIN")
        def cALM = "";
        def mpl = "";
        
        if (searchID && tenantName) {
            def calmBase = "https://jnj-cloudalm.us10.alm.cloud.sap/shell/run?sap-ui-app-id=com.sap.crun.imapp.ui#/monitoring/detail/${tenantName}/SAP_CPI"
            def calmAnchor = "<a href=\"${calmBase}\" target=\"_blank\">${tenantName}</a>"
            cALM = "<tr><th ${labelStyle}><b>cALM Dashboard</b></th><td>${calmAnchor}<br>Search for Message ID: ${searchID}</td></tr>"
            
            if (domain) {
                // Encode only the JSON part
                def idJson = URLEncoder.encode("{\"identifier\":\"${searchID}\"}", "UTF-8")
                // Append the encoded JSON directly to the base URL
                def fullURL = "https://${tenantName}.integrationsuite.${domain}/shell/monitoring/Messages/" + idJson
                def mplAnchorTag = "<a href=\"${fullURL}\" target=\"_blank\">${searchID}</a>"
                
                mpl = "<tr><th ${labelStyle}><b>Message Processing Log URL</b></th><td>${mplAnchorTag}</td></tr>"
            }
        }
        return mpl + cALM;
    }

    /**
     * Automatically format the property names with capitalization and spaces.
     */
    def formatPropertyName(String propertyName) {
        propertyName
            .replaceAll("_", " ")
            .split(' ').collect { it.capitalize() }.join(' ')
            .replaceAll(/([a-z])([A-Z])/, '$1 $2').capitalize()
    }

    private String frameworkVM(String key, String defaultValue = null) {
        try {
            return getValueMapping(key, Constants.ILCD.VM_GLOBAL_SRC_ID, Constants.ILCD.VM_GLOBAL_TRGT_ID) ?: defaultValue
        } catch (Exception e) {
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_Notifications.frameworkVM", false)
            return defaultValue
        }
    }
    private String interfaceVM(String key, String projectName, String integrationID, String defaultValue = null) {
        try {
            return getValueMapping(key, projectName, integrationID) ?: defaultValue
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Notifications.safeGetValueMapping", false)
            return defaultValue
        }
    }

    def getValueMapping(String srcKey, String srcId, String targetId) {
        try {
            def api = ITApiFactory.getApi(ValueMappingApi.class, null)
            return api?.getMappedValue(Constants.ILCD.VM_SRC_AGENCY, srcId, srcKey, Constants.ILCD.VM_TRGT_AGENCY, targetId) ?: ""
        } catch (Exception e) {
            def fields = """\n\n
                Source Agency:      ${srcAgency}\n
                Source Identifier:  ${srcId}\n
                Source Value:       ${srcKey}\n
                Target Agency:      ${targetAgency}\n
                Target Identifier:  ${targetId}\n
            """
            handleScriptError(message, messageLog, e, "Framework_Notifications.getValueMapping", true, fields)
            throw e
        }
    }
}


