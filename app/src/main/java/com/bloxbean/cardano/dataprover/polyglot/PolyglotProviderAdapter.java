package com.bloxbean.cardano.dataprover.polyglot;

import com.bloxbean.cardano.dataprover.exception.DataProviderException;
import com.bloxbean.cardano.dataprover.exception.SerializationException;
import com.bloxbean.cardano.dataprover.service.provider.*;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Adapter that wraps a polyglot script as a DataProvider.
 * Bridges Java provider interface to script functions.
 */
public class PolyglotProviderAdapter implements DataProvider<Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(PolyglotProviderAdapter.class);

    private final ProviderManifest manifest;
    private final Path providerDir;
    private final Path scriptPath;
    private PolyglotContext polyglotContext;
    private volatile boolean initialized = false;

    public PolyglotProviderAdapter(ProviderManifest manifest, Path providerDir) {
        this.manifest = manifest;
        this.providerDir = providerDir;
        this.scriptPath = providerDir.resolve(manifest.getScriptLanguage().getDefaultFile());
    }

    @Override
    public String getName() {
        return manifest.getName();
    }

    @Override
    public String getDescription() {
        return manifest.getDescription();
    }

    @Override
    public void initialize(Map<String, Object> config) {
        try {
            if (polyglotContext != null) {
                polyglotContext.close();
            }

            polyglotContext = new PolyglotContext(
                    manifest.getName(),
                    providerDir,
                    manifest.getScriptLanguage(),
                    manifest.getSandboxConfig()
            );

            polyglotContext.loadScript(scriptPath);
            polyglotContext.callInitialize(config);

            initialized = true;
            log.info("Initialized polyglot provider: {}", manifest.getName());
        } catch (IOException e) {
            throw new DataProviderException(manifest.getName(), "Failed to load script for provider: " + manifest.getName(), e);
        } catch (Exception e) {
            throw new DataProviderException(manifest.getName(), "Failed to initialize provider: " + manifest.getName(), e);
        }
    }

    @Override
    public List<Map<String, Object>> fetchData(Map<String, Object> config) throws DataProviderException {
        ensureInitialized();
        try {
            Value result = polyglotContext.callFetchData(config);
            return convertToList(result);
        } catch (Exception e) {
            throw new DataProviderException(manifest.getName(), "fetchData failed for provider: " + manifest.getName(), e);
        }
    }

    @Override
    public byte[] serializeKey(Map<String, Object> data) throws SerializationException {
        ensureInitialized();
        try {
            log.debug("serializeKey called with data: {}", data);
            Value dataValue = toPolyglotValue(data);
            byte[] result = polyglotContext.callSerializeKey(dataValue);
            log.debug("serializeKey returned {} bytes", result != null ? result.length : "null");
            return result;
        } catch (Exception e) {
            log.error("serializeKey failed for data: {}", data, e);
            throw new SerializationException("serializeKey failed for provider: " + manifest.getName(), e);
        }
    }

    @Override
    public byte[] serializeValue(Map<String, Object> data) throws SerializationException {
        ensureInitialized();
        try {
            log.debug("serializeValue called with data: {}", data);
            Value dataValue = toPolyglotValue(data);
            byte[] result = polyglotContext.callSerializeValue(dataValue);
            log.debug("serializeValue returned {} bytes", result != null ? result.length : "null");
            return result;
        } catch (Exception e) {
            log.error("serializeValue failed for data: {}", data, e);
            throw new SerializationException("serializeValue failed for provider: " + manifest.getName(), e);
        }
    }

    @Override
    public ValidationResult validate(Map<String, Object> data) {
        ensureInitialized();
        try {
            Value dataValue = toPolyglotValue(data);
            Value result = polyglotContext.callValidate(dataValue);

            boolean valid = result.getMember("valid").asBoolean();
            List<String> errors = new ArrayList<>();

            Value errorsValue = result.getMember("errors");
            if (errorsValue != null && errorsValue.hasArrayElements()) {
                for (int i = 0; i < errorsValue.getArraySize(); i++) {
                    errors.add(errorsValue.getArrayElement(i).asString());
                }
            }

            return valid ? ValidationResult.success() : ValidationResult.failure(errors);
        } catch (Exception e) {
            log.warn("Validation failed for provider {}: {}", manifest.getName(), e.getMessage());
            return ValidationResult.failure(List.of("Validation error: " + e.getMessage()));
        }
    }

    @Override
    public Class<Map<String, Object>> getDataType() {
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return mapClass;
    }

    @Override
    public ProviderMetadata getMetadata() {
        ConfigSchema configSchema = getConfigSchemaInternal();
        ConnectionConfigSchema connectionConfigSchema = getConnectionConfigSchema();

        // Determine status based on initialization and connection config requirements
        ProviderStatus status;
        String statusMessage = null;

        if (initialized) {
            status = ProviderStatus.AVAILABLE;
        } else if (connectionConfigSchema != null && !connectionConfigSchema.getFields().isEmpty()) {
            status = ProviderStatus.NOT_CONFIGURED;
            statusMessage = "Connection configuration required. Configure the provider settings.";
        } else {
            status = ProviderStatus.NOT_CONFIGURED;
        }

        return ProviderMetadata.builder()
                .name(getName())
                .description(getDescription())
                .dataType("Map<String, Object>")
                .status(status)
                .statusMessage(statusMessage)
                .configSchema(configSchema)
                .connectionConfigSchema(connectionConfigSchema)
                .build();
    }

    private ConfigSchema getConfigSchemaInternal() {
        // Try to get config schema even when not initialized
        if (!initialized || polyglotContext == null) {
            PolyglotContext tempContext = null;
            try {
                tempContext = new PolyglotContext(
                        manifest.getName(),
                        providerDir,
                        manifest.getScriptLanguage(),
                        manifest.getSandboxConfig()
                );
                tempContext.loadScript(scriptPath);
                Value schemaValue = tempContext.callGetConfigSchema();
                ConfigSchema schema = ConfigSchema.builder().fields(Collections.emptyList()).build();
                if (schemaValue != null && !schemaValue.isNull()) {
                    schema = convertConfigSchema(schemaValue);
                }
                return schema;
            } catch (Exception e) {
                log.warn("Failed to get config schema for {}: {}", manifest.getName(), e.getMessage());
                return ConfigSchema.builder().fields(Collections.emptyList()).build();
            } finally {
                if (tempContext != null) {
                    tempContext.close();
                }
            }
        }

        try {
            Value schemaValue = polyglotContext.callGetConfigSchema();
            if (schemaValue != null && !schemaValue.isNull()) {
                return convertConfigSchema(schemaValue);
            }
        } catch (Exception e) {
            log.debug("Failed to get config schema from provider {}: {}", manifest.getName(), e.getMessage());
        }

        return ConfigSchema.builder().fields(Collections.emptyList()).build();
    }

    @Override
    public byte[] serializeKeyFromInput(String keyInput) throws SerializationException {
        ensureInitialized();
        try {
            return polyglotContext.callSerializeKeyFromInput(keyInput);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException("serializeKeyFromInput failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ConnectionConfigSchema getConnectionConfigSchema() {
        if (!initialized || polyglotContext == null) {
            // Try to get schema without full initialization
            PolyglotContext tempContext = null;
            try {
                tempContext = new PolyglotContext(
                        manifest.getName(),
                        providerDir,
                        manifest.getScriptLanguage(),
                        manifest.getSandboxConfig()
                );
                tempContext.loadScript(scriptPath);
                Value schemaValue = tempContext.callGetConnectionConfigSchema();
                return convertConnectionConfigSchema(schemaValue);
            } catch (Exception e) {
                log.warn("Failed to get connection config schema for {}: {}", manifest.getName(), e.getMessage(), e);
                return null;
            } finally {
                if (tempContext != null) {
                    tempContext.close();
                }
            }
        }

        try {
            Value schemaValue = polyglotContext.callGetConnectionConfigSchema();
            return convertConnectionConfigSchema(schemaValue);
        } catch (Exception e) {
            log.warn("Failed to get connection config schema for {}: {}", manifest.getName(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean reconfigure(Map<String, Object> config) {
        try {
            if (polyglotContext != null) {
                polyglotContext.callReconfigure(config);
                initialized = true;  // Mark as initialized after successful reconfigure
            } else {
                initialize(config);
            }
            return true;
        } catch (Exception e) {
            log.error("Reconfigure failed for provider {}: {}", manifest.getName(), e.getMessage());
            return false;
        }
    }

    @Override
    public ConfigTestResult testConfiguration(Map<String, Object> config) {
        PolyglotContext tempContext = null;
        try {
            // Create a temporary context for testing
            tempContext = new PolyglotContext(
                    manifest.getName(),
                    providerDir,
                    manifest.getScriptLanguage(),
                    manifest.getSandboxConfig()
            );
            tempContext.loadScript(scriptPath);

            Value result = tempContext.callTestConfiguration(config);

            // Extract values BEFORE closing the context (GraalVM Values become invalid after context close)
            boolean success = result.getMember("success").asBoolean();
            String message = result.getMember("message").asString();

            return success ? ConfigTestResult.success(message) : ConfigTestResult.failure(message);
        } catch (Exception e) {
            return ConfigTestResult.failure("Test failed: " + e.getMessage());
        } finally {
            if (tempContext != null) {
                tempContext.close();
            }
        }
    }

    public void reload() throws IOException {
        if (polyglotContext != null) {
            polyglotContext.reloadScript(scriptPath);
            log.info("Reloaded script for provider: {}", manifest.getName());
        }
    }

    public Path getProviderDir() {
        return providerDir;
    }

    public ProviderManifest getManifest() {
        return manifest;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Provider not initialized: " + manifest.getName());
        }
    }

    private Value toPolyglotValue(Map<String, Object> data) {
        // The data is passed as a Java Map which GraalVM auto-converts
        return Value.asValue(data);
    }

    private List<Map<String, Object>> convertToList(Value arrayValue) {
        List<Map<String, Object>> list = new ArrayList<>();

        if (arrayValue.hasArrayElements()) {
            for (int i = 0; i < arrayValue.getArraySize(); i++) {
                Value item = arrayValue.getArrayElement(i);
                list.add(convertToMap(item));
            }
        }

        return list;
    }

    private Map<String, Object> convertToMap(Value value) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (value.hasMembers()) {
            for (String key : value.getMemberKeys()) {
                Value memberValue = value.getMember(key);
                map.put(key, convertValue(memberValue));
            }
        }

        return map;
    }

    private Object convertValue(Value value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < value.getArraySize(); i++) {
                list.add(convertValue(value.getArrayElement(i)));
            }
            return list;
        }
        if (value.hasMembers()) {
            return convertToMap(value);
        }
        return value.toString();
    }

    private ConfigSchema convertConfigSchema(Value schemaValue) {
        ConfigSchema.Builder builder = ConfigSchema.builder();

        Value fieldsValue = schemaValue.getMember("fields");
        if (fieldsValue != null && fieldsValue.hasArrayElements()) {
            List<ConfigField> fields = new ArrayList<>();
            for (int i = 0; i < fieldsValue.getArraySize(); i++) {
                fields.add(convertConfigField(fieldsValue.getArrayElement(i)));
            }
            builder.fields(fields);
        }

        return builder.build();
    }

    private ConnectionConfigSchema convertConnectionConfigSchema(Value schemaValue) {
        if (schemaValue == null || schemaValue.isNull()) {
            return null;
        }

        ConnectionConfigSchema.Builder builder = ConnectionConfigSchema.builder();

        Value fieldsValue = schemaValue.getMember("fields");
        if (fieldsValue != null && fieldsValue.hasArrayElements()) {
            List<ConfigField> fields = new ArrayList<>();
            for (int i = 0; i < fieldsValue.getArraySize(); i++) {
                fields.add(convertConfigField(fieldsValue.getArrayElement(i)));
            }
            builder.fields(fields);
        }

        return builder.build();
    }

    private ConfigField convertConfigField(Value fieldValue) {
        ConfigField.Builder builder = ConfigField.builder();

        if (fieldValue.hasMember("name")) {
            builder.name(fieldValue.getMember("name").asString());
        }
        if (fieldValue.hasMember("label")) {
            builder.label(fieldValue.getMember("label").asString());
        }
        if (fieldValue.hasMember("type")) {
            String typeStr = fieldValue.getMember("type").asString();
            builder.type(FieldType.valueOf(typeStr.toUpperCase()));
        }
        if (fieldValue.hasMember("required")) {
            builder.required(fieldValue.getMember("required").asBoolean());
        }
        if (fieldValue.hasMember("description")) {
            builder.description(fieldValue.getMember("description").asString());
        }
        if (fieldValue.hasMember("placeholder")) {
            builder.placeholder(fieldValue.getMember("placeholder").asString());
        }
        if (fieldValue.hasMember("defaultValue")) {
            builder.defaultValue(convertValue(fieldValue.getMember("defaultValue")));
        }
        if (fieldValue.hasMember("options")) {
            Value optionsValue = fieldValue.getMember("options");
            if (optionsValue.hasArrayElements()) {
                List<SelectOption> options = new ArrayList<>();
                for (int i = 0; i < optionsValue.getArraySize(); i++) {
                    Value opt = optionsValue.getArrayElement(i);
                    options.add(new SelectOption(
                            opt.getMember("value").asString(),
                            opt.getMember("label").asString()
                    ));
                }
                builder.options(options);
            }
        }
        if (fieldValue.hasMember("validation")) {
            Value validationValue = fieldValue.getMember("validation");
            FieldValidation.Builder validationBuilder = FieldValidation.builder();
            if (validationValue.hasMember("min")) {
                validationBuilder.min(validationValue.getMember("min").asInt());
            }
            if (validationValue.hasMember("max")) {
                validationBuilder.max(validationValue.getMember("max").asInt());
            }
            if (validationValue.hasMember("pattern")) {
                validationBuilder.pattern(validationValue.getMember("pattern").asString());
            }
            builder.validation(validationBuilder.build());
        }

        return builder.build();
    }

    public void close() {
        if (polyglotContext != null) {
            polyglotContext.close();
            polyglotContext = null;
            initialized = false;
        }
    }
}
