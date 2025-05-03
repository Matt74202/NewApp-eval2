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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ErpNextClient {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/"; // Using HTTP

    public ErpNextClient() {
        System.out.println("Initializing ErpNextClient...");
        this.cookieStore = new BasicCookieStore();
        this.restTemplate = createRestTemplateWithCookieSupport();
        System.out.println("ErpNextClient initialized successfully.");
    }

    private RestTemplate createRestTemplateWithCookieSupport() {
        System.out.println("Creating RestTemplate with cookie support...");
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultCookieStore(cookieStore)
            .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        System.out.println("RestTemplate created successfully.");
        return new RestTemplate(factory);
    }

    // Call the ERPNext login API without response verification
    public String validateUserCredentials(String username, String password) {
        try {
            System.out.println("Attempting to validate credentials for username: " + username);
            String endpoint = "method/login";
            String url = baseUrl + endpoint;
            System.out.println("Constructed URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String requestBody = "usr=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8) +
                               "&pwd=" + java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            cookieStore.clear();
            System.out.println("Cleared cookies before login request.");

            System.out.println("Sending login request to: " + url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies received after login:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            String rawResponse = response.getBody();
            System.out.println("Raw response from /api/method/login: " + rawResponse);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Login successful with HTTP status: " + response.getStatusCode());
                return null;
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

    // Fetch list of suppliers
    public List<Supplier> getSuppliers() {
        try {
            String url = baseUrl + "resource/Supplier";
            System.out.println("Fetching suppliers from: " + url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

            // Log the raw response for debugging
            System.out.println("Raw response from /api/resource/Supplier: " + response.getBody());

            List<Supplier> suppliers = new ArrayList<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    List<Map<String, Object>> supplierList = (List<Map<String, Object>>) data;
                    for (Map<String, Object> item : supplierList) {
                        Supplier supplier = new Supplier();
                        supplier.setName((String) item.get("name"));
                        suppliers.add(supplier);
                    }
                } else {
                    System.err.println("Response 'data' is not a list: " + data);
                }
            }
            System.out.println("Successfully fetched " + suppliers.size() + " suppliers.");
            return suppliers;
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error fetching suppliers: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching suppliers: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Fetch list of Requests for Quotation
    public List<Map<String, Object>> getRequestsForQuotation() {
        try {
            String url = baseUrl + "resource/Request for Quotation?fields=[\"*\"]";
            System.out.println("Fetching requests for quotation from: " + url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

            // Log the raw response for debugging
            System.out.println("Raw response from /api/resource/Request for Quotation: " + response.getBody());

            List<Map<String, Object>> rfqs = new ArrayList<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    rfqs = (List<Map<String, Object>>) data;
                } else {
                    System.err.println("Response 'data' is not a list: " + data);
                }
            }
            System.out.println("Successfully fetched " + rfqs.size() + " requests for quotation.");
            return rfqs;
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error fetching requests for quotation: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching requests for quotation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Update price for an item in a Request for Quotation
    public String updatePrice(String rfqName, String itemCode, double newPrice) {
        try {
            String url = baseUrl + "resource/Request for Quotation/" + rfqName;
            System.out.println("Updating price for RFQ: " + rfqName + ", item: " + itemCode);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Failed to fetch RFQ: " + rfqName;
            }

            Map<String, Object> rfqData = (Map<String, Object>) response.getBody().get("data");
            List<Map<String, Object>> items = (List<Map<String, Object>>) rfqData.get("items");

            for (Map<String, Object> item : items) {
                if (item.get("item_code").equals(itemCode)) {
                    item.put("rate", newPrice);
                    break;
                }
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(rfqData, new HttpHeaders());
            ResponseEntity<String> updateResponse = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully updated price for item " + itemCode + " in RFQ " + rfqName);
                return null;
            } else {
                return "Failed to update price: HTTP " + updateResponse.getStatusCode();
            }
        } catch (Exception e) {
            System.err.println("Error updating price: " + e.getMessage());
            e.printStackTrace();
            return "Error updating price: " + e.getMessage();
        }
    }

    // Fetch list of Purchase Orders
    public List<Map<String, Object>> getPurchaseOrders() {
        try {
            String url = baseUrl + "resource/Purchase Order?fields=[\"*\"]";
            System.out.println("Fetching purchase orders from: " + url);

            // Log cookies to ensure they are present
            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies being sent with request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

            // Log the raw response for debugging
            System.out.println("Raw response from /api/resource/Purchase Order: " + response.getBody());

            List<Map<String, Object>> orders = new ArrayList<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    orders = (List<Map<String, Object>>) data;
                } else {
                    System.err.println("Response 'data' is not a list: " + data);
                }
            }
            System.out.println("Successfully fetched " + orders.size() + " purchase orders.");
            return orders;
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error fetching purchase orders: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e; // Re-throw to let the controller handle it
        } catch (Exception e) {
            System.err.println("Error fetching purchase orders: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to let the controller handle it
        }
    }

    // Fetch list of Purchase Invoices
    public List<Map<String, Object>> getPurchaseInvoices() {
        try {
            String url = baseUrl + "resource/Purchase Invoice";
            System.out.println("Fetching purchase invoices from: " + url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

            // Log the raw response for debugging
            System.out.println("Raw response from /api/resource/Purchase Invoice: " + response.getBody());

            List<Map<String, Object>> invoices = new ArrayList<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    invoices = (List<Map<String, Object>>) data;
                } else {
                    System.err.println("Response 'data' is not a list: " + data);
                }
            }
            System.out.println("Successfully fetched " + invoices.size() + " purchase invoices.");
            return invoices;
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error fetching purchase invoices: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching purchase invoices: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Update invoice status (e.g., mark as Paid)
    public String updateInvoiceStatus(String invoiceName, String status) {
        try {
            String url = baseUrl + "resource/Purchase Invoice/" + invoiceName;
            System.out.println("Updating status for invoice: " + invoiceName + " to " + status);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Failed to fetch invoice: " + invoiceName;
            }

            Map<String, Object> invoiceData = (Map<String, Object>) response.getBody().get("data");
            invoiceData.put("status", status);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceData, new HttpHeaders());
            ResponseEntity<String> updateResponse = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully updated status for invoice " + invoiceName + " to " + status);
                return null;
            } else {
                return "Failed to update invoice status: HTTP " + updateResponse.getStatusCode();
            }
        } catch (Exception e) {
            System.err.println("Error updating invoice status: " + e.getMessage());
            e.printStackTrace();
            return "Error updating invoice status: " + e.getMessage();
        }
    }
}