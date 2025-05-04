package com.newApp.service.supplier;

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
public class SupplierService {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

    public class Supplier {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public SupplierService(RestTemplate restTemplate, CookieStore cookieStore) {
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
        System.out.println("SupplierService initialized.");
    }

    public boolean isSessionValid() {
        List<Cookie> cookies = cookieStore.getCookies();
        boolean hasSid = cookies.stream().anyMatch(c -> c.getName().equals("sid"));
        System.out.println("Checking session validity: Has 'sid' cookie? " + hasSid);
        return hasSid;
    }

    public List<Supplier> getSuppliers() {
        try {
            if (!isSessionValid()) {
                throw new IllegalStateException("No valid session. Please log in.");
            }

            String url = baseUrl + "resource/Supplier";
            System.out.println("Fetching suppliers from: " + url);

            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies sent with request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);

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
}