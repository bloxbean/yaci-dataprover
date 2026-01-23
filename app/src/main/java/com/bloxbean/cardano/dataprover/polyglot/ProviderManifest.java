package com.bloxbean.cardano.dataprover.polyglot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Model for manifest.json files in polyglot provider directories.
 * Contains metadata, language, schedule, and sandbox configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderManifest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String name;
    private String description;
    private String version = "1.0.0";
    private String author;
    private String language = "javascript";
    private String dataSource;
    private List<String> tags;
    private ScheduleConfig schedule;
    private Map<String, Object> sandbox;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ScheduleConfig getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduleConfig schedule) {
        this.schedule = schedule;
    }

    public Map<String, Object> getSandbox() {
        return sandbox;
    }

    public void setSandbox(Map<String, Object> sandbox) {
        this.sandbox = sandbox;
    }

    public ScriptLanguage getScriptLanguage() {
        return ScriptLanguage.fromString(language);
    }

    public SandboxConfig getSandboxConfig() {
        return SandboxConfig.fromMap(sandbox);
    }

    public static ProviderManifest load(Path manifestPath) throws IOException {
        String content = Files.readString(manifestPath);
        return MAPPER.readValue(content, ProviderManifest.class);
    }
}
