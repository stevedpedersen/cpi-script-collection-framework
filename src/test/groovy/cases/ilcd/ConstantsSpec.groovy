package cases.ilcd

import spock.lang.*
import src.main.resources.script.Constants

class ConstantsSpec extends Specification {
    def "normalizeLogLevel handles string input"() {
        expect:
        Constants.ILCD.normalizeLogLevel("ERROR") == "ERROR"
        Constants.ILCD.normalizeLogLevel("warn") == "WARN"
        Constants.ILCD.normalizeLogLevel("i") == "INFO"
        Constants.ILCD.normalizeLogLevel("T") == "TRACE"
        Constants.ILCD.normalizeLogLevel("unknown", "DEBUG") == "DEBUG"
    }

    def "normalizeLogLevel handles list input"() {
        expect:
        Constants.ILCD.normalizeLogLevel(["ERROR"]) == "ERROR"
        Constants.ILCD.normalizeLogLevel(["warn"]) == "WARN"
        Constants.ILCD.normalizeLogLevel(["i"]) == "INFO"
        Constants.ILCD.normalizeLogLevel(["T"]) == "TRACE"
        Constants.ILCD.normalizeLogLevel(["unknown"], "INFO") == "INFO"
    }

    def "normalizeLogLevel returns default for empty input"() {
        expect:
        Constants.ILCD.normalizeLogLevel(null, "WARN") == "WARN"
        Constants.ILCD.normalizeLogLevel([], "ERROR") == "ERROR"
        Constants.ILCD.normalizeLogLevel("", "INFO") == "INFO"
    }

    def "normalizeLogLevel is case-insensitive and trims input"() {
        expect:
        Constants.ILCD.normalizeLogLevel("  error  ") == "ERROR"
        Constants.ILCD.normalizeLogLevel(["  warn  "]) == "WARN"
    }
}
