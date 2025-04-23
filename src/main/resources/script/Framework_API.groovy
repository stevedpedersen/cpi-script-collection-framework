package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.asdk.datastore.*
import com.sap.it.api.asdk.runtime.*
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.UserCredential
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.exception.SecureStoreException
import java.security.cert.Certificate
import java.security.KeyPair

/**
 *
 */
class Framework_API {
    
    /**
     * Stores a value in the DataStore.
     * @param storeName DataStore name
     * @param entryId   Entry key
     * @param value     Value to store
     * @param overwrite Overwrite if exists (default: true)
     * @return true if successful, false or error map otherwise
     */
    static boolean datastorePut(String storeName, String entryId, String value, boolean overwrite = true) {
        try {
            def service = new Factory(DataStoreService.class).getService()
            if (service != null) {
                def dBean = new DataBean()
                dBean.setDataAsArray(value.getBytes("UTF-8"))
                def dConfig = new DataConfig()
                dConfig.setStoreName(storeName)
                dConfig.setId(entryId)
                dConfig.setOverwrite(overwrite)
                service.put(dBean, dConfig)
                return true
            }
        } catch (Exception e) {
            return [error: "Error storing DataStore entry $entryId in $storeName - ${e.message}"]
        }
        return false
    }

    /**
     * Retrieves a value from the DataStore as a String, or error map if not found/error.
     * @param storeName DataStore name
     * @param entryId   Entry key
     * @return String value or error map/null
     */
    static Object datastoreGet(String storeName, String entryId) {
        try {
            def service = new Factory(DataStoreService.class).getService()
            if (service != null) {
                def dsEntry = service.get(storeName, entryId)
                if (dsEntry != null) {
                    return new String(dsEntry.getDataAsArray(), "UTF-8")
                }
            }
        } catch (Exception e) {
            return [error: "Error retrieving DataStore entry $entryId from $storeName - ${e.message}"]
        }
        return null
    }

    /**
     * Retrieves a secure parameter (UserCredential) as a map or error map.
     * @param alias Secure parameter alias
     * @param throwError Throw SecureStoreException if true
     * @return Map with username, password, properties, or error
     */
    static Object securestoreGet(String alias, boolean throwError = false) {
        try {
            SecureStoreService sss = ITApiFactory.getService(SecureStoreService.class, null)
            UserCredential uc = sss.getUserCredential(alias)
            return [
                username: uc.username ?: 'N/A',
                properties: uc.credentialProperties ?: [:],
                password: "${uc.password?.toString() ?: ''}" ?: 'N/A'
            ]
        } catch (SecureStoreException e) {
            if (throwError) throw e
            return [error: "Error retrieving UserCredential for: $alias - ${e.message}"]
        }
    }

    /**
     * Retrieves the value of a secure parameter (password or username fallback).
     * @param alias Secure parameter alias
     * @param throwError Throw SecureStoreException if true
     * @return String value of the parameter or error map
     */
    static String securestoreGetParam(String alias, boolean throwError = false) {
        try {
            def creds = Framework_API.securestoreGet(alias, true)
            return creds.password && creds.password != 'N/A' ? creds.password : creds.username
        } catch (SecureStoreException e) {
            if (throwError) throw e
            return [error: "Error retrieving SecureParameter for: $alias - ${e.message}"]
        }
    }

    /**
     * Helper to check if a secure parameter is enabled (true/false, case-insensitive).
     * @param alias Secure parameter alias
     * @param message Optional message for property override
     * @return true if enabled, false otherwise
     */
    static boolean isSecureParamEnabled(String alias, Message message = null) {
        def val = message && message.getProperty(alias) != null ? message.getProperty(alias) : Framework_API.securestoreGetParam(alias)
        return val && val.toString().toLowerCase() == "true"
    }

    /**
     * Retrieves a certificate from the KeystoreService by alias.
     * @param alias The alias of the certificate entry.
     * @return The certificate as a java.security.cert.Certificate, or null if not found.
     */
    def keystoreGetCertificate(String alias) {
        def keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
        if (!keystoreService) {
            throw new IllegalStateException("KeystoreService not available")
        }
        return keystoreService.getCertificate(alias)
    }

    /**
     * Retrieves a certificate chain from the KeystoreService by alias.
     * @param alias The alias of the certificate entry.
     * @return The certificate chain as an array, or null if not found.
     */
    def keystoreGetCertificateChain(String alias) {
        def keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
        if (!keystoreService) {
            throw new IllegalStateException("KeystoreService not available")
        }
        return keystoreService.getCertificateChain(alias)
    }

    /**
     * Retrieves a key pair from the KeystoreService by alias.
     * @param alias The alias of the key pair entry.
     * @return The KeyPair object, or null if not found.
     */
    def keystoreGetKeyPair(String alias) {
        def keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
        if (!keystoreService) {
            throw new IllegalStateException("KeystoreService not available")
        }
        return keystoreService.getKeyPair(alias)
    }

    /**
     * Retrieves a certificate from the KeystoreService by alias, returns details as a map.
     * @param alias The alias of the certificate entry.
     * @return Map with certificate details or error information.
     */
    def keystoreGetCertificateFormatted(String alias) {
        try {
            def keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
            if (!keystoreService) {
                return [error: 'KeystoreService not available']
            }
            def cert = keystoreService.getCertificate(alias)
            if (!cert) return [error: "Certificate not found for alias: ${alias}"]
            return [
                alias: alias,
                type: cert.getType(),
                publicKeyAlgorithm: cert.getPublicKey()?.getAlgorithm(),
                publicKeyFormat: cert.getPublicKey()?.getFormat(),
                encoded: cert.getEncoded().encodeBase64().toString()
            ]
        } catch (Exception e) {
            return [error: "Error retrieving certificate for alias ${alias}: ${e.message}"]
        }
    }

    /**
     * Retrieves a certificate chain from the KeystoreService by alias, returns details as a list of maps.
     * @param alias The alias of the certificate entry.
     * @return List of maps with certificate details or error information.
     */
    def keystoreGetCertificateChainFormatted(String alias) {
        try {
            def keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
            if (!keystoreService) {
                return [error: 'KeystoreService not available']
            }
            def chain = keystoreService.getCertificateChain(alias)
            if (!chain) return [error: "Certificate chain not found for alias: ${alias}"]
            return chain.collect { cert ->
                [
                    type: cert.getType(),
                    publicKeyAlgorithm: cert.getPublicKey()?.getAlgorithm(),
                    publicKeyFormat: cert.getPublicKey()?.getFormat(),
                    encoded: cert.getEncoded().encodeBase64().toString()
                ]
            }
        } catch (Exception e) {
            return [error: "Error retrieving certificate chain for alias ${alias}: ${e.message}"]
        }
    }

    /**
     * Retrieves a key pair from the KeystoreService by alias, returns details as a map.
     * @param alias The alias of the key pair entry.
     * @return Map with key pair details or error information.
     */
    def keystoreGetKeyPairFormatted(String alias) {
        try {
            def keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
            if (!keystoreService) {
                return [error: 'KeystoreService not available']
            }
            def keyPair = keystoreService.getKeyPair(alias)
            if (!keyPair) return [error: "KeyPair not found for alias: ${alias}"]
            return [
                alias: alias,
                publicKeyAlgorithm: keyPair.getPublic()?.getAlgorithm(),
                publicKeyFormat: keyPair.getPublic()?.getFormat(),
                publicKeyEncoded: keyPair.getPublic()?.getEncoded()?.encodeBase64().toString(),
                privateKeyAlgorithm: keyPair.getPrivate()?.getAlgorithm(),
                privateKeyFormat: keyPair.getPrivate()?.getFormat(),
                privateKeyEncoded: keyPair.getPrivate()?.getEncoded()?.encodeBase64().toString()
            ]
        } catch (Exception e) {
            return [error: "Error retrieving key pair for alias ${alias}: ${e.message}"]
        }
    }
}