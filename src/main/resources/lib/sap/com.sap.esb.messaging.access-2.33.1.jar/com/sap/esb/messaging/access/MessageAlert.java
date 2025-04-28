package com.sap.esb.messaging.access;

import java.util.Enumeration;
import javax.jms.Message;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MessageAlert {
   Enumeration<Message> getMessages();
}
