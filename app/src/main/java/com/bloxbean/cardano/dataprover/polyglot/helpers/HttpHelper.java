package com.bloxbean.cardano.dataprover.polyglot.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client helper exposed to polyglot scripts.
 * Provides GET and POST methods with JSON support.
 */
public class HttpHelper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient client;

    public HttpHelper() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @HostAccess.Export
    public Object get(String url, Value options) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

            applyOptions(requestBuilder, options);

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return parseJsonResponse(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP GET failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public Object get(String url) {
        return get(url, null);
    }

    @HostAccess.Export
    public Object post(String url, Value options) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url));

            String body = "";
            if (options != null && options.hasMember("body")) {
                Value bodyValue = options.getMember("body");
                if (bodyValue.isString()) {
                    body = bodyValue.asString();
                } else {
                    body = MAPPER.writeValueAsString(convertToJava(bodyValue));
                }
            }

            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));

            // Default content type for JSON
            requestBuilder.header("Content-Type", "application/json");

            applyOptions(requestBuilder, options);

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return parseJsonResponse(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP POST failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public Object put(String url, Value options) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url));

            String body = "";
            if (options != null && options.hasMember("body")) {
                Value bodyValue = options.getMember("body");
                if (bodyValue.isString()) {
                    body = bodyValue.asString();
                } else {
                    body = MAPPER.writeValueAsString(convertToJava(bodyValue));
                }
            }

            requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
            requestBuilder.header("Content-Type", "application/json");

            applyOptions(requestBuilder, options);

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return parseJsonResponse(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP PUT failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public Object delete(String url, Value options) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE();

            applyOptions(requestBuilder, options);

            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return parseJsonResponse(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP DELETE failed: " + e.getMessage(), e);
        }
    }

    private void applyOptions(HttpRequest.Builder requestBuilder, Value options) {
        if (options == null || options.isNull()) {
            return;
        }

        if (options.hasMember("timeout")) {
            int timeout = options.getMember("timeout").asInt();
            requestBuilder.timeout(Duration.ofMillis(timeout));
        }

        if (options.hasMember("headers")) {
            Value headers = options.getMember("headers");
            if (headers.hasMembers()) {
                for (String key : headers.getMemberKeys()) {
                    String value = headers.getMember(key).asString();
                    requestBuilder.header(key, value);
                }
            }
        }
    }

    private Object parseJsonResponse(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            // Try parsing as JSON
            if (body.trim().startsWith("{") || body.trim().startsWith("[")) {
                return MAPPER.readValue(body, Object.class);
            }
            return body;
        } catch (JsonProcessingException e) {
            // Return as string if not valid JSON
            return body;
        }
    }

    private Object convertToJava(Value value) {
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
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertToJava(value.getArrayElement(i));
            }
            return array;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, convertToJava(value.getMember(key)));
            }
            return map;
        }
        return value.toString();
    }
}
