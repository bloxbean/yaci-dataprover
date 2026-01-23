package com.bloxbean.cardano.dataprover.polyglot.helpers;

import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging helper exposed to polyglot scripts.
 * Bridges script logging calls to SLF4J.
 */
public class ScriptLogger {
    private final Logger logger;

    public ScriptLogger(String providerName) {
        this.logger = LoggerFactory.getLogger("polyglot." + providerName);
    }

    @HostAccess.Export
    public void debug(String message) {
        logger.debug(message);
    }

    @HostAccess.Export
    public void info(String message) {
        logger.info(message);
    }

    @HostAccess.Export
    public void warn(String message) {
        logger.warn(message);
    }

    @HostAccess.Export
    public void error(String message) {
        logger.error(message);
    }

    @HostAccess.Export
    public void error(String message, Object error) {
        if (error instanceof Throwable) {
            logger.error(message, (Throwable) error);
        } else {
            logger.error("{}: {}", message, error);
        }
    }
}
