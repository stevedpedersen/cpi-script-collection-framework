// Stub LoggerFactory for testing
package org.slf4j
class LoggerFactory {
    static Logger getLogger(String name) {
        return new Logger()
    }
}