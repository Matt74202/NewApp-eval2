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
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ErpNextClient {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

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

    public String validateUserCredentials(String username, String password) {
        try {
            System.out.println("Attempting to validate credentials for username: " + username);
            String endpoint = "method/login";
            String url = baseUrl + endpoint;
            System.out.println("Constructed URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String requestBody = "usr=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                               "&pwd=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            cookieStore.clear();
            System.out.println("Cleared cookies before login request.");

            System.out.println("Sending login request to: " + url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies received after login:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            String rawResponse = response.getBody() != null ? response.getBody().toString() : "null";
            System.out.println("Raw response from /api/method/login: " + rawResponse);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("full_name") && cookies.stream().anyMatch(c -> c.getName().equals("sid"))) {
                    System.out.println("Login successful with HTTP status: " + response.getStatusCode());
                    return null;
                } else {
                    System.out.println("Login failed: No 'full_name' or 'sid' cookie in response.");
                    return "Login failed: Invalid response or no session cookie.";
                }
            } else {
                System.out.println("Login failed: Invalid response. HTTP status: " + response.getStatusCode());
                return "Login failed: Invalid response. HTTP status: " + response.getStatusCode();
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

    public String updatePrice(String quotationName, String itemCode, double newPrice, String supplier) {
        try {
            if (!isSessionValid()) {
                return "No valid session. Please log in.";
            }

            String url = baseUrl + "resource/Supplier Quotation/" + URLEncoder.encode(quotationName, StandardCharsets.UTF_8);
            System.out.println("Updating price for Supplier Quotation: " + quotationName + ", item: " + itemCode + ", supplier: " + supplier);

            List<Cookie> cookies = cookieStore.getCookies();
            System.out.println("Cookies sent with request:");
            for (Cookie cookie : cookies) {
                System.out.println("Cookie: " + cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path=" + cookie.getPath());
            }

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Failed to fetch Supplier Quotation: " + quotationName;
            }

            Map<String, Object> quotationData = (Map<String, Object>) response.getBody().get("data");
            String quotationSupplier = (String) quotationData.get("supplier");
            if (supplier != null && !supplier.isEmpty() && !supplier.equals(quotationSupplier)) {
                return "Supplier Quotation " + quotationName + " does not belong to supplier: " + supplier;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) quotationData.get("items");
            for (Map<String, Object> item : items) {
                if (item.get("item_code").equals(itemCode)) {
                    item.put("rate", newPrice);
                    break;
                }
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(quotationData, new HttpHeaders());
            ResponseEntity<String> updateResponse = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully updated price for item " + itemCode + " in Supplier Quotation " + quotationName);
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

    public List<Map<String, Object>> getPurchaseOrders(String supplier) {
        try {
            if (!isSessionValid()) {
                throw new IllegalStateException("No valid session. Please log in.");
            }

            StringBuilder url = new StringBuilder(baseUrl + "resource/Purchase Order?fields=[\"*\"]");

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

   
    public String createPaymentEntry(String invoiceName, String supplier, double paymentAmount, String paymentDate, String referenceNo, String paymentAccount) {
        try {
            if (!isSessionValid()) {
                return "No valid session. Please log in.";
            }
    
            String url = baseUrl + "resource/Payment Entry";
            System.out.println("Creating payment entry for invoice: " + invoiceName + ", supplier: " + supplier + ", amount: " + paymentAmount);
    
            // Fetch invoice details
            String invoiceUrl = baseUrl + "resource/Purchase Invoice/" + URLEncoder.encode(invoiceName, StandardCharsets.UTF_8);
            ResponseEntity<Map> invoiceResponse = restTemplate.exchange(invoiceUrl, HttpMethod.GET, null, Map.class);
            if (!invoiceResponse.getStatusCode().is2xxSuccessful() || invoiceResponse.getBody() == null) {
                return "Failed to fetch invoice: " + invoiceName;
            }
    
            Map<String, Object> invoiceData = (Map<String, Object>) invoiceResponse.getBody().get("data");
            String invoiceSupplier = (String) invoiceData.get("supplier");
            if (supplier != null && !supplier.isEmpty() && !supplier.equals(invoiceSupplier)) {
                return "Invoice " + invoiceName + " does not belong to supplier: " + supplier;
            }
    
            // Get invoice amount and outstanding amount
            double grandTotal = ((Number) invoiceData.get("grand_total")).doubleValue();
            double outstandingAmount = ((Number) invoiceData.get("outstanding_amount")).doubleValue();
    
            // Check if payment exceeds outstanding amount
            if (paymentAmount > outstandingAmount) {
                return "Payment amount " + paymentAmount + " exceeds outstanding amount " + outstandingAmount;
            }
    
            // Calculate remaining balance
            double remainingBalance = outstandingAmount - paymentAmount;
    
            // Get invoice currency or default to MAD
            String invoiceCurrency = invoiceData.get("currency") != null ? 
                (String) invoiceData.get("currency") : "MAD";
    
            // Map paymentAccount to General Ledger account
            String paidFromAccount;
            switch (paymentAccount.toLowerCase()) {
                case "bank":
                    paidFromAccount = "Bank Account - AD";
                    break;
                case "cash":
                    paidFromAccount = "1101 - Main Cash - AD - AD";
                    break;
                default:
                    return "Invalid payment account: " + paymentAccount;
            }
    
            // Fetch account details
            String accountUrl = baseUrl + "resource/Account/" + paidFromAccount;
            ResponseEntity<Map> accountResponse = restTemplate.exchange(accountUrl, HttpMethod.GET, null, Map.class);
            
            if (!accountResponse.getStatusCode().is2xxSuccessful() || accountResponse.getBody() == null) {
                return "Account not found: " + paidFromAccount + ". Please verify the exact account name exists in ERPNext.";
            }
    
            Map<String, Object> accountData = (Map<String, Object>) accountResponse.getBody().get("data");
            String accountCurrency = (String) accountData.get("account_currency");
            if (accountCurrency == null) {
                accountCurrency = "MAD";
            }
    
            // Verify currencies match
            if (!invoiceCurrency.equals(accountCurrency)) {
                return "Currency mismatch: Invoice is in " + invoiceCurrency + " but account is in " + accountCurrency;
            }
    
            // Create Payment Entry payload
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("doctype", "Payment Entry");
            paymentData.put("payment_type", "Pay");
            paymentData.put("party_type", "Supplier");
            paymentData.put("party", invoiceSupplier);
            paymentData.put("paid_amount", paymentAmount);
            paymentData.put("received_amount", paymentAmount);
            paymentData.put("mode_of_payment", paymentAccount);
            paymentData.put("posting_date", paymentDate);
            paymentData.put("source_exchange_rate", 1.0);
            paymentData.put("paid_from", paidFromAccount);
            paymentData.put("paid_from_account_currency", accountCurrency);
            paymentData.put("currency", accountCurrency);
            if (referenceNo != null && !referenceNo.isEmpty()) {
                paymentData.put("reference_no", referenceNo);
            }
    
            List<Map<String, Object>> references = new ArrayList<>();
            Map<String, Object> reference = new HashMap<>();
            reference.put("reference_doctype", "Purchase Invoice");
            reference.put("reference_name", invoiceName);
            reference.put("allocated_amount", paymentAmount);
            reference.put("outstanding_amount", remainingBalance);
            references.add(reference);
            paymentData.put("references", references);
    
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentData, headers);
    
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
    
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> createdData = (Map<String, Object>) response.getBody().get("data");
                String paymentName = (String) createdData.get("name");
            
                // Submit payment entry
                String submitUrl = baseUrl + "resource/Payment Entry/" + paymentName;
                HttpHeaders submitHeaders = new HttpHeaders();
                submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            
                Map<String, Object> submitData = new HashMap<>();
                submitData.put("docstatus", 1);
            
                HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(submitData, submitHeaders);
                ResponseEntity<Map> submitResponse = restTemplate.exchange(submitUrl, HttpMethod.PUT, submitEntity, Map.class);
            
                if (submitResponse.getStatusCode().is2xxSuccessful()) {
                    System.out.println("Successfully submitted payment entry: " + paymentName);
                    if (remainingBalance > 0) {
                        return "Payment submitted. Remaining balance to pay: " + remainingBalance + " " + invoiceCurrency;
                    } else {
                        return "Payment submitted. Invoice fully paid.";
                    }
                } else {
                    return "Failed to submit Payment Entry: HTTP " + submitResponse.getStatusCode();
                }
            } else {
                return "Failed to create payment entry: HTTP " + response.getStatusCode();
            }
        } catch (HttpClientErrorException e) {
            System.err.println("HTTP Error creating payment entry: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return "HTTP Error creating payment entry: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("Error creating payment entry: " + e.getMessage());
            e.printStackTrace();
            return "Error creating payment entry: " + e.getMessage();
        }
    }
}