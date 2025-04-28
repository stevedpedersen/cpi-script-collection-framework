package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog

import src.main.resources.script.Framework_ValueMaps
import src.main.resources.script.Constants

class Framework_Notifications {
    Message message
    MessageLog messageLog

    Framework_Notifications(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
    }

    /**
     * Formats and sets the HTML body for the notification message.
     */
    public void formatHtmlBody() {
        def payload = message.getBody(java.lang.String) as String
        def jsonObject = new groovy.json.JsonSlurper().parseText(payload)
        def headers = message.getHeaders()
        def properties = message.getProperties()
        def projectName = properties.getOrDefault("projectName", jsonObject?.projectName) ?: ""
        def integrationID = properties.getOrDefault("integrationID", jsonObject?.integrationID) ?: ""
        def meta_environment = Framework_ValueMaps.frameworkVM("meta_environment", System.getenv("TENANT_NAME"), message, messageLog)
        def meta_integrationName = interfaceVM("meta_integrationName", projectName, integrationID, integrationID)
        def results = resolveEmailRecipients(jsonObject, projectName, integrationID)

        // We can set the errorType in the subject after resolving the email recipients
        String emailSubject = "BTP CI - ${meta_environment} - ${meta_integrationName} [${results.errorType} ERROR]"
        message.setProperty("emailSubject", emailSubject)

        // Generate an HTML table row containing a link to the MPL/cALM dashboard for this message
        def correlationID = jsonObject?.correlationID ?: headers.get(Constants.Header.SAP_CORRELATION_ID)
        def messageID = jsonObject?.messageID ?: properties.get(Constants.Property.SAP_MPL_ID)
        
        def labelStyle = "style='width:25%;text-align:left;'"
        def monitorLinks = getLinksForObservabilityTools(message, correlationID ?: messageID ?: "UnknownID", labelStyle)
        def jsonToHtmlTable = { jsonObj ->
            def tableHtml = new StringBuilder()
            tableHtml.append("<table border='1'>${monitorLinks}")
            jsonObj.each { key, value ->
                def prettyKey = formatPropertyName(key)
                def content = (prettyKey.equalsIgnoreCase("Messages") && value instanceof List) ? createMessagesHtml(value) : escapeHtml(value)
                tableHtml.append("<tr><th ${labelStyle}><b>${prettyKey}</b></th><td>${content}</td></tr>")
            }
            tableHtml.append("</table>")
            tableHtml.toString()
        }
        
        // Filter specified emailFilterValues logs only
        if (!message.getProperty("isEmailFilterApplied")) {
            def emailFilterValues = interfaceVM("emailFilterValues", projectName, integrationID, "E")
            def filterValues = emailFilterValues.toUpperCase().split(",").collect {
                Constants.ILCD.normalizeLogLevel(it.trim())
            }
            def filteredItems = (jsonObject.messages instanceof List ? jsonObject.messages : [jsonObject.messages]).findAll {
                if (it == null) return false
                def itemLevel = Constants.ILCD.normalizeLogLevel(it.logLevel)
                filterValues.contains(itemLevel)
            }
            jsonObject.messages = filteredItems           
        }
        
        // Build the HTML table
        def htmlTable = jsonToHtmlTable(jsonObject)
        message.setBody(htmlTable)
    }

    /**
     * Checks the JSON log for error_type at root level or errorType within an error log entry.
     */
    private String resolveErrorType(def jsonObject, String projectName, String integrationID) {
        def errTypeFallback = Constants.ILCD.ExceptionHandler.VM_KEY_EMAIL_RECIP_TECH
        def errTypeProp = this.message ? this.message.getProperty(Constants.ILCD.ExceptionHandler.ERR_TYPE_PROPERTY) : null
        def errTypeJson = jsonObject?.errorType ? jsonObject.errorType.toString()?.toUpperCase() : jsonObject[Constants.ILCD.ExceptionHandler.MPL_CH_ERR_TYPE]
        def errTypeJsonMsgs = (jsonObject?.messages ?: []).findResult(errTypeFallback, { it?.errorType })
        def errorType = errTypeProp ?: errTypeJson ?: errTypeJsonMsgs ?: errTypeFallback
        return Constants.ILCD.ExceptionHandler.normalizeErrorType(errorType)
    }

    /**
     * Resolves the final email recipients using error type preference and additionally configured DL fallback,
     * also removes placeholders from being considered as recipients.
     */
    private Map resolveEmailRecipients(def jsonObject, String projectName, String integrationID) {
        def defaultDl = frameworkVM(Constants.ILCD.ValueMaps.VM_KEY_EMAIL_DL_ADDR, "")
        def defaultDlFields = frameworkVM(Constants.ILCD.ValueMaps.VM_KEY_EMAIL_DL_FIELDS, "Cc")
        def result = getRecipientsForErrorType(jsonObject, projectName, integrationID)
        def dlFields = defaultDl && defaultDlFields ? defaultDlFields.split(",")?.toList() : []
        
        // Ensure there aren't placeholder values (caused by the CSV Template placeholder values)
        def emailTo = result.emailRecipients.split(/[;,]/)
            .collect { it.trim().toLowerCase() }   // normalize case
            .findAll { it && !["user1@its.jnj.com", "user2@its.jnj.com"].contains(it) }
        
        // Add DL to recipients or CC if configured
        if (dlFields.contains("To")) {
            emailTo << defaultDl
        } else if (dlFields.contains("Cc")) {
            this.message.setHeader("Cc", defaultDl)
            debug("email_Cc", defaultDl, messageLog)
        }

        def verifiedRecipients = emailTo && emailTo.size() > 0 && emailTo.unique() ? emailTo.unique() : []
        def emailRecipients = verifiedRecipients.join(";").trim() ?: ""
        debug("email_To", emailRecipients, messageLog)
        this.message.setProperty("emailRecipients", emailRecipients)
        this.message.setProperty("hasRecipients", !emailRecipients.isEmpty())
        
        return [emailRecipients: emailRecipients, errorType: result.errorType]
    }

    /**
     * Does a lookup for email recipients based on errorType (functional/technical) with fallback.
     */
    private Map getRecipientsForErrorType(def jsonObject, String projectName, String integrationID) {
        def normalizedType = resolveErrorType(jsonObject, projectName, integrationID)
        def recipientKey = (normalizedType == Constants.ILCD.ExceptionHandler.ERR_TYPE_FUNC)
            ? Constants.ILCD.ExceptionHandler.VM_KEY_EMAIL_RECIP_FUNC
            : Constants.ILCD.ExceptionHandler.VM_KEY_EMAIL_RECIP_TECH
        def emailRecipients = interfaceVM(recipientKey, projectName, integrationID, null)
        // Fallback to technical if functional is missing
        if (!emailRecipients?.trim() && normalizedType == Constants.ILCD.ExceptionHandler.ERR_TYPE_FUNC) {
            emailRecipients = interfaceVM(Constants.ILCD.ExceptionHandler.VM_KEY_EMAIL_RECIP_TECH, projectName, integrationID, "")
            message.setProperty("missingFunctionalRecipients", "true")
            debug("missing_functionalEmailRecipients", "Missing Value Map key 'functionalEmailRecipients' for errorType 'FUNCTIONAL'. Used fallback.", messageLog)
        }
        // Final fallback if still missing
        if (!emailRecipients?.trim()) {
            emailRecipients = ""
            message.setProperty("missingAllRecipients", "true")
            debug("missing_allEmailRecipients", "No Value Map key found for either functional or technical recipients. Email will not be sent.", messageLog)
        } else {
            debug("resolved_emailRecipients", "Used key: ${recipientKey}, value: ${emailRecipients ?: '[empty]'}", messageLog)
        }

        return [emailRecipients: (emailRecipients ?: ""), errorType: normalizedType]
    }

    /**
     * Escape HTML special characters for safe rendering.
     */
    def escapeHtml(val) {
        if (val == null) return ''
        def s = val.toString()
        s = s.replace('&', '&amp;')
        s = s.replace('<', '&lt;')
        s = s.replace('>', '&gt;')
        s = s.replace('"', '&quot;')
        s = s.replace("'", '&#39;')
        return s.replace('\n', '<br>')
    }

    /**
     * Takes a List of Map and produces one-dimensional HTML description lists. 
     */
    def createMessagesHtml(List<Map> messages) {
        def error = "style='color:${Constants.Style.JNJ_RED_HEX} !important;'"
        def sb = new StringBuilder()
        messages.each { msgMap ->
            sb.append("<dl style='font-size:0.8rem;margin-bottom:0.8em;'>")
            msgMap.each { k, v ->
                def style = k.equalsIgnoreCase("logLevel") ? error : ""
                sb.append("<dt><b ${style}>${escapeHtml(k)}</b></dt>")
                sb.append("<dd ${style}>${escapeHtml(v)}</dd>")
            }
            sb.append("</dl>")
        }
        return sb.toString()
    }

    /**
     * Automatically format the property names with capitalization and spaces.
     */
    private String formatPropertyName(String propertyName) {
        propertyName
            .replaceAll("_", " ")
            .replaceAll(/([a-z])([A-Z])/, '$1 $2')
            .split(' ').collect { it.capitalize() }.join(' ')
    }

    /**
     * Constructs a URL to the MPL & cALM dashboards, if either is available
     */
    private String getLinksForObservabilityTools(Message message, String searchID, String labelStyle) {
        def tenantName = System.getenv("TENANT_NAME")
        def domain = System.getenv("IT_TENANT_UX_DOMAIN")
        def cALM = "";
        def mpl = "";
        if (searchID && tenantName) {
            def calmBase = "https://jnj-cloudalm.us10.alm.cloud.sap/shell/run?sap-ui-app-id=com.sap.crun.imapp.ui#/monitoring/detail/${tenantName}/SAP_CPI"
            def calmAnchor = "<a href=\"${calmBase}\" target=\"_blank\">Search for Correlation ID: ${searchID}</a>"
            cALM = "<tr><th ${labelStyle}><b>cALM Dashboard</b></th><td>${calmAnchor}</td></tr>"
            if (domain) {
                def idJson = java.net.URLEncoder.encode("{\"identifier\":\"${searchID}\"}", "UTF-8")
                def fullURL = "https://${tenantName}.integrationsuite.${domain}/shell/monitoring/Messages/" + idJson
                def mplAnchorTag = "<a href=\"${fullURL}\" target=\"_blank\">${searchID}</a>"
                mpl = "<tr><th ${labelStyle}><b>Message Processing Log URL</b></th><td>${mplAnchorTag}</td></tr>"
            }
        }
        return mpl + cALM;
    }

    private String interfaceVM(String key, String projectName, String integrationID, def defaultValue = "") {
        return Framework_ValueMaps.interfaceVM(key, projectName, integrationID, defaultValue, message, messageLog)
    }

    private String frameworkVM(String key, String defaultValue = null) {
        return Framework_ValueMaps.frameworkVM(key, defaultValue, message, messageLog)
    }

    static void debug(key, value, log) {
        if (!log || !key || !value) return
        log.setStringProperty("${key}", "${value}")
    }
}
