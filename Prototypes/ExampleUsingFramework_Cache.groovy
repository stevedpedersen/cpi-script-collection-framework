
public class GroovyUtil {
 
    private static GroovyShell groovyShell;
 
    static {
        groovyShell = new GroovyShell();
    }
    
    public static Object execute(String ruleScript, Map<String, Object> varMap) {
        String scriptMd5 = null;
        try {
            scriptMd5 = Md5Util.encryptForHex(ruleScript);
        } catch (Exception e) {
            // do something...
        }
        Script script;
        if (scriptMd5 == null) {
            script = groovyShell.parse(ruleScript);
        } else {
            String finalScriptMd5 = scriptMd5;
            script = Framework_Cache.getValue(
                Framework_Cache.GROOVY_SHELL_KEY_PREFIX + scriptMd5, 
                () -> Optional.ofNullable(groovyShell.parse(ruleScript, generateScriptName(finalScriptMd5))),
                new TypeReference<Script>() {}
            );
            if (script == null) {
                script = groovyShell.parse(ruleScript, generateScriptName(finalScriptMd5));
            }
        }
 
        // Lock the script here to prevent confusion of Binding data from concurrent execution of multiple threads
        synchronized(script) {
            Binding binding = new Binding(varMap);
            script.setBinding(binding);
            return script.run();
        }
    }
    
    private static String generateScriptName(String scriptName) {
        return "Script" + scriptName + ".groovy";
    }
}