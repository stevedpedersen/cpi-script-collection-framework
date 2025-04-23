// TestData.groovy
package src.main.resources.data
class TestData {
    static final Map<String, Object> SAP_STANDARD_HEADERS = [
        'SAP_MessageProcessingLogID': '12345',
        'SAP_ApplicationID': 'APPID',
        'SAP_Component': 'COMP',
        'SAP_DataStoreRetries': 0,
        'SAP_isComponentRedeliveryEnabled': false,
        'SAP_IntegrationID': 'IF_ReturnsOrder_Post_OPENTEXT_TIME',
        'SAP_IntegrationName': 'ReturnsOrder from OpenText to Send',
        'SAP_MsgType': 'ZOM_RETURN',
        'SAP_Priority': 'High',
        'SAP_ProcessArea': 'Deliver',
        'SAP_ProjectName': 'TranscendIM',
        'SAP_SourceSystemConnectionType': 'AS2',
        'SAP_TargetSystemConnectionType': 'SOAP',
        // Add more from Constants.Header as needed
    ]
    static final Map<String, Object> SAP_STANDARD_PROPERTIES = [
        'SAP_isComponentRedeliveryEnabled': false,
        'SAP_DataStoreRetries': 0,
        'SAP_MessageProcessingLogID': '12345',
        'SAP_ApplicationID': 'APPID',
        'SAP_Component': 'COMP',
        'SAP_IntegrationID': 'IF_ReturnsOrder_Post_OPENTEXT_TIME',
        'SAP_IntegrationName': 'ReturnsOrder from OpenText to Send',
        'SAP_MsgType': 'ZOM_RETURN',
        'SAP_Priority': 'High',
        'SAP_ProcessArea': 'Deliver',
        'SAP_ProjectName': 'TranscendIM',
        'SAP_SourceSystemConnectionType': 'AS2',
        'SAP_TargetSystemConnectionType': 'SOAP',
        // Add more from Constants.Property as needed
    ]
}