package com.bloxbean.cardano.dataprover.polyglot;

import com.bloxbean.cardano.dataprover.dto.CreateMerkleRequest;
import com.bloxbean.cardano.dataprover.dto.IngestRequest;
import com.bloxbean.cardano.dataprover.exception.MerkleNotFoundException;
import com.bloxbean.cardano.dataprover.service.IngestionService;
import com.bloxbean.cardano.dataprover.service.MerkleManagementService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scheduler for automated provider execution.
 * Supports cron expressions and interval-based scheduling.
 */
@Component
public class ProviderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ProviderScheduler.class);

    private static final Pattern INTERVAL_PATTERN = Pattern.compile("(\\d+)(m|h|d)");

    private final TaskScheduler taskScheduler;
    private final PolyglotPluginLoader pluginLoader;
    private final IngestionService ingestionService;
    private final MerkleManagementService merkleService;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public ProviderScheduler(@org.springframework.beans.factory.annotation.Autowired(required = false) TaskScheduler taskScheduler,
                             PolyglotPluginLoader pluginLoader,
                             IngestionService ingestionService,
                             MerkleManagementService merkleService) {
        this.taskScheduler = taskScheduler;
        this.pluginLoader = pluginLoader;
        this.ingestionService = ingestionService;
        this.merkleService = merkleService;
    }

    @PostConstruct
    public void scheduleProviders() {
        if (taskScheduler == null) {
            log.info("TaskScheduler not available, provider scheduling disabled");
            return;
        }

        Map<String, PolyglotProviderAdapter> providers = pluginLoader.getLoadedProviders();

        for (Map.Entry<String, PolyglotProviderAdapter> entry : providers.entrySet()) {
            String providerName = entry.getKey();
            PolyglotProviderAdapter adapter = entry.getValue();
            ScheduleConfig schedule = adapter.getManifest().getSchedule();

            if (schedule != null && schedule.isEnabled()) {
                scheduleProvider(providerName, schedule);
            }
        }

        log.info("Scheduled {} providers", scheduledTasks.size());
    }

    public void scheduleProvider(String providerName, ScheduleConfig config) {
        if (taskScheduler == null) {
            log.warn("Cannot schedule provider {}: TaskScheduler not available", providerName);
            return;
        }

        // Cancel existing schedule if any
        cancelSchedule(providerName);

        if (config.getCron() != null && !config.getCron().isEmpty()) {
            // Use cron trigger
            CronTrigger trigger = new CronTrigger(config.getCron());
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeProvider(providerName, config),
                    trigger
            );
            scheduledTasks.put(providerName, future);
            log.info("Scheduled provider {} with cron: {} ({})",
                    providerName, config.getCron(), config.getDescription());

        } else if (config.getInterval() != null && !config.getInterval().isEmpty()) {
            // Use fixed rate
            Duration interval = parseInterval(config.getInterval());
            ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                    () -> executeProvider(providerName, config),
                    interval
            );
            scheduledTasks.put(providerName, future);
            log.info("Scheduled provider {} with interval: {} ({})",
                    providerName, config.getInterval(), config.getDescription());
        }
    }

    public void cancelSchedule(String providerName) {
        ScheduledFuture<?> existing = scheduledTasks.remove(providerName);
        if (existing != null) {
            existing.cancel(false);
            log.info("Cancelled schedule for provider: {}", providerName);
        }
    }

    public void enableSchedule(String providerName) {
        PolyglotProviderAdapter adapter = pluginLoader.getProvider(providerName);
        if (adapter == null) {
            log.warn("Provider not found: {}", providerName);
            return;
        }

        ScheduleConfig schedule = adapter.getManifest().getSchedule();
        if (schedule != null) {
            scheduleProvider(providerName, schedule);
        }
    }

    public void disableSchedule(String providerName) {
        cancelSchedule(providerName);
    }

    public void triggerProvider(String providerName) {
        PolyglotProviderAdapter adapter = pluginLoader.getProvider(providerName);
        if (adapter == null) {
            log.warn("Provider not found for trigger: {}", providerName);
            return;
        }

        ScheduleConfig config = adapter.getManifest().getSchedule();
        if (config == null) {
            config = new ScheduleConfig();
        }

        executeProvider(providerName, config);
    }

    private void executeProvider(String providerName, ScheduleConfig config) {
        log.info("Executing scheduled provider: {}", providerName);

        try {
            String merkleName = resolvePlaceholders(config.getTargetMerkle(), providerName);

            // Auto-create merkle if configured
            if (config.isAutoCreateMerkle() && !merkleExists(merkleName)) {
                createMerkle(merkleName);
            }

            // Execute ingestion
            Map<String, Object> ingestionConfig = config.getDefaultConfig();
            if (ingestionConfig == null) {
                ingestionConfig = Map.of();
            }

            IngestRequest request = new IngestRequest();
            request.setProvider(providerName);
            request.setConfig(ingestionConfig);

            ingestionService.ingestData(merkleName, request);
            log.info("Scheduled execution completed for provider: {} -> merkle: {}",
                    providerName, merkleName);

        } catch (Exception e) {
            log.error("Scheduled execution failed for provider: {}", providerName, e);
        }
    }

    private String resolvePlaceholders(String template, String providerName) {
        if (template == null || template.isEmpty()) {
            return providerName + "-" + LocalDate.now();
        }

        String result = template;
        result = result.replace("{date}", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        result = result.replace("{datetime}",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result = result.replace("{timestamp}", String.valueOf(System.currentTimeMillis() / 1000));

        // Epoch placeholder - would need Cardano node connection for real value
        // For now, use a placeholder calculation
        result = result.replace("{epoch}", String.valueOf(calculateApproximateEpoch()));

        return result;
    }

    private int calculateApproximateEpoch() {
        // Cardano mainnet epoch calculation (approximate)
        // Shelley started at epoch 208 on 2020-07-29
        // Each epoch is 5 days
        long shelleyStart = 1596000000L; // Approximate Unix timestamp
        long now = System.currentTimeMillis() / 1000;
        long daysSinceShelley = (now - shelleyStart) / (24 * 60 * 60);
        return 208 + (int) (daysSinceShelley / 5);
    }

    private boolean merkleExists(String merkleName) {
        try {
            merkleService.getMerkle(merkleName);
            return true;
        } catch (MerkleNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void createMerkle(String merkleName) {
        try {
            CreateMerkleRequest request = new CreateMerkleRequest();
            request.setIdentifier(merkleName);
            request.setScheme("mpf");
            merkleService.createMerkle(request);
            log.info("Auto-created merkle: {}", merkleName);
        } catch (Exception e) {
            log.warn("Failed to auto-create merkle {}: {}", merkleName, e.getMessage());
        }
    }

    private Duration parseInterval(String interval) {
        Matcher matcher = INTERVAL_PATTERN.matcher(interval.toLowerCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid interval format: " + interval);
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unknown interval unit: " + unit);
        };
    }

    public Map<String, ScheduleStatus> getScheduleStatuses() {
        Map<String, ScheduleStatus> statuses = new ConcurrentHashMap<>();

        for (Map.Entry<String, PolyglotProviderAdapter> entry : pluginLoader.getLoadedProviders().entrySet()) {
            String name = entry.getKey();
            ScheduleConfig config = entry.getValue().getManifest().getSchedule();
            boolean isScheduled = scheduledTasks.containsKey(name);

            statuses.put(name, new ScheduleStatus(
                    name,
                    config != null && config.isEnabled(),
                    isScheduled,
                    config != null ? config.getCron() : null,
                    config != null ? config.getInterval() : null,
                    config != null ? config.getDescription() : null
            ));
        }

        return statuses;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cancelling {} scheduled tasks", scheduledTasks.size());
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(false);
        }
        scheduledTasks.clear();
    }

    public record ScheduleStatus(
            String providerName,
            boolean configuredEnabled,
            boolean currentlyScheduled,
            String cron,
            String interval,
            String description
    ) {}
}
