package src.main.resources.script

public class Framework_Cache {

    private static Cache<String, Optional<Object>> localMemoryCache =
            CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

    private static FLogger LOGGER = FLoggerFactory.getLogger(Framework_Cache.class);

    public static String GROOVY_SHELL_KEY_PREFIX = "GROOVY_SHELL#";

    public static <T> T getValue(String key, Callable<Optional<Object>> load, TypeReference<T> typeReference) {
        try {
            Optional<Object> value = localMemoryCache.get(key, load);
            if (value.isPresent()) {
                return (T) value.get();
            }
            return null;
        } catch (Exception ex) {
            LOGGER.error("Get cached value exception, key:{} ", key, ex);
        }
        return null;
    }
}
