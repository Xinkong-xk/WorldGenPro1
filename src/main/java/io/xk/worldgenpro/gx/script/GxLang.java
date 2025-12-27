package io.xk.worldgenpro.gx.script;

public enum GxLang {
    JAVA, LUA, JavaScript;

    public static GxLang fromString(String name) {
        switch (name.toLowerCase()) {
            case "java": return JAVA;
            case "lua": return LUA;
            case "js":
            case "javascript": return JavaScript;
            default: throw new IllegalArgumentException("Unknown language: " + name);
        }
    }
}
