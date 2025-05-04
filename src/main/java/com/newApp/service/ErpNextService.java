package com.newApp.service;

import com.newApp.model.ErpNextClient;
import com.newApp.service.paymententry.PaymentEntryService;
import com.newApp.service.purchaseinvoice.PurchaseInvoiceService;
import com.newApp.service.purchaseorder.PurchaseOrderService;
import com.newApp.service.supplier.SupplierService;
import com.newApp.service.supplierquotation.SupplierQuotationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ErpNextService {

    private final ErpNextClient erpNextClient;
    private final SupplierService supplierService;
    private final SupplierQuotationService supplierQuotationService;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final PaymentEntryService paymentEntryService;

    public ErpNextService(ErpNextClient erpNextClient,
                         SupplierService supplierService,
                         SupplierQuotationService supplierQuotationService,
                         PurchaseOrderService purchaseOrderService,
                         PurchaseInvoiceService purchaseInvoiceService,
                         PaymentEntryService paymentEntryService) {
        this.erpNextClient = erpNextClient;
        this.supplierService = supplierService;
        this.supplierQuotationService = supplierQuotationService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseInvoiceService = purchaseInvoiceService;
        this.paymentEntryService = paymentEntryService;
    }

    public String validateUserCredentials(String username, String password) {
        return erpNextClient.validateUserCredentials(username, password);
    }

    public List<SupplierService.Supplier> getSuppliers() {
        return supplierService.getSuppliers();
    }

    public List<Map<String, Object>> getRequestsForQuotation(String supplier) throws Exception {
        return supplierQuotationService.getRequestsForQuotation(supplier);
    }

    public String updatePrice(String rfqName, String itemCode, double newPrice, String supplier) {
        return supplierQuotationService.updatePrice(rfqName, itemCode, newPrice, supplier);
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