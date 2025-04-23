package org.apache.camel.support
import org.apache.camel.impl.DefaultCamelContext
class DefaultExchange extends org.apache.camel.Exchange {
    DefaultCamelContext ctx
    DefaultExchange(DefaultCamelContext ctx) {
        this.ctx = ctx
    }
    DefaultCamelContext getIn() {
        return ctx
    }
}