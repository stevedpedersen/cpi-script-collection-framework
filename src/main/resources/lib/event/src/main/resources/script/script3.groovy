import com.sap.gateway.ip.core.customdev.util.Message
import java.util.HashMap
import org.apache.camel.*
import org.osgi.framework.*
import org.osgi.service.event.*

def Message processData(Message message) {

    //Read the name of the event handler from properties
    def eventHandlerId = message.getProperty("eventHandlerId")
    //Call unregister function with custom eventhandler id
    def res = unregisterOSGiEvent(eventHandlerId)
    return message;
}

/****************************************************************
 * This function de-registers an event handler by its custom id *
 ****************************************************************/
private unregisterOSGiEvent(def eventHandlerId){
    //Get general bundle context
    def bundleCtx = FrameworkUtil.getBundle(Class.forName("com.sap.gateway.ip.core.customdev.util.Message")).getBundleContext()
    //Get all service references that match our eventhandler id
    ServiceReference[] srs = bundleCtx.getServiceReferences(EventHandler.class.getName(), "(custom.id=${eventHandlerId})")
    //For each service reference found...
    srs.each { sRef ->
        //...get registration and unregister it!
        sRef.getRegistration().unregister()
    }
    return [successful:true, count:srs?.size()]
}