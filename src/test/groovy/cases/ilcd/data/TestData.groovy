package cases.ilcd.data

class TestData {
    static final String SAMPLE_JSON = '{"foo": "bar"}'
    static final String SAMPLE_MSG = 'Test message body.'
    static final Map<String, Object> SAMPLE_HEADERS = [k1: 'v1', k2: 'v2']
    static final Map<String, Object> SAMPLE_PROPERTIES = [p1: 42, p2: true]
    static final Map<String, Object> SAP_STANDARD_HEADERS = [
        // General SAP Headers
        'SAP_ApplicationID': 'APPID',
        'SAP_MessageType': 'ZOM_RETURN',
        'SAP_MplCorrelationId': 'AGgM_nYt1pa4VGeDHH68OaiAzrCa',
        'SAP_MessageProcessingLogID': 'AGgLw48ugSNUD6it_8D0swWSi4Gp', // can be set as both headers & propers
        // Mail Adapter
        'Archived-At': 'https://archive.example.com/mail/123',
        'Date': '2023-01-01T12:00:00Z',
        'From': 'sender@example.com',
        'Message-ID': '<msgid@example.com>',
        'Reply-to': '<replyid@example.com>',
        'Cc': 'cc@example.com',
        // HTTP/HTTPS Adapter
        'CamelHttpMethod': 'POST',
        'CamelHttpPath': '/api/v1/resource',
        'CamelHttpQuery': 'id=1234',
        'CamelHttpResponseCode': 200,
        'CamelHttpUri': 'https://example.com/api/v1/resource',
        'CamelHttpUrl': 'https://example.com/api/v1/resource',
        'CamelServletContextPath': '/abcd/1234',
        'Content-Encoding': 'gzip',
        'Content-Type': 'application/json',
        // SFTP/FTP Adapter
        'CamelFileName': 'file.txt',
        'CamelFileNameOnly': 'file.txt',
        'CamelFileParent': '/files',
        'CamelRemoteFileInputStream': 'stream',
        // Aggregator/Splitter
        'CamelAggregatedCompletedBy': 'timeout',
        'CamelSplitComplete': true,
        'CamelSplitIndex': 0,
        'CamelSplitSize': 3,
        // XML Signer
        'CamelXmlSignatureTransformMethods': 'http://www.w3.org/2000/09/xmldsig#enveloped-signature',
        'CamelXmlSignatureXAdESQualifyingPropertiesId': 'qpId',
        'CamelXmlSignatureXAdESSignedDataObjectPropertiesId': 'sdopId',
        'CamelXmlSignatureXAdESSignedSignaturePropertiesId': 'sspId',
        'CamelXmlSignatureXAdESDataObjectFormatEncoding': 'UTF-8',
        'CamelXmlSignatureXAdESNamespace': 'http://uri.etsi.org/01903/v1.3.2#',
        'CamelXmlSignatureXAdESPrefix': 'xades',
        // AS2 Adapter
        'SAP_AS2_Outbound_Authentication_Type': 'Basic',
        'SAP_AS2_Outbound_Content_Transfer_Encoding': 'binary',
        'SAP_AS2_Outbound_Proxy_Type': 'HTTP',
        'SAP_AS2_Outbound_Compress_Message': true,
        'SAP_AS2_Outbound_Content_Type': 'application/edi-x12',
        'SAP_AS2_Outbound_Encrypt_Message': true,
        'SAP_AS2_Outbound_Encryption_Key_Length': 256,
        'SAP_AS2_Outbound_Encryption_Algorithm': 'AES256',
        'SAP_AS2_Outbound_Encryption_Public_Key': 'publicKeyAlias',
        'SAP_AS2_Outbound_Async_Mdn_Url': 'https://partner.example.com/as2/mdn',
        'SAP_AS2_Outbound_Mdn_Request_Mic': true,
        'SAP_AS2_Outbound_Mdn_Request_Signing': true,
        'SAP_AS2_Outbound_Mdn_Signing_Algorithm': 'SHA256',
        'SAP_AS2_Outbound_Mdn_Type': 'MIC',
        'SAP_AS2_Outbound_Mdn_Verify_Mic': true,
        'SAP_AS2_Outbound_Mdn_Verify_Signature': true,
        'SAP_AS2_Outbound_Sign_Message': true,
        'SAP_AS2_Outbound_Signing_Algorithm': 'SHA256',
        'SAP_AS2_Outbound_Signing_Private_Key_Alias': 'privateKeyAlias',
        // AS4 Adapter
        'SAP_AS4_Outbound_Authentication_Type': 'saml',
        'SAP_AS4_Outbound_Username_Token': 'plainTextPassword',
        'SAP_AS4_Outbound_Security_Type': 'signAndEncrypt',
        'SAP_AS4_Outbound_Sign_Message': true,
        'SAP_AS4_Outbound_Signing_Algorithm': 'SHA256/RSA',
        'SAP_AS4_Outbound_Encryption_Cert': 'certAlias',
        'SAP_AS4_Outbound_Encryption_Algorithm': 'AES256',
        'SAP_AS4_Outbound_Save_Receipt': true,
        'SAP_AS4_Outbound_Verify_Receipt_Username_Token': 'required',
        'SAP_AS4_Outbound_Verify_Receipt': true,
        'SAP_AS4_Outbound_Pull_Username_Token': 'plainTextPassword',
        'SAP_AS4_Inbound_Sign_Message': true,
        'SAP_AS4_Inbound_Signing_Algorithm': 'SHA256/RSA',
        'SAP_AS4_Inbound_Verify_Sign': true,
        'SAP_AS4_Outbound_Verify_Response_Username_Token': 'required',
        // Data Store
        'SAP_DataStoreCreatedAt': 1680000000000,
        'SAP_DataStoreRetries': 1,
        'SAP_DataStoreExpiresAt': 1680003600000,
        // EDI Splitter
        'SAP_EDI_Document_Number': '123456789',
        'SAP_EDISPLITTER_EDIFACT_DECIMAL_CHARACTER': 'dot',
        'SAP_EDISPLITTER_EDIFACT_CONTRL_MSG_VERSION': 'D.96A',
        'SAP_EDISPLITTER_EDIFACT_CREATE_ACK': true,
        'SAP_EDISPLITTER_EDIFACT_INCLUDE_UNA': true,
        'SAP_EDISPLITTER_EDIFACT_INTERCHANGE_NUMBER': 'INT123',
        'SAP_EDISPLITTER_EDIFACT_UNIQUE_INTERCHANGE_NUMBER': 'required',
        'SAP_EDISPLITTER_EDIFACT_NUMBER_RANGE': 'range1',
        'SAP_EDISPLITTER_EDIFACT_SCHEMA_SOURCE': 'Header',
        'SAP_EDISPLITTER_EDIFACT_SOURCE_ENCODING': 'UTF-8',
        'SAP_EDISPLITTER_EDIFACT_TRANSACTION_MODE': 'Interchange',
        'SAP_EDISPLITTER_EDIFACT_VALIDATE_MESSAGE': true,
        'SAP_EDISPLITTER_EDIFACT_VALIDATION_METHOD': 'Envelope',
        'SAP_EDISPLITTER_X12_CREATE_ACK': true,
        'SAP_EDISPLITTER_X12_EXCLUDE_AK3_AK4': false,
        'SAP_EDISPLITTER_X12_INTERCHANGE_NUMBER': 'X12INT123',
        'SAP_EDISPLITTER_X12_UNIQUE_INTERCHANGE_NUMBER': 'required',
        'SAP_EDISPLITTER_X12_NUMBER_RANGE': 'range2',
        'SAP_EDISPLITTER_X12_SCHEMA_SOURCE': 'Header',
        'SAP_EDISPLITTER_X12_SOURCE_ENCODING': 'UTF-8',
        'SAP_EDISPLITTER_X12_TRANSACTION_MODE': 'Interchange',
        'SAP_EDISPLITTER_X12_VALIDATE_MESSAGE_OPTION': true,
        'SAP_EDISPLITTER_997_GROUP_CONTROL_NUMBER': 'numberRange',
        'SAP_EDISPLITTER_997_UNIQUE_GROUP_CONTROL_NUMBER': 'required',
        'SAP_EDISPLITTER_997_TRANSACTION_SET_NUMBER': 'numberRange',
        'SAP_EDISPLITTER_997_UNIQUE_TRANSACTION_SET_NUMBER': 'required',
        // ELSTER Receiver
        'SAP_ERiCResponse': 'OK',
        // IDoc Adapter
        'SapIDocType': 'WPDTAX01',
        'SapIDocTransferId': '0000000000166099',
        'SapIDocDbId': '0000000000160816',
        // XI/JMS Adapter
        'SapQualityOfService': 'BestEffort',
        'SapPlainSoapQueueId': 'QID123',
        // Passport
        'SAP-PASSPORT': 'passportValue',
    ]
    static final Map<String, Object> SAP_STANDARD_PROPERTIES = [
        // General SAP Properties
        'SAP_isComponentRedeliveryEnabled': false,
        'SAP_DataStoreRetries': 0,
        'SAP_MessageProcessingLogID': 'AGgLw48ugSNUD6it_8D0swWSi4Gp',
        // Error Model
        'SAP_ErrorModelStepID': 'STEP01',
        // Logging
        'SAP_MessageProcessingLogCustomStatus': 'customStatus',
        // Log Level
        'SAP_MessageProcessingLogLevel': 'INFO',
        // OData V2
        'SAP_ODataV2_RefreshCacheOnExpiry': false,
        // Mail Encryption/Signature
        'SAP_MAIL_ENCRYPTION_DETAILS_DECRYPTION_ALIAS (String)': 'alias1',
        'SAP_MAIL_ENCRYPTION_DETAILS_DECRYPTION_OK (boolean)': true,
        'SAP_MAIL_ENCRYPTION_DETAILS_ENCRYPTED (boolean)': true,
        'SAP_MAIL_ENCRYPTION_DETAILS_ERROR_MESSAGES (String)': '',
        'SAP_MAIL_ORIGINAL_MESSAGE': 'originalMessage',
        'SAP_MAIL_SIGNATURE_OVERALL_VERIFICATION_OK (boolean)': true,
        'SAP_MAIL_SIGNATURE_DETAILS_CERTIFICATES (Array of java.security.cert.X509Certificate)': [],
        'SAP_MAIL_SIGNATURE_DETAILS_VERIFICATION_OK (Array of boolean)': [true],
        'SAP_MAIL_SIGNATURE_DETAILS_ERROR_MESSAGES (Array of String)': [],
    ]
    static final Map<String, Object> VALUE_MAP_ENTRIES = [
        'emailFilterValues': 'E',
        'emailSensitiveFields': '',
        'emailRecipients': 'user1@its.jnj.com',
        'loggerSensitiveFields': 'mySensitiveField',
        'loggerPayloadLogPoints': 'START',
        'meta_integrationID': 'IF_ReturnsOrder_Post_OPENTEXT_TIME',
        'meta_integrationName': 'ReturnsOrder from OpenText to Send',
        'meta_iFlowName': 'IF_ReturnsOrder_Post_OPENTEXT_TIME',
        'meta_priority': 'High',
        'meta_processArea': 'Deliver',
        'meta_projectName': 'TranscendIM',
        'meta_sourceSystemConnectionType': 'AS2',
        'meta_targetSystemConnectionType': 'SOAP',
        'meta_sourceSystemName': 'S4',
        'meta_targetSystemName': 'CFIN',
        // Add more from Constants.Property as needed
    ]
}