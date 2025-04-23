package src.main.resources.data
class ILCD_EXC {
    static String attachment = """
—————————————————
EXCEPTION SUMMARY
—————————————————
 Script/Function                         : PARSE_METADATA.filterLogs                                                                           
 Exception Message                       : No signature of method: static src.main.resources.script.Framework_Logger.normalizeLogLevel() is app... (truncated)
 Cause                                   : Unknown                                                                                             
 Details                                 : N/A                                                                                                 
 Stack Trace                             : 
groovy.lang.MissingMethodException: No signature of method: static src.main.resources.script.Framework_Logger.normalizeLogLevel() is applicable for argument types: (java.lang.String) values: [E]
Possible solutions: normalizeLogLevel(java.lang.String), normalizeLogLevel(java.lang.String, src.main.resources.script.Framework_Logger)
	at groovy.lang.MetaClassImpl.invokeStaticMissingMethod(MetaClassImpl.java:1518)
	at groovy.lang.MetaClassImpl.invokeStaticMethod(MetaClassImpl.java:1504)
	at org.codehaus.groovy.runtime.callsite.StaticMetaClassSite.call(StaticMetaClassSite.java:52)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:128)
	at src.main.resources.script.Framework_Utils\$_filterLogs_closure8.doCall(Framework_Utils.groovy:250)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:98)
	at groovy.lang.MetaMethod.doMethodInvoke(MetaMethod.java:325)
	at org.codehaus.groovy.runtime.metaclass.ClosureMetaClass.invokeMethod(ClosureMetaClass.java:264)
	at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1034)
	at groovy.lang.Closure.call(Closure.java:420)
	at groovy.lang.Closure.call(Closure.java:436)
	at org.codehaus.groovy.runtime.DefaultGroovyMethods.collect(DefaultGroovyMethods.java:3241)
	at org.codehaus.groovy.runtime.DefaultGroovyMethods.collect(DefaultGroovyMethods.java:3212)
	at org.codehaus.groovy.runtime.dgm\$66.invoke(Unknown Source)
	at org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite\$PojoMetaMethodSiteNoUnwrapNoCoerce.invoke(PojoMetaMethodSite.java:274)
	at org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite.call(PojoMetaMethodSite.java:56)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:128)
	at src.main.resources.script.Framework_Utils.filterLogs(Framework_Utils.groovy:249)
	at src.main.resources.script.Framework_Utils\$filterLogs.call(Unknown Source)
	at Script12.filterLogs(Script12.groovy:87)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:98)
	at groovy.lang.MetaMethod.doMethodInvoke(MetaMethod.java:325)
	at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1225)
	at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1092)
	at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1034)
	at groovy.lang.Closure.call(Closure.java:420)
	at org.codehaus.groovy.jsr223.GroovyScriptEngineImpl.callGlobal(GroovyScriptEngineImp... (truncated)

——————————
PROPERTIES
——————————
 iFlowName                               : IF_IN0411_Apheresis_POST_BTPIS_CMO                                                                  
 reqpayload                              : <ZCRT_MILESTONE>
            <IDOC BEGIN="1">
                <EDI_DC40 SEGMENT="1">
               ... (truncated)
 integrationName                         : IN0411: CarT Apheresis Milestone                                                                    
 meta_attribute_EventID                  : 25BC4023                                                                                            
 SAP_InspectModelStepId2615              : CallActivity_62340                                                                                  
 CamelExternalRedelivered                : false                                                                                               
 SAP_MessageProcessingLogConfiguration   : MplConfiguration [persistOnlyOnIntermediateError=false, mplActive=true, logLevel=INFO, logLevelRepli... (truncated)
 SAP_AuthHeaderName                      : Proxy-Authorization                                                                                 
 SAP_RunId                               : AGgHmV1myNqcLFd7vwZAWfF1Bsi-                                                                        
 StatusProvider_CAMEL_C139129D0705CF0-000000000000000F: StatusProvider [status=com.sap.it.op.mpl.message.status.ProcessingStatus@53031067]                  
 SAP_MonitoringStateProperties           : {com.sap.it.op.mpl.TypedMessageProcessingLogKey@33c06023=C139129D0705CF0-000000000000000F, com.sap.i... (truncated)
 cache_hit_count                         : 2798                                                                                                
 cache_disabled                          : false                                                                                               
 SAP_MPL_LogLevel_Overall                : INFO                                                                                                
 ILCD_EXC_inProgress                     : true                                                                                                
 SAP_MessageProcessingLogID              : AGgHmV2189N1LRStZv7szZHLhEhz                                                                        
 initial_Payload                         : {
    "projectName": "CART_CMO_Integration",
    "systemName": "jnj-im-dev-na",
    "messageID": "AG... (truncated)
 text                                    :                                                                                                     
 CamelBinding                            : com.sap.esb.camel.jms.impl.Binding@fc3ff61                                                          
 InitialPayload                          : <ZCRT_MILESTONE>
            <IDOC BEGIN="1">
                <EDI_DC40 SEGMENT="1">
               ... (truncated)
 log_statusCode                          :                                                                                                     
 cache_ttl_seconds                       : 300                                                                                                 
 SAP_InspectModelStepId2944              : ParallelGateway_5                                                                                   
 SAP_MessageProcessingLogCustomStatus    : Failed                                                                                              
 forceDisableEmail                       : false                                                                                               
 SAP_isComponentRedeliveryEnabled        : true                                                                                                
 errorStatusCode                         : 500                                                                                                 
 errorExceptionMessage                   : Runtime exception during processing target field mapping  /CMO/data/details. The root message is: Un... (truncated)
 projectName                             : CART_CMO_Integration                                                                                
 disableNotifications                    : false                                                                                               
 cache_stats_datastore_enabled           : true                                                                                                
 errorExceptionClass                     : com.sap.xi.mapping.camel.XiMappingException                                                         
 errorType                               : TECHNICAL                                                                                           
 errorResponseMessage                    : Runtime exception during processing target field mapping  /CMO/data/details. The root message is: Un... (truncated)
 cache_miss_count                        : 1205                                                                                                
 integrationID                           : IF_IN0411_Apheresis_POST_BTPIS_CMO                                                                  
 hasRemainingRetryAttempts               : false                                                                                               
 globalOutputID                          : VM_Framework_Global_Metadata                                                                        
 sendEmail                               : Yes                                                                                                 
 logLevel                                :                                                                                                     
 MFSiteID                                : CarT_Apheresis                                                                                      
 SAP_InspectModelStepId3492              : MessageFlow_14                                                                                      
 log_exceptionClass                      :                                                                                                     
 SAP_MPL_LogLevel_Internal               : INFO                                                                                                
 globalInputID                           : IP_FoundationFramework                                                                              
 SAP_MPL_LogLevel_External               : NONE                                                                                                
 CamelToEndpoint                         : sap-jms-util://convertToProperties                                                                  
 meta_attribute_CARTOrderID              : AU-EMUH_01_S-10262736-02                                                                            
 meta_attribute_error_type               : TECHNICAL                                                                                           
 SAP_MessageProcessingLog                : Processing exchange C139129D0705CF0-000000000000000F:;   StartTime           = Tue Apr 22 13:27:57.9... (truncated)
 SAP_AuthHeaderValue                     : com.sap.it.rt.scc.connectivity.principal.propagator.PrincipalToken@3b7bf1b3                         
 LOG_ATTACHMENTS_COUNTER                 : 2                                                                                                   
 SAP_InspectModelStepId2571              : MessageFlow_48468                                                                                   
 deliverycount                           : 1                                                                                                   
 meta_attribute_ManufacturingSiteID      : MFG-10006                                                                                           
 SAP_IntegrationFlowID                   : IF_Framework_Master_JMS_Listener                                                                    
 meta_attribute_Interface_Flow           : Subscriber                                                                                          
 meta_attribute_ErrorClassification      : Business                                                                                            
 SAP_SAPPASSPORT                         : com.sap.jdsr.passport.DSRPassport@30245034                                                          
 errorLocation                           : BTP CI MAIN IPR                                                                                     

———————
HEADERS
———————
 emailTrigger                            : yes                                                                                                 
 functionName                            : filterLogs                                                                                          
 HasError                                : true                                                                                                
 SAP_MessageProcessingLogID              : AGgHmV2189N1LRStZv7szZHLhEhz                                                                        
 sapFunctionName                         : null                                                                                                
 scriptBundleId                          : SCR_Framework_V2                                                                                    
 scriptFile                              : PARSE_METADATA.groovy                                                                               
 scriptFileType                          : groovy                                                                                              
"""
}