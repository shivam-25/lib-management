package com.schoolmgmt.report.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmgmt.report.exception.BackendApiException;
import com.schoolmgmt.report.model.Student;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SchoolApiClient {

    private final String baseUrl;
    private final String username;
    private final String password;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchoolApiClient(
            @Value("${backend.base-url}") String baseUrl,
            @Value("${backend.username}") String username,
            @Value("${backend.password}") String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Student fetchStudent(String id) {
        Map<String, String> cookies = login();
        String csrfToken = cookies.get("csrfToken");
        if (csrfToken == null) {
            throw new BackendApiException(HttpStatus.BAD_GATEWAY, "Missing CSRF token from backend login response");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/students/" + id))
                .timeout(Duration.ofSeconds(15))
                .header("Cookie", toCookieHeader(cookies))
                .header("x-csrf-token", csrfToken)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            throw new BackendApiException(HttpStatus.NOT_FOUND, "Student not found");
        }
        if (response.statusCode() != 200) {
            throw new BackendApiException(HttpStatus.BAD_GATEWAY,
                    "Failed to fetch student. Backend responded with status " + response.statusCode());
        }

        try {
            return objectMapper.readValue(response.body(), Student.class);
        } catch (Exception e) {
            throw new BackendApiException(HttpStatus.BAD_GATEWAY, "Unable to parse student data");
        }
    }

    private Map<String, String> login() {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        } catch (Exception e) {
            throw new BackendApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to build login request");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new BackendApiException(HttpStatus.BAD_GATEWAY,
                    "Backend login failed with status " + response.statusCode());
        }

        Map<String, String> cookies = parseCookies(response.headers().allValues("set-cookie"));
        if (cookies.isEmpty()) {
            throw new BackendApiException(HttpStatus.BAD_GATEWAY, "Backend login did not return session cookies");
        }
        return cookies;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new BackendApiException(HttpStatus.BAD_GATEWAY, "Unable to reach the backend API");
        }
    }

    private Map<String, String> parseCookies(List<String> setCookieHeaders) {
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String header : setCookieHeaders) {
            String pair = header.split(";", 2)[0].trim();
            int eq = pair.indexOf('=');
            if (eq > 0) {
                cookies.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return cookies;
    }

    private String toCookieHeader(Map<String, String> cookies) {
        return cookies.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
    }
}
