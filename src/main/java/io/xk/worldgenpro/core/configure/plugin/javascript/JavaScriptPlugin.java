package io.xk.worldgenpro.core.configure.plugin.javascript;


import io.xk.worldgenpro.core.configure.plugin.PluginContext;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class JavaScriptPlugin {
    public final String identifier;
    public final Value runFunc;

    public JavaScriptPlugin(String identifier, Value runFunc) {
        this.identifier = identifier;
        this.runFunc = runFunc;
    }

    public void run(PluginContext pluginContext) {
        try {
            runFunc.execute(pluginContext);
        } catch (PolyglotException e) {
            throw new RuntimeException("Failed to execute <JavaScript> plugin: " + identifier, e);
        }
    }
}
