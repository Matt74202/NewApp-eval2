package com.newApp.service.supplierquotation;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SupplierQuotationService {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

    public SupplierQuotationService(RestTemplate restTemplate, CookieStore cookieStore) {
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
        System.out.println("SupplierQuotationService initialized.");
    }

    public boolean isSessionValid() {
        List<Cookie> cookies = cookieStore.getCookies();
        boolean hasSid = cookies.stream().anyMatch(c -> c.getName().equals("sid"));
        System.out.println("Checking session validity: Has 'sid' cookie? " + hasSid);
        return hasSid;
    }

    public List<Map<String, Object>> getRequestsForQuotation(String supplier) throws Exception {
        if (!isSessionValid()) {
            throw new IllegalStateException("No valid session. Please log in.");
        }

        String fields = "[\"name\",\"title\",\"status\",\"supplier\",\"transaction_date\",\"grand_total\",\"items.item_code\",\"items.item_name\",\"items.qty\",\"items.rate\",\"items.amount\",\"items.stock_uom\",\"items.uom\",\"items.conversion_factor\",\"items.base_rate\",\"items.base_amount\",\"items.warehouse\"]";
        StringBuilder url = new StringBuilder(baseUrl + "resource/Supplier Quotation?fields=" + fields);

        if (supplier != null && !supplier.isEmpty()) {
            String filters = "[[\"supplier\",\"=\",\"" + supplier + "\"]]";
            url.append("&filters=").append(filters);
        }

        String finalUrl = url.toString();
        System.out.println("Final URL: " + finalUrl);

        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(finalUrl, HttpMethod.GET, requestEntity, Map.class);

        System.out.println("Response: " + response.getBody());

        List<Map<String, Object>> rawQuotations = new ArrayList<>();
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object data = response.getBody().get("data");
            if (data instanceof List) {
                rawQuotations = (List<Map<String, Object>>) data;
            } else {
                System.err.println("Unexpected response structure: " + data);
            }
        }

        Map<String, Map<String, Object>> quotationMap = new HashMap<>();
        for (Map<String, Object> rawQuotation : rawQuotations) {
            String name = (String) rawQuotation.get("name");
            if (!quotationMap.containsKey(name)) {
                Map<String, Object> quotation = new HashMap<>();
                quotation.put("name", name);
                quotation.put("title", rawQuotation.get("title"));
                quotation.put("status", rawQuotation.get("status"));
                quotation.put("supplier", rawQuotation.get("supplier"));
                quotation.put("transaction_date", rawQuotation.get("transaction_date"));
                quotation.put("grand_total", rawQuotation.get("grand_total"));
                quotation.put("items", new ArrayList<Map<String, Object>>());
                quotationMap.put(name, quotation);
            }

            Map<String, Object> item = new HashMap<>();
            item.put("item_code", rawQuotation.get("item_code"));
            item.put("item_name", rawQuotation.get("item_name"));
            item.put("qty", rawQuotation.get("qty"));
            item.put("rate", rawQuotation.get("rate"));
            item.put("amount", rawQuotation.get("amount"));
            item.put("stock_uom", rawQuotation.get("stock_uom"));
            item.put("uom", rawQuotation.get("uom"));
            item.put("conversion_factor", rawQuotation.get("conversion_factor"));
            item.put("base_rate", rawQuotation.get("base_rate"));
            item.put("base_amount", rawQuotation.get("base_amount"));
            item.put("warehouse", rawQuotation.get("warehouse"));

            if (item.get("item_code") != null) {
                ((List<Map<String, Object>>) quotationMap.get(name).get("items")).add(item);
            }
        }

        return new ArrayList<>(quotationMap.values());
    }

    public String updatePriceAndSubmit(String quotationName, String itemCode, double newPrice, String supplier) {
        try {
            if (!isSessionValid()) {
                return "No valid session. Please log in.";
            }
    
            // Étape 1: Récupérer le Supplier Quotation
            String url = baseUrl + "resource/Supplier Quotation/" + URLEncoder.encode(quotationName, StandardCharsets.UTF_8);
            System.out.println("Updating price for Supplier Quotation: " + quotationName + ", item: " + itemCode + ", supplier: " + supplier);
    
            // Récupération des données du Supplier Quotation
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Failed to fetch Supplier Quotation: " + quotationName;
            }
    
            Map<String, Object> quotationData = (Map<String, Object>) response.getBody().get("data");
            String quotationSupplier = (String) quotationData.get("supplier");
            if (supplier != null && !supplier.isEmpty() && !supplier.equals(quotationSupplier)) {
                return "Supplier Quotation " + quotationName + " does not belong to supplier: " + supplier;
            }
    
            // Mise à jour du prix de l'article
            List<Map<String, Object>> items = (List<Map<String, Object>>) quotationData.get("items");
            boolean itemFound = false;
            for (Map<String, Object> item : items) {
                if (item.get("item_code").equals(itemCode)) {
                    item.put("rate", newPrice);
                    itemFound = true;
                    break;
                }
            }
    
            if (!itemFound) {
                return "Item " + itemCode + " not found in Supplier Quotation.";
            }
    
            // Sauvegarde de la modification
            HttpHeaders updateHeaders = new HttpHeaders();
            updateHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(quotationData, updateHeaders);
    
            ResponseEntity<String> updateResponse = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            if (!updateResponse.getStatusCode().is2xxSuccessful()) {
                return "Failed to update price: HTTP " + updateResponse.getStatusCode();
            }
    
            System.out.println("Successfully updated price for item " + itemCode + " in Supplier Quotation " + quotationName);
    
            // Étape 2: Soumettre le document avec run_method=submit
            String submitUrl = baseUrl + "resource/Supplier Quotation/" + 
                   URLEncoder.encode(quotationName, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("run_method", "submit");

            HttpEntity<MultiValueMap<String, String>> submitEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> submitResponse = restTemplate.exchange(
                submitUrl,
                HttpMethod.POST,
                submitEntity,
                String.class
            );

    
            if (submitResponse.getStatusCode().is2xxSuccessful()) {
                return null; // Succès
            } else {
                return "Submit failed: " + submitResponse.getBody();
            }
    
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
}