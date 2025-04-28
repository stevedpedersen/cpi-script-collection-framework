package com.sap.esb.messaging.access;

import javax.jms.JMSException;
import javax.jms.Message;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface PropertiesChange {
   Message change(Message var1) throws JMSException;
}
