package com.newApp.service.purchaseorder;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PurchaseOrderService {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

    public PurchaseOrderService(RestTemplate restTemplate, CookieStore cookieStore) {
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
        System.out.println("PurchaseOrderService initialized.");
    }

    public boolean isSessionValid() {
        List<Cookie> cookies = cookieStore.getCookies();
        boolean hasSid = cookies.stream().anyMatch(c -> c.getName().equals("sid"));
        System.out.println("Checking session validity: Has 'sid' cookie? " + hasSid);
        return hasSid;
    }

    public List<Map<String, Object>> getPurchaseOrders(String supplier) {
        try {
            if (!isSessionValid()) {
                throw new IllegalStateException("No valid session. Please log in.");
            }
    
            String fields = "[\"name\", \"supplier\", \"supplier_name\", \"status\", \"transaction_date\", \"grand_total\", \"per_billed\", \"per_received\"]";
            StringBuilder url = new StringBuilder(baseUrl + "resource/Purchase Order?fields=" + fields);
    
            if (supplier != null && !supplier.isEmpty()) {
                String rawFilter = "[[\"supplier\", \"=\", \"" + supplier + "\"]]";
                System.out.println("Raw filter: " + rawFilter);
                url.append("&filters=").append(rawFilter);
            }
    
            String finalUrl = url.toString();
            System.out.println("Fetching purchase orders from: " + finalUrl);
    
            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies sent with request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }
    
            ResponseEntity<Map> response = restTemplate.exchange(finalUrl, HttpMethod.GET, null, Map.class);
    
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
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching purchase orders: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    }