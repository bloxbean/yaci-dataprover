package com.bloxbean.cardano.dataprover.polyglot;

/**
 * Supported scripting languages for polyglot providers.
 */
public enum ScriptLanguage {
    JAVASCRIPT("js", "provider.js"),
    PYTHON("python", "provider.py");

    private final String graalId;
    private final String defaultFile;

    ScriptLanguage(String graalId, String defaultFile) {
        this.graalId = graalId;
        this.defaultFile = defaultFile;
    }

    public String getGraalId() {
        return graalId;
    }

    public String getDefaultFile() {
        return defaultFile;
    }

    public static ScriptLanguage fromString(String language) {
        if (language == null) {
            return JAVASCRIPT;
        }
        return switch (language.toLowerCase()) {
            case "javascript", "js" -> JAVASCRIPT;
            case "python", "py" -> PYTHON;
            default -> JAVASCRIPT;
        };
    }
}
