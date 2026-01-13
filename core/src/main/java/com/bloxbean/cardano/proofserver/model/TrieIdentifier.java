package com.bloxbean.cardano.proofserver.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object for trie identifiers.
 * Ensures identifiers follow the required format.
 */
public class TrieIdentifier {

    private static final Pattern VALID_PATTERN =
        Pattern.compile("^[a-z0-9]([a-z0-9-]{1,62}[a-z0-9])?$");

    private final String value;

    private TrieIdentifier(String value) {
        this.value = value;
    }

    /**
     * Creates a new TrieIdentifier from a string value.
     *
     * @param value the identifier value
     * @return the TrieIdentifier
     * @throws IllegalArgumentException if the value is invalid
     */
    public static TrieIdentifier of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trie identifier cannot be blank");
        }

        String normalized = value.toLowerCase().trim();

        if (!VALID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Trie identifier must be 3-64 chars, lowercase letters/numbers/hyphens only, " +
                "start and end with alphanumeric: " + value
            );
        }

        return new TrieIdentifier(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrieIdentifier that = (TrieIdentifier) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
