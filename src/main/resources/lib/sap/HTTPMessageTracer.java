package com.sap.esb.camel.http.ahc.configurer.impl;

import com.sap.it.api.msglog.adapter.AdapterMessageLog;
import com.sap.it.api.msglog.adapter.AdapterMessageLogFactory;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.apache.camel.Exchange;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPMessageTracer {
   public static final String SAP_DISABLE_ATTACHMENTS_HTTP = "SAP.DisableAttachments.HTTP";
   private static final String[] COOKIE_ATTRIBUTES = new String[]{"Domain", "Path", "Expires", "SameSite"};
   private static final Logger log = LoggerFactory.getLogger(HTTPMessageTracer.class);

   protected static AdapterMessageLog getMessageLogger(Exchange exchange, String text) {
      try {
         AdapterMessageLogFactory msgLogFactory = (AdapterMessageLogFactory)exchange.getContext().getRegistry().lookupByName(AdapterMessageLogFactory.class.getName());
         return msgLogFactory.getMessageLog(exchange, text, "Http_Receiver", exchange.getExchangeId());
      } catch (Exception var3) {
         log.error("There was an error in getting MessageLogFactory instance ", var3);
         log.debug("AdapterMessageLog is null from AdapterMessageLogFactory");
         return null;
      }
   }

   public static void createMPL(Exchange exchange, ByteArrayOutputStream outputStream) {
      try {
         AdapterMessageLog adapterMessageLog = getMessageLogger(exchange, "Logging error attachments");
         if (adapterMessageLog != null) {
            Object requestHeaders = exchange.getProperty("SAP_AhcRequestHeaders");
            Object responseHeaders = exchange.getProperty("SAP_AhcResponseHeaders");
            String reqValue = getHeadersAsString(requestHeaders);
            String resValue = getHeadersAsString(responseHeaders);
            loggingAttachmentContent(exchange, reqValue, resValue, outputStream);
            if (enableMPLAttachments(exchange)) {
               createMPLAttachments(adapterMessageLog, "HTTP_Receiver_Adapter_Request_Headers", reqValue);
               createMPLAttachments(adapterMessageLog, "HTTP_Receiver_Adapter_Response_Headers", resValue);
               createMPLAttachments(adapterMessageLog, "HTTP_Receiver_Adapter_Response_Body", outputStream);
            }
         } else {
            log.debug("Unable to create attachments with request headers, response headers and body content as the AdapterMessageLog instance is null");
         }
      } catch (Exception var7) {
         log.error("Exception occurred in logInMPL ", var7);
      }

   }

   private static boolean enableMPLAttachments(Exchange exchange) {
      return Boolean.getBoolean("SAP.DisableAttachments.HTTP") ? false : Boolean.parseBoolean((String)exchange.getIn().getHeader("SAPEnableMPLAttachments", "true"));
   }

   public static void createMPL(Exchange exchange) {
      try {
         Object requestHeaders = exchange.getProperty("SAP_AhcRequestHeaders");
         String val = getHeadersAsString(requestHeaders);
         loggingAttachmentContent(exchange, val, (String)null, (ByteArrayOutputStream)null);
         AdapterMessageLog adapterMessageLog = getMessageLogger(exchange, "Logging error attachments");
         if (adapterMessageLog != null) {
            if (Boolean.parseBoolean((String)exchange.getIn().getHeader("SAPEnableMPLAttachments", "true"))) {
               createMPLAttachments(adapterMessageLog, "HTTP_Receiver_Adapter_Request_Headers", val);
            }
         } else {
            log.debug("Unable to create attachments with request headers as the AdapterMessageLog instance is null");
         }
      } catch (Exception var4) {
         log.error("Exception occurred in logInMPL ", var4);
      }

   }

   public static void loggingAttachmentContent(Exchange exchange, String reqValue, String resValue, ByteArrayOutputStream outputStream) {
      String mplId = (String)exchange.getIn().getHeader("SAP_MessageProcessingLogID");
      StringBuilder sb = new StringBuilder();
      sb.append(System.lineSeparator()).append("MessageProcessingId: ").append(mplId);
      sb.append(System.lineSeparator()).append("CorrelationId: ").append(exchange.getIn().getHeader("SAP_MplCorrelationId"));
      sb.append(System.lineSeparator()).append("Address: ").append(exchange.getIn().getHeader("SAP_RealDestinationUrl"));
      sb.append(System.lineSeparator()).append("HTTP Method: ").append(exchange.getIn().getHeader("CamelHttpMethod"));
      sb.append(System.lineSeparator()).append("Camel Endpoint: ").append(exchange.getIn().getHeader("CamelToEndpoint"));
      if (reqValue != null) {
         sb.append(System.lineSeparator()).append("################# Request Headers #################").append(System.lineSeparator()).append(reqValue);
      }

      if (resValue != null) {
         sb.append(System.lineSeparator()).append("################# Response Headers #################").append(System.lineSeparator()).append(resValue);
      }

      if (outputStream != null) {
         sb.append(System.lineSeparator()).append("################# Response Body #################").append(System.lineSeparator()).append(encodeToString(outputStream));
      }

      log.error(sb.toString());
   }

   protected static void createMPLAttachments(AdapterMessageLog adapterMessageLog, String fileName, String fileContent) {
      try {
         if (adapterMessageLog != null && fileContent != null) {
            byte[] bytes = fileContent.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 52428800) {
               fileContent = truncateFileContent(bytes);
            }

            adapterMessageLog.addAttachmentAsString(fileName, fileContent, "text/plain");
         } else {
            log.debug("No file content present to create attachment for {}", fileName);
         }
      } catch (Exception var4) {
         log.error("Exception occurred in createMPLAttachments ", var4);
      }

   }

   protected static void createMPLAttachments(AdapterMessageLog adapterMessageLog, String fileName, ByteArrayOutputStream outputStream) {
      try {
         if (adapterMessageLog != null && outputStream != null) {
            byte[] bytes = outputStream.toByteArray();
            String fileContent;
            if (bytes.length > 52428800) {
               fileContent = truncateFileContent(bytes);
            } else {
               fileContent = encodeToString(outputStream);
            }

            adapterMessageLog.addAttachmentAsString(fileName, fileContent, "text/plain");
         } else {
            log.debug("No file content present to create attachment for {}", fileName);
         }
      } catch (Exception var5) {
         log.error("Exception occurred in createMPLAttachments ", var5);
      }

   }

   protected static String getHeadersAsString(Object headers) {
      StringBuilder messageBuilder = new StringBuilder(HTTPMessageConstants.NEW_LINE);
      if (headers != null && headers instanceof Map) {
         try {
            Map<String, List<String>> headersMap = (Map)headers;

            String headerName;
            String headerValue;
            for(Iterator var3 = headersMap.entrySet().iterator(); var3.hasNext(); messageBuilder.append(HTTPMessageConstants.PADDING_LEFT).append(String.format("%-25s", headerName)).append(HTTPMessageConstants.SEPARATOR).append(headerValue).append(HTTPMessageConstants.NEW_LINE)) {
               Entry<String, List<String>> entry = (Entry)var3.next();
               List<String> valueList = (List)entry.getValue();
               headerName = (String)entry.getKey();
               headerValue = String.join(";", valueList);
               if ((!"x-csrf-token".equalsIgnoreCase(headerName) || "fetch".equalsIgnoreCase(headerValue)) && !headerName.equalsIgnoreCase("SAP-PASSPORT")) {
                  if (!headerName.equalsIgnoreCase("Set-Cookie") && !headerName.equalsIgnoreCase("Cookie")) {
                     if (headerName.equalsIgnoreCase("Authorization") || headerName.equalsIgnoreCase("Proxy-Authorization") || headerName.equalsIgnoreCase("SAP-Connectivity-Technical-Authentication")) {
                        headerValue = maskAuthorization(headerValue);
                     }
                  } else {
                     headerValue = hashCookies(headerValue);
                  }
               } else {
                  headerValue = "SHA256:".concat(DigestUtils.sha256Hex(headerValue));
               }
            }
         } catch (Exception var8) {
            log.error("Exception occurred in getHeadersAsString ", var8);
         }

         return messageBuilder.toString();
      } else {
         return null;
      }
   }

   private static String hashCookies(String cookieVal) {
      if (null != cookieVal && !"".equals(cookieVal)) {
         String hashResult = "";

         try {
            String[] rawCookieParams = cookieVal.split(";");
            List<String> hashedCookieHeaders = new ArrayList();
            String[] var4 = rawCookieParams;
            int var5 = rawCookieParams.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               String cookieValue = var4[var6];
               String[] cookiePair = cookieValue.split("=", 2);
               StringBuilder hashedCookiePair = new StringBuilder(cookiePair[0]);
               if (cookiePair.length > 1) {
                  Stream var10000 = Arrays.asList(COOKIE_ATTRIBUTES).stream();
                  String var10001 = cookiePair[0].trim();
                  var10001.getClass();
                  if (!var10000.anyMatch(var10001::equalsIgnoreCase)) {
                     hashedCookiePair.append(" = ").append("SHA256:").append(DigestUtils.sha256Hex(cookiePair[1]));
                  } else {
                     hashedCookiePair.append(" = ").append(cookiePair[1]);
                  }
               }

               hashedCookieHeaders.add(hashedCookiePair.toString());
            }

            hashResult = String.join(";", hashedCookieHeaders);
         } catch (Exception var10) {
            log.error("Exception occurred in hashCookies ", var10);
         }

         return hashResult;
      } else {
         return "";
      }
   }

   protected static final String maskAuthorization(String headerValue) {
      StringBuilder sb = new StringBuilder("");
      if (null != headerValue && !headerValue.isEmpty()) {
         int index = headerValue.indexOf(" ");
         if (index > -1) {
            sb.append(headerValue.substring(0, index)).append(" ").append("********");
         } else {
            sb.append("********");
         }
      }

      return sb.toString();
   }

   private static String encodeToString(ByteArrayOutputStream outputStream) {
      String fileContent = "";

      try {
         fileContent = outputStream.toString(StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException var3) {
         fileContent = var3.getMessage();
         log.error("Failed to convert to UTF-8 string ", var3);
      }

      return fileContent;
   }

   private static String truncateFileContent(byte[] bytes) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      outputStream.write(bytes, 0, Math.min(bytes.length, 52428800));
      return encodeToString(outputStream);
   }

   public static void createMPLAttachments(Exchange exchange, StringBuilder droppedHeaders) {
      try {
         AdapterMessageLog adapterMessageLog = getMessageLogger(exchange, "Logging error attachments");
         if (adapterMessageLog != null && enableMPLAttachments(exchange)) {
            createMPLAttachments(adapterMessageLog, "HTTP_Dropped_Request_Headers", droppedHeaders.toString());
         } else {
            log.debug("Unable to create attachments with dropped request headers as the AdapterMessageLog instance is null");
         }
      } catch (Exception var3) {
         log.error("Exception occurred while creating MPL attachment of dropped request headers", var3);
      }

   }

   public static void addRetryMessageToMPL(Exchange exchange, String logTitle, String message) {
      try {
         AdapterMessageLog adapterMessageLog = getMessageLogger(exchange, message);
         if (adapterMessageLog != null) {
            adapterMessageLog.setStringProperty(logTitle, message);
         } else {
            log.debug("Unable to add Retry message to MPL as the AdapterMessageLog instance is null");
         }
      } catch (Exception var4) {
         log.error("Exception occurred while creating MPL attachment of dropped request headers", var4);
      }

   }
}
