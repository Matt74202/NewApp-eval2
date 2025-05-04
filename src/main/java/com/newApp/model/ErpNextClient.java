package com.newApp.model;

import com.newApp.service.paymententry.PaymentEntryService;
import com.newApp.service.purchaseinvoice.PurchaseInvoiceService;
import com.newApp.service.purchaseorder.PurchaseOrderService;
import com.newApp.service.supplier.SupplierService;
import com.newApp.service.supplierquotation.SupplierQuotationService;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class ErpNextClient {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final SupplierService supplierService;
    private final SupplierQuotationService supplierQuotationService;
    private final PurchaseOrderService purchaseOrderService;
    private final PaymentEntryService paymentEntryService;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

    public ErpNextClient(RestTemplate restTemplate, CookieStore cookieStore,
                        PurchaseInvoiceService purchaseInvoiceService,
                        SupplierService supplierService,
                        SupplierQuotationService supplierQuotationService,
                        PurchaseOrderService purchaseOrderService,
                        PaymentEntryService paymentEntryService) {
        System.out.println("Initializing ErpNextClient...");
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
        this.purchaseInvoiceService = purchaseInvoiceService;
        this.supplierService = supplierService;
        this.supplierQuotationService = supplierQuotationService;
        this.purchaseOrderService = purchaseOrderService;
        this.paymentEntryService = paymentEntryService;
        System.out.println("ErpNextClient initialized successfully.");
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
        return purchaseInvoiceService.isSessionValid();
    }

    public List<SupplierService.Supplier> getSuppliers() {
        return supplierService.getSuppliers();
    }

    public List<Map<String, Object>> getRequestsForQuotation(String supplier) throws Exception {
        return supplierQuotationService.getRequestsForQuotation(supplier);
    }

    public String updatePrice(String quotationName, String itemCode, double newPrice, String supplier) {
        return supplierQuotationService.updatePrice(quotationName, itemCode, newPrice, supplier);
    }

    public List<Map<String, Object>> getPurchaseOrders(String supplier) {
        return purchaseOrderService.getPurchaseOrders(supplier);
    }

    public List<Map<String, Object>> getPurchaseInvoices(String supplier) {
        return purchaseInvoiceService.getPurchaseInvoices(supplier);
    }

    public String updateInvoiceStatus(String invoiceName, String status, String supplier) {
        return purchaseInvoiceService.updateInvoiceStatus(invoiceName, status, supplier);
    }

    public String createPaymentEntry(String invoiceName, String supplier, double paymentAmount, String paymentDate, String referenceNo, String paymentAccount) {
        return paymentEntryService.createPaymentEntry(invoiceName, supplier, paymentAmount, paymentDate, referenceNo, paymentAccount);
    }
}