import com.sap.it.api.asdk.datastore.*
import com.sap.it.api.asdk.runtime.*
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import org.osgi.framework.*
import org.osgi.service.event.*

def Message processData(Message message) {

    //Get message properties, which were set in ContentModifier (1)
    def eventHandlerId = message.getProperty("eventHandlerId")
    def eventListenType = message.getProperty("eventListenType")
    //Register our custom IFlow event
    def res = registerOSGiEvent(eventHandlerId, eventListenType)

    return message
}

/***********************************************
 * This function helps registering OSGi events *
 ***********************************************/
private registerOSGiEvent(def eventHandlerId, def eventListenType){
    //Get general bundle context
    def bundleCtx = FrameworkUtil.getBundle(Class.forName("com.sap.gateway.ip.core.customdev.util.Message")).getBundleContext()

    //Define the topics we like to listen to
    def topics = eventListenType
    //Configure our event listener
    def props = new Hashtable();
    props.put(EventConstants.EVENT_TOPIC, topics)
    props.put("custom.id", eventHandlerId)

    //Register custom EventHandler as service and pass properties, credentials
    bundleCtx.registerService(EventHandler.class.getName(), new DeployEventHandler(), props)
    return [successful:true]
}

/**************************************************************
 * This is the custom eventhandler class, we want to register *
 **************************************************************/
public class DeployEventHandler implements EventHandler {
    //This function will be called everytime, when an
    //event with a matching topic passes by. Everything which
    //should happen at an event, must be implemented here.
    public void handleEvent(Event event)
    {
        //The complete code is called as "async" Runnable in a different thread,
        //because OSGi has a default timeout of 5000ms for events. If an event-
        //handler takes more time, it will be blacklisted. By use of Runnable,
        //we can bypass this limit.
        Runnable runnable = {
            //Build event information
            def evntMsg = []
            try {
                evntMsg = [topic:event.getTopic(), bundleName:event.getProperty("bundle").getSymbolicName(), TimeStamp:event.getProperty("timestamp")]
                def bundle = event.getProperty("bundle").getSymbolicName();
                def pattern = ~/^Test_[a-fA-F0-9]{32}$/
                if (!(bundle ==~ pattern)) {
                    def service = new Factory(DataStoreService.class).getService()
                    //Check if valid service instance was retrieved
                    if (service != null) {
                        def dBean = new DataBean()
                        dBean.setDataAsArray(JsonOutput.toJson(evntMsg).getBytes("UTF-8"))
                        //Define datatore name and entry id
                        def dConfig = new DataConfig()
                        dConfig.setStoreName("osgiEvents")
                        dConfig.setId(bundle)
                        dConfig.doOverwrite()
                        //Write to data store
                        def result = service.put(dBean, dConfig)
                    }
                }
            }catch (Exception e){
                def pattern = ~/An entry with id \w+ does already exist in data store \w+/
                if (!(e instanceof com.sap.it.api.asdk.exception.DataStoreException &&
                        e.message ==~ pattern)){
                    evntMsg = e.toString();
                    def service = new Factory(DataStoreService.class).getService()
                    //Check if valid service instance was retrieved
                    if( service != null) {
                        def dBean = new DataBean()
                        dBean.setDataAsArray(evntMsg.getBytes("UTF-8"))
                        //Define datatore name and entry id
                        def dConfig = new DataConfig()
                        dConfig.setStoreName("osgiEventsError")
                        dConfig.setId("error")
                        dConfig.doOverwrite()
                        //Write to data store
                        def result = service.put(dBean,dConfig)
                    }
                }
            }
        }
        //Call the Runnable
        def thread = new Thread(runnable);
        thread.start();
    }
}