package com.newApp.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ErpNextClient {

    private final RestTemplate restTemplate;
    private final String baseUrl = "http://erpnext.localhost:8001/api/method/login";
    // Hardcoded API Key and Secret (must correspond to the ERPNext user being validated)
    private final String apiKey = "209316feabd854a";
    private final String apiSecret = "5e45c5187e6075e";

    public ErpNextClient() {
        this.restTemplate = new RestTemplate();
    }

    // Validate ERPNext username and password using the /api/method/login endpoint
    public String validateUserCredentials(String username, String password) {
        try {
            String url = baseUrl + "method/login";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            // Create the request body with username and password
            String requestBody = "usr=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8) +
                               "&pwd=" + java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode responseBody = parseResponse(response.getBody());

            if (responseBody.has("message") && responseBody.get("message").asText().equals("Logged In")) {
                return null; // Success, no error message
            } else {
                return "Invalid ERPNext username or password.";
            }
        } catch (HttpClientErrorException e) {
            return "HTTP Error during login validation: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            return "Failed to connect to ERPNext for login validation: " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected error during login validation: " + e.getMessage();
        }
    }

    // Test the hardcoded API credentials
    public String testCredentials() {
        try {
            getSuppliers();
            return null; // Success, no error message
        } catch (HttpClientErrorException e) {
            return "HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            return "Failed to connect to ERPNext: " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    private HttpHeaders createHeaders() {
        if (apiKey == null || apiSecret == null) {
            throw new IllegalStateException("API credentials are not set.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    public JsonNode getSuppliers() {
        String url = baseUrl + "resource/Supplier";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return parseResponse(response.getBody());
    }

    public JsonNode getPurchaseOrders() {
        String url = baseUrl + "resource/Purchase Order";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return parseResponse(response.getBody());
    }

    public JsonNode getQuotations() {
        String url = baseUrl + "resource/Quotation";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return parseResponse(response.getBody());
    }

    public JsonNode getPurchaseInvoices() {
        String url = baseUrl + "resource/Purchase Invoice";
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return parseResponse(response.getBody());
    }

    private JsonNode parseResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing response: " + e.getMessage());
        }
    }
}