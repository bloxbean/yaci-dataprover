package com.bloxbean.cardano.dataprover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the DataProver Server.
 */
@SpringBootApplication
public class DataProverApplication {

    public static void main(String[] args) {
        // Check for --ui argument and convert to property
        for (String arg : args) {
            if ("--ui".equals(arg) || "-ui".equals(arg)) {
                System.setProperty("dataprover.ui.enabled", "true");
                break;
            }
        }

        SpringApplication.run(DataProverApplication.class, args);
    }
}
