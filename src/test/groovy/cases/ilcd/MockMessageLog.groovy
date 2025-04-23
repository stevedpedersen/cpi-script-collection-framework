package ilcd

class MockMessageLog {
    def attachments = []
    def headers = [:]
    def properties = [:]
    void addAttachmentAsString(String name, String body, String type) {
        attachments << [name: name, body: body, type: type]
    }
    void addCustomHeaderProperty(String name, String value) {
        headers[name] = value
    }
    void setProperty(String name, Object value) {
        properties[name] = value
    }
    // TODO: Add more mock methods as needed
}
