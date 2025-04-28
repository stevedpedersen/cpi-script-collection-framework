@Grab('org.yaml:snakeyaml:2.0')
import org.yaml.snakeyaml.Yaml
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import java.net.HttpURLConnection
import java.util.UUID
import groovy.json.JsonSlurper

// --- FUNCTIONS ---
def getAccessToken(tokenUrl, clientId, clientSecret) {
    def url = new URL(tokenUrl)
    def conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = 'POST'
    def authString = "${clientId}:${clientSecret}".bytes.encodeBase64().toString()
    conn.setRequestProperty('Authorization', "Basic ${authString}")
    conn.doOutput = true
    def body = 'grant_type=client_credentials'
    conn.outputStream.withWriter { it << body }
    def resp = conn.inputStream.getText('UTF-8')
    def json = new JsonSlurper().parseText(resp)
    return json.access_token
}

def getValueMappingXml(token, valueMappingId, apiHost) {
    def url = new URL("${apiHost}/api/v1/ValueMappingDesigntimeArtifacts(Id='${valueMappingId}',Version='Draft')/\$value")
    def conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty('Authorization', "Bearer ${token}")
    conn.setRequestProperty('Accept', 'application/xml')
    conn.connect()
    def xmlText = conn.inputStream.getText('UTF-8')
    println "--- API Response Start ---"
    println xmlText.take(200)
    println "--- API Response End ---"
    return xmlText
}

def putValueMappingXml(token, valueMappingId, apiHost, xmlText) {
    def url = new URL("${apiHost}/api/v1/ValueMappingDesigntimeArtifacts(Id='${valueMappingId}',Version='Draft')/\$value")
    def conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = 'PUT'
    conn.setRequestProperty('Authorization', "Bearer ${token}")
    conn.setRequestProperty('Content-Type', 'application/xml')
    conn.doOutput = true
    conn.outputStream.withWriter('UTF-8') { it << xmlText }
    def resp = conn.inputStream.getText('UTF-8')
    return resp
}

def main() {
    // --- CONFIGURATION ---
    def clientId = ''
    def clientSecret = ''
    def valueMappingId = 'VM_Dev_FrameworkMetadata'
    def apiHost = 'https://jnj-im-dev-na.it-cpi019.cfapps.us10-002.hana.ondemand.com:443'
    def tokenUrl = 'https://jnj-im-dev-na.authentication.us10.hana.ondemand.com/oauth/token'
    def yamlFile = 'dev_valueMappings.yaml'

    def yaml = new Yaml().load(new File(yamlFile).text)
    def token = getAccessToken(tokenUrl, clientId, clientSecret)
    def xmlText = getValueMappingXml(token, valueMappingId, apiHost)
    // Debug: Stop here if the response is not XML
    if (!xmlText.trim().startsWith('<')) {
        println "API did not return XML. Aborting."
        return
    }
    def xml = new XmlParser().parseText(xmlText)

    // --- Find the correct ValueMap section in YAML ---
    def vmYaml = yaml[valueMappingId]
    if (!vmYaml) {
        println "No mapping for ${valueMappingId} found in YAML."
        return
    }
    def schemaKey = vmYaml.keySet().find { it.startsWith('ValueMapSchema') }
    def schemaYaml = vmYaml[schemaKey]
    def valueMaps = schemaYaml['valueMaps']
    
    // --- Add new entries with generated UUIDs ---
    valueMaps.each { k, v ->
        if (k == 'new') {
            def newId = UUID.randomUUID().toString().replaceAll('-', '')
            v.each { srcKey, tgtVal ->
                // --- Add new mapping to XML ---
                def newEntry = new Node(null, 'ValueMapping', [id: newId])
                def src = new Node(newEntry, 'Source', [id: schemaYaml['SrcId'], agency: schemaYaml['SrcAgency']])
                def tgt = new Node(newEntry, 'Target', [id: schemaYaml['TgtId'], agency: schemaYaml['TgtAgency']])
                src.appendNode('Value', srcKey)
                tgt.appendNode('Value', tgtVal)
                xml.append(newEntry)
                println "Added new entry with id=${newId}, ${srcKey} -> ${tgtVal}"
            }
        }
    }
    
    // --- Write updated XML to file and upload ---
    def updatedXml = XmlUtil.serialize(xml)
    new File("updated_${valueMappingId}.xml").text = updatedXml
    println "Updated XML written to updated_${valueMappingId}.xml"
    def resp = putValueMappingXml(token, valueMappingId, apiHost, updatedXml)
    println "Upload response: ${resp}"
}

main()
