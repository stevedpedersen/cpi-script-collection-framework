package org.apache.camel.support
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Exchange

class DefaultExchange extends Exchange {
    DefaultCamelContext context
    Object body
    Map<String, Object> headers = [:]
    Map<String, Object> properties = [:]

    DefaultExchange(DefaultCamelContext ctx) {
        this.context = ctx
    }

    DefaultCamelContext getIn() {
        return context
    }

    Object getBody() { return body }
    void setBody(Object b) { this.body = b }
    Object getHeader(String name) { return headers.get(name) }
    void setHeader(String name, Object value) { headers.put(name, value) }
    void setProperty(String name, Object value) { properties.put(name, value) }
    Object getProperty(String name) { return properties.get(name) }
}