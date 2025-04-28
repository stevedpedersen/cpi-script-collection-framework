package com.sap.esb.messaging.access;

import java.util.Map;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ChunkMessageHelper {
   int getEncryptionKeyId();

   Map<String, Object> getMessageProperties();
}
