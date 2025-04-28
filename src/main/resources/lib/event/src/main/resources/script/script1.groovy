import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.json.JsonOutput
import org.osgi.framework.*
import org.osgi.service.event.*

def Message processData(Message message) {

    //Read custom event handler id from message properties
    def eventHandlerId = message.getProperty("eventHandlerId")

    //Retrieve a list of EventHandler information
    def res = listOSGi()
    message.setProperty("eventHandlers", res);
    //Check if own EventHandler is registered (by counting matching eventhandlers)
    def numOwnEvents = res.findAll{
        it.props.any {
            prop -> prop.getKey() == "custom.id" && prop.getValue() == eventHandlerId
        }
    }.size()

    //Store custom event handler, so that we can use it in a route-block later
    message.setProperty("numOwnEvents", numOwnEvents)

    return message;
}

//This function returns a list of registered EventHandler objects
private listOSGi(){

    //Get an OSGi general bundle context
    def bundleCtx = FrameworkUtil.getBundle(Class.forName("com.sap.gateway.ip.core.customdev.util.Message")).getBundleContext()

    //Get a complete list of service references of type (OSGi) EventHandler
    ServiceReference[] srs = bundleCtx.getServiceReferences(EventHandler.class.getName(), null)

    //Create data object to store event handler information
    data = []

    //Loop through all results and read contents
    srs.each { ref ->
        //Create map of all properties of current eventhandler
        def props = [:]
        ref.getPropertyKeys().each { propKey ->
            props <<  ["$propKey":ref.getProperty(propKey)]
        }
        //Add eventhandler info (incl. properties map) to data array
        data << [
                name:ref.getBundle().getSymbolicName(),
                propCount:props.size(),
                props:props
        ]
    }
    return data
}