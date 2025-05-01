package com.newApp.model;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ErpNextClient {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/"; // Changed to HTTP

    public ErpNextClient() {
        System.out.println("Initializing ErpNextClient...");
        // Create a cookie store to manage cookies
        this.cookieStore = new BasicCookieStore();
        // Create a custom RestTemplate with cookie support
        this.restTemplate = createRestTemplateWithCookieSupport();
        System.out.println("ErpNextClient initialized successfully.");
    }

    private RestTemplate createRestTemplateWithCookieSupport() {
        System.out.println("Creating RestTemplate with cookie support...");
        // Create a connection manager
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .build();

        // Create an HTTP client that uses the connection manager and cookie store
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultCookieStore(cookieStore)
            .build();

        // Create a request factory that uses the HTTP client
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        System.out.println("RestTemplate created successfully.");
        return new RestTemplate(factory);
    }

    // Call the ERPNext login API without response verification
    public String validateUserCredentials(String username, String password) {
        try {
            System.out.println("Attempting to validate credentials for username: " + username);
            String url = baseUrl + "method/login";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            // Create the request body with username and password
            String requestBody = "usr=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8) +
                               "&pwd=" + java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Clear cookies before login to ensure a fresh session
            cookieStore.clear();
            System.out.println("Cleared cookies before login request.");

            System.out.println("Sending login request to: " + url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // Log the cookies received in the response
            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies received after login:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            // Log the raw response for debugging
            String rawResponse = response.getBody();
            System.out.println("Raw response from /api/method/login: " + rawResponse);

            // Check if the HTTP status is 200 (OK)
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Login successful with HTTP status: " + response.getStatusCode());
                return null; // Success, no error message
            } else {
                System.out.println("Login failed with HTTP status: " + response.getStatusCode());
                return "Login failed with HTTP status: " + response.getStatusCode();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error during login: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return "HTTP Error during login: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            System.err.println("Failed to connect to ERPNext for login: " + e.getMessage());
            return "Failed to connect to ERPNext for login: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("Unexpected error during login: " + e.getMessage());
            e.printStackTrace();
            return "Unexpected error during login: " + e.getMessage();
        }
    }
}