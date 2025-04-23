import com.sap.gateway.ip.core.customdev.util.Message

Message processData(Message message) {
    def body = message.getBody(String)

    if (!body || body.trim().isEmpty()) {
        throw new Exception("Empty input response.")
    }

    // First-level parse of main XML
    def parser = new XmlSlurper(false, false)
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    def parsed = parser.parseText(body)

    // Get escaped XML content inside <processActionReturn>
    def encodedXml = parsed.'**'.find { it.name() == 'processActionReturn' }?.text()

    if (!encodedXml || encodedXml.trim().isEmpty()) {
        throw new Exception("No propertylist content found in response.")
    }

    // Unescape and parse the CDATA XML string (decode HTML entities like &lt;)
    def decodedXml = encodedXml.replaceAll('&lt;', '<')
                               .replaceAll('&gt;', '>')
                               .replaceAll('&amp;', '&')

    def innerParsed = parser.parseText(decodedXml)

    // Check for <property id="error">
    def errorNode = innerParsed.'**'.find { it.name() == 'property' && it.@id == 'error' }
    if (errorNode && errorNode.text().trim()) {
        def errorMessage = errorNode.text().trim()
        throw new Exception("Failure in response: ${errorMessage}")
    }

    // Check <property id="status"> == FAILED
    def statusNode = innerParsed.'**'.find { it.name() == 'property' && it.@id == 'status' }
    if (statusNode && statusNode.text().toUpperCase() == "FAILED") {
        throw new Exception("Response status is FAILED.")
    }

    return message
}