package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.mapping.ValueMappingApi
import com.sap.it.api.ITApiFactory
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants
import src.main.resources.script.Framework_API


/**
 * Framework_ValueMaps
 *
 * Value mapping, cache, and API utility class for SAP CPI Groovy scripts.
 */
class Framework_ValueMaps {
    Message message
    MessageLog messageLog
    Framework_Logger logger

    /**
     * This is to ensure that the required fields are set by developers. 
     */
    public static void validateMetadataIdentifiers(String projectName, String integrationID) {
        if (!projectName || !integrationID) {
            throw new FrameworkMetadataException(
                "Undefined framework identifier values - projectName: ${projectName} | integrationID: ${integrationID}."
            )
        }
    }

    /**
     * Retrieve a global value mapping (framework scope).
     * @param key          Value map key
     * @param defaultValue Default value if not found
     * @param message      Message object
     * @param messageLog   MessageLog object
     * @return Value from cache or value mapping
     */
    public static String frameworkVM(String key, String defaultValue = null, Message message, MessageLog messageLog) {
        try {
            return Framework_ValueMaps.getCachedOrFetch(key, Constants.ILCD.VM_GLOBAL_SRC_ID, Constants.ILCD.VM_GLOBAL_TRGT_ID, defaultValue, message, messageLog)
        } catch (Exception e) {
            return defaultValue
        }
    }

    /**
     * Retrieve an interface-specific value mapping.
     * @param key          Value map key
     * @param projectName  Source project name
     * @param integrationID Target integration ID
     * @param defaultValue Default value if not found
     * @param message      Message object
     * @param messageLog   MessageLog object
     * @return Value from cache or value mapping
     */
    public static String interfaceVM(String key, String projectName, String integrationID, String defaultValue = null, Message message, MessageLog messageLog) {
        try {
            return Framework_ValueMaps.getCachedOrFetch(key, projectName, integrationID, defaultValue, message, messageLog)
        } catch (Exception e) {
            return defaultValue
        }
    }

    /**
     * Retrieve a value mapping from SAP CPI ValueMapping API.
     * @param srcKey      Source value
     * @param srcId       Source agency/ID
     * @param targetId    Target agency/ID
     * @param message     Message object (optional)
     * @param messageLog  MessageLog object (optional)
     * @return Value mapping result or empty string
     */
    static String getValueMapping(String srcKey, String srcId, String targetId, Message message = null, MessageLog messageLog = null) {
        try {
            def api = ITApiFactory.getApi(ValueMappingApi.class, null)
            return api?.getMappedValue(Constants.ILCD.VM_SRC_AGENCY, srcId, srcKey, Constants.ILCD.VM_TRGT_AGENCY, targetId) ?: ""
        } catch (Exception e) {
            def fields = """\n\n
                Source Agency:      ${Constants.ILCD.VM_SRC_AGENCY}\n
                Source Identifier:  ${srcId}\n
                Source Value:       ${srcKey}\n
                Target Agency:      ${Constants.ILCD.VM_TRGT_AGENCY}\n
                Target Identifier:  ${targetId}\n
            """
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ValueMaps.getValueMapping", true, fields)
            throw e
        }
    }

    // ===============================================================================
    //                             VALUE MAPS CACHE SECTION
    // -------------------------------------------------------------------------------
    // This section implements a static in-memory cache for value mapping lookups in
    // SAP CPI. The cache is keyed by a composite of source/target/key, and supports
    // flexible configuration via message properties or secure parameters:
    //
    //   - cache_disabled: If true, disables static caching entirely (always fetches).
    //     Settable via message property or secure parameter (default: false).
    //
    //   - cache_ttl_seconds: Time-to-live for cache entries in seconds. Controls how
    //     long a value stays cached before being refreshed. Settable via message
    //     property or secure parameter (default: 300 seconds).
    //
    //   - cache_stats_datastore_enabled: If true, enables saving cache hit/miss
    //     statistics to DataStore for monitoring. Otherwise, stats are tracked only
    //     in message properties. Settable via message property or secure parameter.
    //
    // All options can be set as message properties at runtime or as secure parameters
    // for global configuration. Message properties take precedence if both are set.
    // ===============================================================================
    private static final Map<String, CacheEntry> cache = [:]
    private static final int DEFAULT_TTL_SECONDS = 300 // 5 minutes

    private static String getCacheKey(String key, String srcId, String targetId) {
        return "${srcId}:${targetId}:${key}"
    }

    /**
     * Retrieve a value from cache or fetch from value mapping, applying TTL and cache disable logic.
     * @param key         Value map key
     * @param srcId       Source ID
     * @param targetId    Target ID
     * @param defaultValue Default value if not found
     * @param message     Message object
     * @param messageLog  MessageLog object
     * @param ttlSeconds  TTL in seconds (optional)
     * @return Value from cache or value mapping
     */
    private static String getCachedOrFetch(String key, String srcId, String targetId, String defaultValue, Message message, MessageLog messageLog, int ttlSeconds = DEFAULT_TTL_SECONDS) {
        // Check disabled
        def staticCacheDisabled = message.getProperty(Constants.ILCD.Cache.SPARAM_IS_DISABLED) != null ? 
            message.getProperty(Constants.ILCD.Cache.SPARAM_IS_DISABLED) : Framework_API.securestoreGetParam(Constants.ILCD.Cache.SPARAM_IS_DISABLED)
        def disableCachingValueMapEntries = staticCacheDisabled && staticCacheDisabled.toString().toLowerCase() == "true"
        message.setProperty(Constants.ILCD.Cache.SPARAM_IS_DISABLED, "${disableCachingValueMapEntries}")
        if (disableCachingValueMapEntries) {
            return Framework_ValueMaps.getValueMapping(key, srcId, targetId, message, messageLog) ?: defaultValue
        }
        // Check TTL
        def now = System.currentTimeMillis()
        def ttl = message.getProperty(Constants.ILCD.Cache.SPARAM_TTL) != null ? message.getProperty(Constants.ILCD.Cache.SPARAM_TTL) : Framework_API.securestoreGetParam(Constants.ILCD.Cache.SPARAM_TTL)
        ttlSeconds = (ttl != null && ttl.toString().isInteger()) ? ttl.toInteger() : DEFAULT_TTL_SECONDS
        message.setProperty(Constants.ILCD.Cache.SPARAM_TTL, "${ttlSeconds}")

        def cacheKey = getCacheKey(key, srcId, targetId)
        def entry = cache[cacheKey]
        if (entry && entry.expiry > now) {
            updateCacheStats("hit", cacheKey, entry.expiry, message, messageLog)
            return entry.value
        }

        def value = Framework_ValueMaps.getValueMapping(key, srcId, targetId, message, messageLog) ?: defaultValue
        def expiry = now + (ttlSeconds * 1000)
        cache[cacheKey] = new CacheEntry(value, expiry)
        updateCacheStats("miss", cacheKey, expiry, message, messageLog)
        return value
    }

    /**
     * Update cache statistics for hits/misses, using DataStore or message properties.
     * @param type      'hit' or 'miss'
     * @param cacheKey  Unique cache key
     * @param expiry    Expiry timestamp
     * @param message   Message object
     * @param messageLog MessageLog object
     */
    private static void updateCacheStats(String type, String cacheKey, long expiry, Message message, MessageLog messageLog) {
        try {
            // Check datastore cache stats enabled
            def dsEnabled = message.getProperty(Constants.ILCD.Cache.SPARAM_DS_STATS) != null ? 
                message.getProperty(Constants.ILCD.Cache.SPARAM_DS_STATS) : Framework_API.securestoreGetParam(Constants.ILCD.Cache.SPARAM_DS_STATS)
            def enableSavingCacheStatsToDatastore = dsEnabled && dsEnabled.toString().toLowerCase() == "true"
            message.setProperty(Constants.ILCD.Cache.SPARAM_DS_STATS, "${enableSavingCacheStatsToDatastore}")

            def (srcId, tgtId, entryKey) = cacheKey.split(":").size() == 3 ? cacheKey.split(":", 3) : []
            def interfaceKey = "${srcId}:${tgtId}"
            def typeCount = 0

            if (enableSavingCacheStatsToDatastore) {
                def dsEntryId = new Date().format("yyyyMMddHH")
                def statsJson = Framework_API.datastoreGet(Constants.ILCD.Cache.DATASTORE, dsEntryId)
                def stats
                if (!statsJson) {
                    stats = [hits:0, misses:0, entries: [:]]
                } else if (statsJson instanceof Map && statsJson.error) {
                    stats = [hits:0, misses:0, entries: [:]]
                } else if (statsJson instanceof String) {
                    stats = new JsonSlurper().parseText(statsJson)
                } else {
                    stats = [hits:0, misses:0, entries: [:]]
                }
                if (!stats.entries[interfaceKey]) stats.entries[interfaceKey] = [:]
                if (type == "hit") { 
                    stats.hits++
                    typeCount = stats.hits
                } else {
                    stats.misses++
                    typeCount = stats.misses
                }
                stats.entries[interfaceKey][entryKey] = formatUtcTimestamp(expiry) ?: expiry
                Framework_API.datastorePut(Constants.ILCD.Cache.DATASTORE, dsEntryId, JsonOutput.toJson(stats), true)
                message.setProperty("cache_${type}_count", (type == "hit" ? "${stats.hits}" : "${stats.misses}"))
            } else {
                def prop = type == "hit" ? "cache_hit_count" : "cache_miss_count"
                def count = ((message.getProperty(prop) ?: 0) as int) + 1
                typeCount = count
                message.setProperty(prop, "${count}")
            }
            if (messageLog) {
                messageLog.setStringProperty("cache_${type}.${interfaceKey}", "count: ${typeCount}, ${type == 'hit' ? 'existing' : 'new'} exp: ${formatUtcTimestamp(expiry) ?: expiry}")
            }
        } catch (Exception e) {
            if (messageLog) {
                messageLog.addAttachmentAsString("UpdateCacheStats Exception", "${e.message}\n${e.stackTrace}", "text/plain")
            } else {
                throw e
            }
        }
    }

    private static String formatUtcTimestamp(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
        return sdf.format(new Date(timestampMillis))
    }

    static class CacheEntry {
        String value
        long expiry
        CacheEntry(String value, long expiry) {
            this.value = value
            this.expiry = expiry
        }
    }

    static class FrameworkMetadataException extends RuntimeException {
        FrameworkMetadataException(String message) {
            super(message)
        }
        Throwable getCause() {
            return new Throwable("FrameworkMetadataException: Invalid metadata configuration for projectName/integrationID.")
        }
    }
}