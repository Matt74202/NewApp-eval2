package com.newApp.service.paymententry;

import com.newApp.service.purchaseinvoice.PurchaseInvoiceService;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentEntryService {

    private final RestTemplate restTemplate;
    private final CookieStore cookieStore;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final String baseUrl = "http://erpnext.localhost:8001/api/";

    public PaymentEntryService(RestTemplate restTemplate, CookieStore cookieStore, PurchaseInvoiceService purchaseInvoiceService) {
        this.restTemplate = restTemplate;
        this.cookieStore = cookieStore;
        this.purchaseInvoiceService = purchaseInvoiceService;
        System.out.println("PaymentEntryService initialized.");
    }

    public String createPaymentEntry(String invoiceName, String supplier, double paymentAmount, String paymentDate, String referenceNo, String paymentAccount) {
        try {
            if (!purchaseInvoiceService.isSessionValid()) {
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