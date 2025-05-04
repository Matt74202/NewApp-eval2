package com.newApp.service;

import com.newApp.model.ErpNextClient;
import com.newApp.model.Supplier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ErpNextService {

    private final ErpNextClient erpNextClient;

    public ErpNextService(ErpNextClient erpNextClient) {
        this.erpNextClient = erpNextClient;
    }

    public String validateUserCredentials(String username, String password) {
        return erpNextClient.validateUserCredentials(username, password);
    }

    public List<Supplier> getSuppliers() {
        return erpNextClient.getSuppliers();
    }

    public List<Map<String, Object>> getRequestsForQuotation(String supplier) throws Exception {
        return erpNextClient.getRequestsForQuotation(supplier);
    }

    public String updatePrice(String rfqName, String itemCode, double newPrice, String supplier) {
        return erpNextClient.updatePrice(rfqName, itemCode, newPrice, supplier);
    }

    public List<Map<String, Object>> getPurchaseOrders(String supplier) {
        return erpNextClient.getPurchaseOrders(supplier);
    }

    public List<Map<String, Object>> getPurchaseInvoices(String supplier) {
        return erpNextClient.getPurchaseInvoices(supplier);
    }

    public String updateInvoiceStatus(String invoiceName, String status, String supplier) {
        return erpNextClient.updateInvoiceStatus(invoiceName, status, supplier);
    }
    
}