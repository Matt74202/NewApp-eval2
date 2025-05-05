package com.newApp.service.purchaseinvoice;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PurchaseInvoiceService {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

    public PurchaseInvoiceService(RestTemplate restTemplate, CookieStore cookieStore) {
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
        System.out.println("PurchaseInvoiceService initialized.");
    }

    public boolean isSessionValid() {
        List<Cookie> cookies = cookieStore.getCookies();
        boolean hasSid = cookies.stream().anyMatch(c -> c.getName().equals("sid"));
        System.out.println("Checking session validity: Has 'sid' cookie? " + hasSid);
        return hasSid;
    }

    public List<Map<String, Object>> getPurchaseInvoices(String supplier) {
        try {
            if (!isSessionValid()) {
                throw new IllegalStateException("No valid session. Please log in.");
            }

            StringBuilder url = new StringBuilder(baseUrl + "resource/Purchase Invoice?fields=[\"*\"]");
            if (supplier != null && !supplier.isEmpty()) {
                String filter = "[[\"supplier\",\"=\",\"" + supplier + "\"]]";
                System.out.println("Raw filter: " + filter);
                url.append("&filters=").append(filter);
            }

            String finalUrl = url.toString();
            System.out.println("Fetching purchase invoices from: " + finalUrl);

            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies sent with request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            ResponseEntity<Map> response = restTemplate.exchange(finalUrl, HttpMethod.GET, null, Map.class);

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

    public String updateInvoiceStatus(String invoiceName, String status, String supplier) {
        try {
            if (!isSessionValid()) {
                return "No valid session. Please log in.";
            }

            String url = baseUrl + "resource/Purchase Invoice/" + URLEncoder.encode(invoiceName, StandardCharsets.UTF_8);
            System.out.println("Updating status for invoice: " + invoiceName + " to " + status + ", supplier: " + supplier);

            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies sent with request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Failed to fetch invoice: " + invoiceName;
            }

            Map<String, Object> invoiceData = (Map<String, Object>) response.getBody().get("data");
            String invoiceSupplier = (String) invoiceData.get("supplier");
            if (supplier != null && !supplier.isEmpty() && !supplier.equals(invoiceSupplier)) {
                return "Invoice " + invoiceName + " does not belong to supplier: " + supplier;
            }

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
    public Map<String, Object> getPurchaseInvoiceDetails(String invoiceName) {
    try {
        if (!isSessionValid()) {
            throw new IllegalStateException("No valid session. Please log in.");
        }

        if (invoiceName == null || invoiceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice name must not be null or empty.");
        }

        String url = baseUrl + "resource/Purchase Invoice/" + invoiceName;
        System.out.println("Fetching purchase invoice details from: " + url);

        List<Cookie> cookies = cookieStore.getCookies();
        System.out.println("Cookies sent with request:");
        for (Cookie cookie : cookies) {
            System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
        }

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

        System.out.println("Raw response from /api/resource/Purchase Invoice/" + invoiceName + ": " + response.getBody());

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object data = response.getBody().get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            } else {
                System.err.println("Response 'data' is not a map: " + data);
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
    } catch (HttpClientErrorException e) {
        System.err.println("HTTP Error fetching purchase invoice details: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        throw e;
    } catch (Exception e) {
        System.err.println("Error fetching purchase invoice details: " + e.getMessage());
        e.printStackTrace();
        throw e;
    }
}
    
}