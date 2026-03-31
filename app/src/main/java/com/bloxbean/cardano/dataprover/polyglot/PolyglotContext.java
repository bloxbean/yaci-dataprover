package com.bloxbean.cardano.dataprover.polyglot;

import com.bloxbean.cardano.dataprover.polyglot.helpers.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Manages GraalVM polyglot context lifecycle for script execution.
 * Handles context creation, helper bindings, and resource management.
 */
public class PolyglotContext implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PolyglotContext.class);

    private final Context context;
    private final ScriptLanguage language;
    private final String providerName;
    private final Path providerDir;
    private Value module;

    public PolyglotContext(String providerName, Path providerDir, ScriptLanguage language,
                           SandboxConfig sandboxConfig) {
        this.providerName = providerName;
        this.providerDir = providerDir;
        this.language = language;

        // Build context with sandbox restrictions
        Context.Builder builder = Context.newBuilder(language.getGraalId())
                .allowHostAccess(HostAccess.newBuilder()
                        .allowPublicAccess(true)
                        .allowArrayAccess(true)
                        .allowListAccess(true)
                        .allowMapAccess(true)
                        .allowAllImplementations(true)
                        .build())
                .allowExperimentalOptions(true);

        // Apply sandbox restrictions
        if (!sandboxConfig.isAllowIO()) {
            builder.allowIO(false);
        }

        this.context = builder.build();

        // Bind helper objects
        bindHelpers();
    }

    private void bindHelpers() {
        Value bindings = context.getBindings(language.getGraalId());

        // Logger
        bindings.putMember("log", new ScriptLogger(providerName));

        // Hex utilities
        bindings.putMember("hex", new HexHelper());

        // CBOR encoding
        bindings.putMember("cbor", new CborHelper());

        // Cardano utilities
        bindings.putMember("cardano", new CardanoHelper());

        // Database helper
        bindings.putMember("db", new DatabaseHelper());

        // HTTP client
        bindings.putMember("http", new HttpHelper());

        // File access (restricted to provider directory)
        bindings.putMember("file", new FileHelper(providerDir));

        // Plutus data helper (uses CCL for consistent encoding)
        bindings.putMember("plutus", new PlutusDataHelper());

        log.debug("Bound helper objects to polyglot context for provider: {}", providerName);
    }

    public void loadScript(Path scriptPath) throws IOException {
        String code = Files.readString(scriptPath);

        // For JavaScript, wrap in a function to capture exports
        if (language == ScriptLanguage.JAVASCRIPT) {
            // Wrap the script to expose module.exports
            String wrappedCode = """
                (function() {
                    var module = { exports: {} };
                    var exports = module.exports;
                    %s
                    return module.exports;
                })()
                """.formatted(code);

            Source source = Source.newBuilder(language.getGraalId(), wrappedCode, scriptPath.getFileName().toString())
                    .build();

            module = context.eval(source);
        } else {
            // For Python, evaluate directly and get globals
            Source source = Source.newBuilder(language.getGraalId(), code, scriptPath.getFileName().toString())
                    .build();
            context.eval(source);
            module = context.getBindings(language.getGraalId());
        }

        log.info("Loaded script for provider {}: {}", providerName, scriptPath.getFileName());
    }

    public void reloadScript(Path scriptPath) throws IOException {
        loadScript(scriptPath);
        log.info("Reloaded script for provider: {}", providerName);
    }

    public String getName() {
        Value nameValue = module.getMember("name");
        return nameValue != null && !nameValue.isNull() ? nameValue.asString() : providerName;
    }

    public String getDescription() {
        Value descValue = module.getMember("description");
        return descValue != null && !descValue.isNull() ? descValue.asString() : "";
    }

    public boolean hasFunction(String name) {
        Value fn = module.getMember(name);
        return fn != null && !fn.isNull() && fn.canExecute();
    }

    public void callInitialize(Map<String, Object> config) {
        if (hasFunction("initialize")) {
            Value fn = module.getMember("initialize");
            fn.execute(config);
        }
    }

    public Value callFetchData(Map<String, Object> config) {
        Value fn = module.getMember("fetchData");
        if (fn == null || fn.isNull() || !fn.canExecute()) {
            throw new RuntimeException("fetchData function not found in provider: " + providerName);
        }
        return fn.execute(config);
    }

    public byte[] callSerializeKey(Value data) {
        Value fn = module.getMember("serializeKey");
        if (fn == null || fn.isNull() || !fn.canExecute()) {
            throw new RuntimeException("serializeKey function not found in provider: " + providerName);
        }
        Value result = fn.execute(data);
        log.debug("serializeKey result type: isNull={}, hasArrayElements={}, isHostObject={}",
                result.isNull(), result.hasArrayElements(), result.isHostObject());
        return toByteArray(result);
    }

    public byte[] callSerializeValue(Value data) {
        Value fn = module.getMember("serializeValue");
        if (fn == null || fn.isNull() || !fn.canExecute()) {
            throw new RuntimeException("serializeValue function not found in provider: " + providerName);
        }
        Value result = fn.execute(data);
        log.debug("serializeValue result type: isNull={}, hasArrayElements={}, isHostObject={}",
                result.isNull(), result.hasArrayElements(), result.isHostObject());
        return toByteArray(result);
    }

    public Value callValidate(Value data) {
        Value fn = module.getMember("validate");
        if (fn == null || fn.isNull() || !fn.canExecute()) {
            // Return a default valid result if validate is not implemented
            return context.eval(language.getGraalId(), "({ valid: true, errors: [] })");
        }
        return fn.execute(data);
    }

    public Value callGetConfigSchema() {
        if (hasFunction("getConfigSchema")) {
            Value fn = module.getMember("getConfigSchema");
            return fn.execute();
        }
        return null;
    }

    public Value callGetConnectionConfigSchema() {
        if (hasFunction("getConnectionConfigSchema")) {
            Value fn = module.getMember("getConnectionConfigSchema");
            return fn.execute();
        }
        return null;
    }

    public Value callTestConfiguration(Map<String, Object> config) {
        if (hasFunction("testConfiguration")) {
            Value fn = module.getMember("testConfiguration");
            return fn.execute(config);
        }
        // Return default success if not implemented
        return context.eval(language.getGraalId(), "({ success: true, message: 'OK' })");
    }

    public void callReconfigure(Map<String, Object> config) {
        if (hasFunction("reconfigure")) {
            Value fn = module.getMember("reconfigure");
            fn.execute(config);
        } else if (hasFunction("initialize")) {
            // Fall back to initialize if reconfigure not available
            callInitialize(config);
        }
    }

    public byte[] callSerializeKeyFromInput(String keyInput) {
        if (hasFunction("serializeKeyFromInput")) {
            Value fn = module.getMember("serializeKeyFromInput");
            Value result = fn.execute(keyInput);
            return toByteArray(result);
        }
        throw new UnsupportedOperationException(
                "serializeKeyFromInput not implemented by provider: " + providerName);
    }

    private byte[] toByteArray(Value value) {
        if (value.isNull()) {
            log.warn("toByteArray: value is null, returning empty byte array");
            return new byte[0];
        }

        // Handle byte[] directly (returned from Java helpers like CardanoHelper, CborHelper)
        if (value.isHostObject()) {
            Object hostObject = value.asHostObject();
            log.debug("toByteArray: isHostObject=true, hostObject type={}",
                    hostObject != null ? hostObject.getClass().getName() : "null");
            if (hostObject instanceof byte[] b) {
                return b;
            }
        }

        // Handle Uint8Array or typed array from JavaScript
        if (value.hasArrayElements()) {
            int length = (int) value.getArraySize();
            log.debug("toByteArray: hasArrayElements=true, length={}", length);
            byte[] bytes = new byte[length];
            for (int i = 0; i < length; i++) {
                bytes[i] = (byte) value.getArrayElement(i).asInt();
            }
            return bytes;
        }

        log.error("toByteArray: Cannot convert value to byte array. " +
                "isNull={}, hasArrayElements={}, isHostObject={}, value={}",
                value.isNull(), value.hasArrayElements(), value.isHostObject(), value);
        throw new RuntimeException("Cannot convert value to byte array: " + value);
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
            log.debug("Closed polyglot context for provider: {}", providerName);
        }
    }

    public Path getProviderDir() {
        return providerDir;
    }

    public String getProviderName() {
        return providerName;
    }
}
