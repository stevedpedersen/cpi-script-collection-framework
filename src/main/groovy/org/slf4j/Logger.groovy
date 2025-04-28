// Stub Logger class for testing
package org.slf4j
class Logger {
    void debug(Object msg) { println "[DEBUG] $msg" }
    void debug(Object msg, Object ex) { println "[DEBUG] $msg $ex" }
    void warn(Object msg) { println "[WARN] $msg" }
    void error(Object msg) { println "[ERROR] $msg" }
    void error(Object msg, Object ex) { println "[ERROR] $msg $ex" }
}