import com.sap.gateway.ip.core.customdev.util.Message
import groovy.io.GroovyPrintStream
import org.apache.karaf.shell.api.console.Session
import org.apache.karaf.shell.api.console.SessionFactory
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil

Message processData(Message message) {

    String command = message.getBody(String.class)

    ByteArrayOutputStream out = new ByteArrayOutputStream()
    ByteArrayOutputStream err = new ByteArrayOutputStream()

    BundleContext context = FrameworkUtil.getBundle(Message.class).bundleContext
    SessionFactory sessionFactory = (SessionFactory) context.getService(context.getServiceReference(SessionFactory.class))
    Session session = sessionFactory.create(new ByteArrayInputStream(), new GroovyPrintStream(out, true), new GroovyPrintStream(err, true))
    session.execute(command)
    message.body = out.toString()
    session.close()

    return message

}