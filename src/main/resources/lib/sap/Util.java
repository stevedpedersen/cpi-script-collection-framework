package com.sap.esb.messaging.access.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;

public class Util {
   private static final String JMS_TYPE = "JMSType";
   private static final String DOT = ".";
   private static final String DOT_REPLACEMENT = "_DOT_";
   private static final String HYPHEN = "-";
   private static final String HYPHEN_REPLACEMENT = "_HYPHEN_";

   public static HashMap<String, Object> getMessageProperties(Message msg) throws JMSException {
      HashMap<String, Object> properties = new HashMap();
      if (msg != null) {
         Enumeration srcProperties = msg.getPropertyNames();

         while(srcProperties.hasMoreElements()) {
            String propertyName = (String)srcProperties.nextElement();
            properties.put(propertyName, msg.getObjectProperty(propertyName));
         }

         if (msg.getJMSType() != null) {
            properties.put("JMSType", msg.getJMSType());
         }
      }

      return properties;
   }

   public static void setMessageProperties(Message msg, HashMap<String, Object> proporties) throws JMSException {
      if (proporties != null) {
         Iterator var2 = proporties.entrySet().iterator();

         while(var2.hasNext()) {
            Entry<String, Object> entry = (Entry)var2.next();
            if ("JMSType".equals(entry.getKey())) {
               msg.setJMSType((String)entry.getValue());
            } else {
               String propertyName = (String)entry.getKey();
               Object value = entry.getValue();
               msg.setObjectProperty(propertyName, value);
            }
         }

      }
   }

   public static void copyBytesBody(BytesMessage fromMessage, BytesMessage toMessage) throws JMSException {
      byte[] body = new byte[(int)fromMessage.getBodyLength()];
      fromMessage.readBytes(body);
      toMessage.writeBytes(body);
   }

   public static String decodeKey(String key) {
      String answer = replaceAll(key, "_DOT_", ".");
      answer = replaceAll(answer, "_HYPHEN_", "-");
      return answer;
   }

   public static String replaceAll(String input, String from, String to) {
      if (isEmpty(input)) {
         return input;
      } else if (from == null) {
         throw new IllegalArgumentException("from cannot be null");
      } else if (to == null) {
         throw new IllegalArgumentException("to cannot be null");
      } else if (!input.contains(from)) {
         return input;
      } else {
         int len = from.length();
         int max = input.length();
         StringBuilder sb = new StringBuilder(max);
         int i = 0;

         while(true) {
            while(i < max) {
               if (i + len <= max) {
                  String token = input.substring(i, i + len);
                  if (from.equals(token)) {
                     sb.append(to);
                     i += len;
                     continue;
                  }
               }

               sb.append(input.charAt(i));
               ++i;
            }

            return sb.toString();
         }
      }
   }

   public static boolean isEmpty(Object value) {
      return !isNotEmpty(value);
   }

   public static boolean isNotEmpty(Object value) {
      if (value == null) {
         return false;
      } else if (value instanceof String) {
         String text = (String)value;
         return text.trim().length() > 0;
      } else {
         return true;
      }
   }
}
