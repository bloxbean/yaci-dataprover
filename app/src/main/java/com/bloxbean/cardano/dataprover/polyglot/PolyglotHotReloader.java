package com.bloxbean.cardano.dataprover.polyglot;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File watcher for hot-reloading polyglot provider scripts during development.
 * Watches for changes to provider.js/py files and reloads them automatically.
 */
@Component
public class PolyglotHotReloader {
    private static final Logger log = LoggerFactory.getLogger(PolyglotHotReloader.class);

    private final DataProverProperties properties;
    private final PolyglotPluginLoader pluginLoader;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService watchExecutor;
    private WatchService watchService;

    public PolyglotHotReloader(DataProverProperties properties,
                                PolyglotPluginLoader pluginLoader) {
        this.properties = properties;
        this.pluginLoader = pluginLoader;
    }

    @PostConstruct
    public void start() {
        if (!properties.getPlugins().getPolyglot().isEnabled()) {
            return;
        }
        if (!properties.getPlugins().getPolyglot().isHotReload()) {
            log.info("Hot-reload is disabled for polyglot providers");
            return;
        }

        try {
            startWatching();
        } catch (IOException e) {
            log.error("Failed to start hot-reload watcher", e);
        }
    }

    private void startWatching() throws IOException {
        Path pluginsDir = Paths.get(properties.getPlugins().getPath());

        if (!Files.exists(pluginsDir)) {
            log.warn("Plugins directory does not exist, hot-reload disabled: {}", pluginsDir);
            return;
        }

        watchService = FileSystems.getDefault().newWatchService();

        // Register each provider directory for watching
        Map<String, PolyglotProviderAdapter> providers = pluginLoader.getLoadedProviders();
        for (PolyglotProviderAdapter adapter : providers.values()) {
            Path providerDir = adapter.getProviderDir();
            if (Files.exists(providerDir)) {
                providerDir.register(watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);
                log.debug("Registered hot-reload watch for: {}", providerDir);
            }
        }

        running.set(true);
        watchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "polyglot-hot-reload");
            t.setDaemon(true);
            return t;
        });

        watchExecutor.submit(this::watchLoop);
        log.info("Hot-reload watcher started for {} providers", providers.size());
    }

    private void watchLoop() {
        while (running.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changed = pathEvent.context();

                    // Only reload if it's a provider script file
                    String filename = changed.getFileName().toString();
                    if (filename.equals("provider.js") || filename.equals("provider.py")) {
                        handleFileChange(key, changed);
                    }
                }

                if (!key.reset()) {
                    log.warn("Watch key no longer valid");
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            } catch (Exception e) {
                log.error("Error in hot-reload watch loop", e);
            }
        }
    }

    private void handleFileChange(WatchKey key, Path changedFile) {
        // Find which provider this file belongs to
        Path watchedDir = (Path) key.watchable();
        String dirName = watchedDir.getFileName().toString();

        Map<String, PolyglotProviderAdapter> providers = pluginLoader.getLoadedProviders();

        for (Map.Entry<String, PolyglotProviderAdapter> entry : providers.entrySet()) {
            PolyglotProviderAdapter adapter = entry.getValue();
            if (adapter.getProviderDir().getFileName().toString().equals(dirName)) {
                log.info("Detected change in {}, reloading provider: {}",
                        changedFile, entry.getKey());

                // Small delay to ensure file write is complete
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                pluginLoader.reloadProvider(entry.getKey());
                break;
            }
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Error closing watch service", e);
            }
        }

        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
        }

        log.info("Hot-reload watcher stopped");
    }
}
