// LOG_BATCH_SUMMARY.groovy
import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants

/**
 * Adds 3 properties to custom header properties, in addition to normal logs:
 *  - batchJobID
 *  - batchRecordFailIDs // (comma-sep) e.g. "REC001,REC002,REC003"
 *  - batchRecordPassIDs // (comma-sep) e.g. "REC004,REC005,REC006"
 */
def Message processData(Message message) {
    int maxHeaderChars = 200
    def props = message.getProperties()
    def headers = message.getHeaders()
    def messageLog = messageLogFactory.getMessageLog(message)
    if (!messageLog) return message

    try {
        // Build a stable "batchID" from jobID or fallback to correlationID/filename
        def rawBatchID = props?.batchJobID ?: headers?.filename ?: props?.filename
            ?: headers?.SAP_MessageType ?: headers?.correlationID ?: "unknownBatch"

        // Pass/fail IDs as comma-separated strings (set by your flow logic)
        def failIds = props.get(Constants.ILCD.Batch.FAIL_IDS)  // e.g. "REC001,REC002,REC003"
        def passIds = props.get(Constants.ILCD.Batch.PASS_IDS)  // e.g. "REC004,REC005,REC006"
        def numFailed = props.get(Constants.ILCD.Batch.NUM_FAILED)
        def numSuccess = props.get(Constants.ILCD.Batch.NUM_SUCCESS)

        // Minimal check
        if (!failIds && !passIds && !numFailed && !numSuccess) {
            // No pass/fail IDs to log
            return message
        }

        // Always record the batch ID in a custom header
        messageLog.addCustomHeaderProperty("batch_jobID", rawBatchID)

        // Safely remove leading comma, if present
        if (failIds?.startsWith(",")) {
            failIds = failIds.substring(1)
        }
        if (passIds?.startsWith(",")) {
            passIds = passIds.substring(1)
        }

        // If number-of-failed was not set, default to counting from failIds
        if (failIds || numFailed) {
            numFailed = numFailed ?: failIds?.split(",")?.size()?.toString() ?: "0"
            message.setProperty(Constants.ILCD.Batch.NUM_FAILED, numFailed)
            message.setProperty(Constants.ILCD.Batch.FAIL_IDS, failIds)
            messageLog.addCustomHeaderProperty("batch_failIDs", numFailed)
            // chunkIdsIntoHeaders(messageLog, "batch_failIDs", failIds, maxHeaderChars)
        }
        // If number-of-successful was not set, default to counting from passIds
        if (passIds || numSuccess) {
            numSuccess = numSuccess ?: passIds?.split(",")?.size()?.toString() ?: "0"
            message.setProperty(Constants.ILCD.Batch.NUM_SUCCESS, numSuccess)
            message.setProperty(Constants.ILCD.Batch.PASS_IDS, passIds)
            messageLog.addCustomHeaderProperty("batch_passIDs", numSuccess)
            // chunkIdsIntoHeaders(messageLog, "batch_passIDs", passIds, maxHeaderChars)
        }

        def customStatus = (numFailed && numFailed.trim() != "0" && numFailed.toInteger() > 0)
            ? "Failed - ${numFailed} failures occurred"
            : "Success" + (numSuccess && numSuccess.toInteger() > 0 ? " - ${numSuccess} processed" : "")
        message.setProperty(Constants.Property.MPL_CUSTOM_STATUS, customStatus)

        // do normal ILCD logs
        def logger = new Framework_Logger(message, messageLog)
        logger.logMessage("LOG_BATCH_SUMMARY", "INFO",
            "numFailed=${numFailed}&numSuccess=${numSuccess}&failIDS=${failIds}&passIDs=${passIds}")
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "LOG_BATCH_SUMMARY", true)
    }

    return message
}

// Utility function: chunk a comma-separated string into multiple header entries
def chunkIdsIntoHeaders(def msgLog, String headerName, String commaSeparatedIds, int maxChars) {
    if(!commaSeparatedIds?.trim()) return

    def idList = commaSeparatedIds.split(',')*.trim()
    def currentChunk = []

    idList.each { id ->
        // If we add this ID to the chunk, does it exceed maxChars?
        def candidate = (currentChunk + [id]).join(',')
        if(candidate.size() > maxChars) {
            // Flush current chunk to a custom header
            if(currentChunk) {
                msgLog.addCustomHeaderProperty(headerName, currentChunk.join(','))
            }
            // Start a new chunk with the current ID
            currentChunk = [id]
        } else {
            currentChunk << id
        }
    }
    // Flush any leftover chunk
    if(currentChunk) {
        msgLog.addCustomHeaderProperty(headerName, currentChunk.join(','))
    }
}
