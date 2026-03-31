package com.bloxbean.cardano.dataprover.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the data prover.
 * Binds to 'dataprover' prefix in application.yml.
 */
@ConfigurationProperties(prefix = "dataprover")
public class DataProverProperties {

    private StorageProperties storage = new StorageProperties();
    private String defaultScheme = "mpf";
    private CacheProperties cache = new CacheProperties();
    private RetentionProperties retention = new RetentionProperties();
    private PluginsProperties plugins = new PluginsProperties();

    public StorageProperties getStorage() {
        return storage;
    }

    public void setStorage(StorageProperties storage) {
        this.storage = storage;
    }

    public String getDefaultScheme() {
        return defaultScheme;
    }

    public void setDefaultScheme(String defaultScheme) {
        this.defaultScheme = defaultScheme;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public RetentionProperties getRetention() {
        return retention;
    }

    public void setRetention(RetentionProperties retention) {
        this.retention = retention;
    }

    public PluginsProperties getPlugins() {
        return plugins;
    }

    public void setPlugins(PluginsProperties plugins) {
        this.plugins = plugins;
    }

    public static class StorageProperties {
        private String rocksdbPath = "./data/rocksdb";
        private Integer cacheSizeMb = 512;
        private Integer writeBufferSizeMb = 128;
        private String compression = "LZ4";
        private Integer maxOpenFiles = 1000;
        private Boolean createIfMissing = true;

        public String getRocksdbPath() {
            return rocksdbPath;
        }

        public void setRocksdbPath(String rocksdbPath) {
            this.rocksdbPath = rocksdbPath;
        }

        public Integer getCacheSizeMb() {
            return cacheSizeMb;
        }

        public void setCacheSizeMb(Integer cacheSizeMb) {
            this.cacheSizeMb = cacheSizeMb;
        }

        public Integer getWriteBufferSizeMb() {
            return writeBufferSizeMb;
        }

        public void setWriteBufferSizeMb(Integer writeBufferSizeMb) {
            this.writeBufferSizeMb = writeBufferSizeMb;
        }

        public String getCompression() {
            return compression;
        }

        public void setCompression(String compression) {
            this.compression = compression;
        }

        public Integer getMaxOpenFiles() {
            return maxOpenFiles;
        }

        public void setMaxOpenFiles(Integer maxOpenFiles) {
            this.maxOpenFiles = maxOpenFiles;
        }

        public Boolean getCreateIfMissing() {
            return createIfMissing;
        }

        public void setCreateIfMissing(Boolean createIfMissing) {
            this.createIfMissing = createIfMissing;
        }
    }

    public static class CacheProperties {
        private Integer maxActiveMerkle = 50;
        private String evictionPolicy = "LRU";
        private Integer ttlMinutes = 60;

        public Integer getMaxActiveMerkle() {
            return maxActiveMerkle;
        }

        public void setMaxActiveMerkle(Integer maxActiveMerkle) {
            this.maxActiveMerkle = maxActiveMerkle;
        }

        public String getEvictionPolicy() {
            return evictionPolicy;
        }

        public void setEvictionPolicy(String evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
        }

        public Integer getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(Integer ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }

    public static class RetentionProperties {
        private Integer maxMerkle = 100;
        private String archivePolicy = "oldest-first";
        private Integer autoArchiveThreshold = 90;

        public Integer getMaxMerkle() {
            return maxMerkle;
        }

        public void setMaxMerkle(Integer maxMerkle) {
            this.maxMerkle = maxMerkle;
        }

        public String getArchivePolicy() {
            return archivePolicy;
        }

        public void setArchivePolicy(String archivePolicy) {
            this.archivePolicy = archivePolicy;
        }

        public Integer getAutoArchiveThreshold() {
            return autoArchiveThreshold;
        }

        public void setAutoArchiveThreshold(Integer autoArchiveThreshold) {
            this.autoArchiveThreshold = autoArchiveThreshold;
        }
    }

    public static class PluginsProperties {
        private String path = "./plugins";
        private Map<String, Map<String, Object>> providers = new HashMap<>();
        private PolyglotProperties polyglot = new PolyglotProperties();

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, Map<String, Object>> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, Map<String, Object>> providers) {
            this.providers = providers;
        }

        public PolyglotProperties getPolyglot() {
            return polyglot;
        }

        public void setPolyglot(PolyglotProperties polyglot) {
            this.polyglot = polyglot;
        }
    }

    public static class PolyglotProperties {
        private boolean enabled = true;
        private boolean hotReload = false;
        private SandboxProperties sandbox = new SandboxProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isHotReload() {
            return hotReload;
        }

        public void setHotReload(boolean hotReload) {
            this.hotReload = hotReload;
        }

        public SandboxProperties getSandbox() {
            return sandbox;
        }

        public void setSandbox(SandboxProperties sandbox) {
            this.sandbox = sandbox;
        }
    }

    public static class SandboxProperties {
        private boolean restrictIo = true;
        private boolean allowNativeAccess = false;
        private long maxStatements = 10000000L;

        public boolean isRestrictIo() {
            return restrictIo;
        }

        public void setRestrictIo(boolean restrictIo) {
            this.restrictIo = restrictIo;
        }

        public boolean isAllowNativeAccess() {
            return allowNativeAccess;
        }

        public void setAllowNativeAccess(boolean allowNativeAccess) {
            this.allowNativeAccess = allowNativeAccess;
        }

        public long getMaxStatements() {
            return maxStatements;
        }

        public void setMaxStatements(long maxStatements) {
            this.maxStatements = maxStatements;
        }
    }
}
