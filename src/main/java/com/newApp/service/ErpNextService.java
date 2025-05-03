package com.newApp.service;

import com.newApp.model.ErpNextClient;
import com.newApp.model.Supplier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ErpNextService {

    private final ErpNextClient erpNextClient;

    public ErpNextService(ErpNextClient erpNextClient) {
        this.erpNextClient = erpNextClient;
    }

    public List<Supplier> fetchSuppliers() {
        return erpNextClient.getSuppliers();
    }

    public List<Map<String, Object>> fetchRequestsForQuotation() {
        return erpNextClient.getRequestsForQuotation();
    }

    public String updatePrice(String rfqName, String itemCode, double newPrice) {
        return erpNextClient.updatePrice(rfqName, itemCode, newPrice);
    }

    public List<Map<String, Object>> fetchPurchaseOrders() {
        return erpNextClient.getPurchaseOrders();
    }

    public List<Map<String, Object>> fetchPurchaseInvoices() {
        return erpNextClient.getPurchaseInvoices();
    }

    public String updateInvoiceStatus(String invoiceName, String status) {
        return erpNextClient.updateInvoiceStatus(invoiceName, status);
    }
}