package src.main.resources.script

class Constants {

    // Constants.Property - Standard + Commonly Used Properties 
    static class Property {
        static final String MPL_CUSTOM_STATUS   = "SAP_MessageProcessingLogCustomStatus"
        static final String MPL_LEVEL_INTERNAL  = "SAP_MPL_LogLevel_Internal"
        static final String MPL_LEVEL_OVERALL   = "SAP_MPL_LogLevel_Overall"
        static final String MPL_LEVEL_EXTERNAL  = "SAP_MPL_LogLevel_External"
        static final String SAP_MPL_ID          = "SAP_MessageProcessingLogID"
        static final String SAP_ERR_STEP_ID     = "SAP_ErrorModelStepID"
        static final String SAP_IS_REDELIVERY_ENABLED = "SAP_isComponentRedeliveryEnabled"
        static final String CAMEL_EXC_CAUGHT    = "CamelExceptionCaught"
        static final String AEM_RMID            = "ReplicationGroupMessageID"
    }

    // Constants.Header - Standard + Commonly Used Headers
    static class Header {
        static final String SAP_CORRELATION_ID  = "SAP_MplCorrelationId"
        static final String SAP_MESSAGE_TYPE    = "SAP_MessageType"
        static final String SAP_APP_ID          = "SAP_ApplicationID"
        static final String SAP_SENDER          = "SAP_Sender"
        static final String SAP_RECEIVER        = "SAP_Receiver"
        static final String CAMEL_RESPONSE_CODE = "CamelHttpResponseCode" 
        static final String CHARSET_UTF8        = "charset=utf-8"
        static final String CONTENT_TYPE        = "Content-Type"
        static final String CONTENT_TYPE_XML    = "application/xml"
        static final String CONTENT_TYPE_JSON   = "application/json"
        static final String CONTENT_TYPE_TEXT   = "text/plain"
        static final String HAS_ERROR           = "HasError"
    }

    // Constants.ILCD - Headers + Properties + Variables + anything else unique to ILCD
    static class ILCD {
        static final String VM_SRC_ID           = "projectName"
        static final String VM_TRGT_ID          = "integrationID"
        static final String VM_SRC_AGENCY       = "Input"
        static final String VM_TRGT_AGENCY      = "Output"
        static final String VM_GLOBAL_SRC_ID    = "IP_FoundationFramework"
        static final String VM_GLOBAL_TRGT_ID   = "VM_Framework_Global_Metadata"
        static final String LOG_STACK_PROPERTY  = "messageLog"
        static final String LOG_COUNT           = "framework_internal_log_counter"
        static final String EXC_PREFIX          = "ILCD_EXC"
        static final String EXC_SUFFIX          = "SCRIPT_ERROR"
        static final String ATTACH_DISABLED     = "LOG_ATTACHMENTS_DISABLED"
        static final String ATTACH_DISABLED_MSG = 
            "Log attachments are currently DISABLED. To enable, change the `setting_attachmentsDisabled` flag in the ${VM_GLOBAL_TRGT_ID} value map."

        static final List<String> META_FIELDS_TO_CUSTOM_HEADERS = [
            "integrationID",
            "integrationName",
            "projectName",
            "processArea",
            "priority",
            "sourceSystemConnectionType",
            "targetSystemConnectionType"
        ]
        
        // Constants.ILCD.Logger
        static class Logger {}
        // Constants.ILCD.ExceptionHandler
        static class ExceptionHandler {}
        // Constants.ILCD.Utils
        static class Utils {
            static final String SUCCESS_RESPONSE_MSG = "Request processed successfully"
        }
        // Constants.ILCD.Validator
        static class Validator {
            // MM_ERROR_PREFIX
            static final String MM_ERROR_PREFIX = "Mandatory Field"
            static final String EXC_CLASS = "SchemaValidationException"
            static final String EXC_CAUSE = "Bad Request: ${EXC_CLASS}"
            static final String VM_KEYS_XML_ROOT = "xmlRoot"
            static final String VM_KEYS_XML_PK = "xmlPrimaryKey"
            static final String VM_KEYS_XML_HEADERS = "xmlHeaders"
            static final String VM_KEYS_XML_SPLIT = "xmlSplitterNode"
            static final String LOG_INFO_MSG = "Successful mandatory field validation"
            static final String LOG_WARN_MSG = "Failed mandatory field validation"
            static final String ERR_MSG_PROP = "validationErrorMessage"
        }
        // Constants.ILCD.Batch
        static class Batch {
            static final String JOB_ID = "batchJobID"
            static final String NUM_FAILED = "batchRecordsNumFailed"
            static final String FAIL_IDS = "batchRecordFailIDs"
            static final String NUM_SUCCESS = "batchRecordsNumSuccess"
            static final String PASS_IDS = "batchRecordPassIDs"
        }
    }

    // Constants.Style - J&J Corporate Branding Styles 
    static class Style {
        // CSS color hex codes 
        static final String JNJ_RED_HEX = "#B41601"
        static final String JNJ_GRAY_HEX = "#828282"
        static final String JNJ_LITEGRAY_HEX = "#E5E5E5"
        static final String JNJ_DARKGRAY_HEX = "#646464"
        static final String JNJ_BLUE_HEX = "#12C2E9"
        static final String JNJ_DARKBLUE_HEX = "#0A8CAA"
        static final String JNJ_BLACK_HEX = "#000000"
        static final String JNJ_WHITE_HEX = "#FFFFFF"
    }
}
