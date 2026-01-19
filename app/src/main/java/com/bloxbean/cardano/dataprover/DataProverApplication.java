package com.bloxbean.cardano.dataprover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the DataProver Server.
 */
@SpringBootApplication
public class DataProverApplication {

    public static void main(String[] args) {
        // Check for --generate-key argument
        for (String arg : args) {
            if ("--generate-key".equals(arg) || "-generate-key".equals(arg)) {
                generateAndPrintKey();
                return;  // Exit without starting Spring
            }
        }

        // Check for --ui argument and convert to property
        for (String arg : args) {
            if ("--ui".equals(arg) || "-ui".equals(arg)) {
                System.setProperty("dataprover.ui.enabled", "true");
                break;
            }
        }

        SpringApplication.run(DataProverApplication.class, args);
    }

    private static void generateAndPrintKey() {
        try {
            // Generate 32 random bytes (256 bits)
            byte[] key = new byte[32];
            java.security.SecureRandom.getInstanceStrong().nextBytes(key);

            // Encode as base64
            String base64Key = java.util.Base64.getEncoder().encodeToString(key);

            // Print with instructions
            System.out.println("============================================");
            System.out.println("AES-256 Encryption Key (base64):");
            System.out.println(base64Key);
            System.out.println();
            System.out.println("Set this as an environment variable:");
            System.out.println("  export DATAPROVER_ENCRYPTION_KEY=" + base64Key);
            System.out.println("============================================");
        } catch (Exception e) {
            System.err.println("Failed to generate key: " + e.getMessage());
            System.exit(1);
        }
    }
}
