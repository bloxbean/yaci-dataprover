package com.bloxbean.cardano.dataprover.polyglot.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File access helper exposed to polyglot scripts.
 * Restricted to the provider directory for security.
 */
public class FileHelper {
    private static final Logger log = LoggerFactory.getLogger(FileHelper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path providerDir;

    public FileHelper(Path providerDir) {
        this.providerDir = providerDir.toAbsolutePath().normalize();
    }

    @HostAccess.Export
    public String readText(String relativePath) {
        Path filePath = resolveSafePath(relativePath);
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + relativePath, e);
        }
    }

    @HostAccess.Export
    public Object readJson(String relativePath) {
        Path filePath = resolveSafePath(relativePath);
        try {
            return MAPPER.readValue(filePath.toFile(), Object.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + relativePath, e);
        }
    }

    @HostAccess.Export
    public List<Map<String, String>> readCsv(String relativePath) {
        return readCsv(relativePath, ",", true);
    }

    @HostAccess.Export
    public List<Map<String, String>> readCsv(String relativePath, String delimiter, boolean hasHeader) {
        Path filePath = resolveSafePath(relativePath);
        List<Map<String, String>> results = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String[] headers = null;

            if (hasHeader) {
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    headers = parseCsvLine(headerLine, delimiter);
                }
            }

            String line;
            int rowIndex = 0;
            while ((line = reader.readLine()) != null) {
                String[] values = parseCsvLine(line, delimiter);
                Map<String, String> row = new LinkedHashMap<>();

                if (headers != null) {
                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        row.put(headers[i].trim(), values[i].trim());
                    }
                } else {
                    for (int i = 0; i < values.length; i++) {
                        row.put("col" + i, values[i].trim());
                    }
                }

                results.add(row);
                rowIndex++;
            }

            log.debug("Read {} rows from CSV: {}", rowIndex, relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + relativePath, e);
        }

        return results;
    }

    @HostAccess.Export
    public List<String> readLines(String relativePath) {
        Path filePath = resolveSafePath(relativePath);
        try {
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines from file: " + relativePath, e);
        }
    }

    @HostAccess.Export
    public byte[] readBytes(String relativePath) {
        Path filePath = resolveSafePath(relativePath);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bytes from file: " + relativePath, e);
        }
    }

    @HostAccess.Export
    public boolean exists(String relativePath) {
        try {
            Path filePath = resolveSafePath(relativePath);
            return Files.exists(filePath);
        } catch (SecurityException e) {
            return false;
        }
    }

    private Path resolveSafePath(String relativePath) {
        Path resolved = providerDir.resolve(relativePath).toAbsolutePath().normalize();

        // Security check: ensure the resolved path is within the provider directory
        if (!resolved.startsWith(providerDir)) {
            throw new SecurityException("Access denied: path escapes provider directory: " + relativePath);
        }

        return resolved;
    }

    private String[] parseCsvLine(String line, String delimiter) {
        // Simple CSV parsing (doesn't handle quoted values with delimiters)
        return line.split(delimiter, -1);
    }
}
