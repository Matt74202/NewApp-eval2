package com.newApp.controller;

import com.newApp.model.ErpNextClient;
import com.newApp.model.PurchaseInvoice;
import com.newApp.service.paymententry.PaymentEntryService;
import com.newApp.service.purchaseinvoice.PurchaseInvoiceService;
import com.newApp.service.purchaseorder.PurchaseOrderService;
import com.newApp.service.supplier.SupplierService;
import com.newApp.service.supplierquotation.SupplierQuotationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class ErpNextController {

    private final ErpNextClient erpNextClient;
    private final SupplierService supplierService;
    private final SupplierQuotationService supplierQuotationService;
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final PaymentEntryService paymentEntryService;

    public ErpNextController(ErpNextClient erpNextClient,
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

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        String result = erpNextClient.validateUserCredentials(username, password);
        if (result == null) {
            return "redirect:/dashboard";
        } else {
            model.addAttribute("errorMessage", result);
            return "login";
        }
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model) {
        try {
            // Load RFQs (all, without supplier filter)
            List<Map<String, Object>> rfqs = supplierQuotationService.getRequestsForQuotation(null);
            model.addAttribute("rfqs", rfqs != null ? rfqs : new ArrayList<>());

            // Load purchase orders (all, without supplier filter)
            List<Map<String, Object>> purchaseOrders = purchaseOrderService.getPurchaseOrders(null);
            model.addAttribute("purchaseOrders", purchaseOrders != null ? purchaseOrders : new ArrayList<>());

            // Load purchase invoices (all, without supplier filter)
            List<Map<String, Object>> purchaseInvoices = purchaseInvoiceService.getPurchaseInvoices(null);
            model.addAttribute("purchaseInvoices", purchaseInvoices != null ? purchaseInvoices : new ArrayList<>());

            return "dashboard";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching dashboard data: " + e.getMessage());
            model.addAttribute("rfqs", new ArrayList<>());
            model.addAttribute("purchaseOrders", new ArrayList<>());
            model.addAttribute("purchaseInvoices", new ArrayList<>());
            return "dashboard";
        }
    }    @GetMapping("/rfqs")
    public String showRFQs(@RequestParam(required = false) String supplier, Model model) {
        try {
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            List<Map<String, Object>> rfqs = supplier != null && !supplier.isEmpty() 
                ? supplierQuotationService.getRequestsForQuotation(supplier) 
                : new ArrayList<>();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            model.addAttribute("rfqs", rfqs);
            model.addAttribute("selectedSupplier", supplier);
            return "rfqs";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching supplier quotations: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            model.addAttribute("rfqs", new ArrayList<>());
            model.addAttribute("selectedSupplier", supplier);
            return "rfqs";
        }
    }

    @GetMapping("/api/rfqs")
    @ResponseBody
    public ResponseEntity<?> getRFQs(@RequestParam String supplier) {
        try {
            List<Map<String, Object>> rfqs = supplierQuotationService.getRequestsForQuotation(supplier);
            return ResponseEntity.ok(rfqs);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body("No valid session. Please log in.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching Supplier Quotations: " + e.getMessage());
        }
    }

    @GetMapping("/purchase-orders")
    public String showPurchaseOrders(@RequestParam(required = false) String supplier, Model model) {
        try {
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            List<Map<String, Object>> orders = purchaseOrderService.getPurchaseOrders(supplier);
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            model.addAttribute("orders", orders != null ? orders : new ArrayList<>());
            model.addAttribute("selectedSupplier", supplier);
            return "purchase-orders";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching purchase orders: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            model.addAttribute("orders", new ArrayList<>());
            model.addAttribute("selectedSupplier", supplier);
            return "purchase-orders";
        }
    }

    @GetMapping("/purchase-invoices")
    public String showPurchaseInvoices(@RequestParam(required = false) String supplier, Model model) {
        try {
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            List<Map<String, Object>> invoices = purchaseInvoiceService.getPurchaseInvoices(supplier);
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            model.addAttribute("invoices", invoices != null ? invoices : new ArrayList<>());
            model.addAttribute("selectedSupplier", supplier);
            return "purchase-invoices";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching purchase invoices: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            model.addAttribute("invoices", new ArrayList<>());
            model.addAttribute("selectedSupplier", supplier);
            return "purchase-invoices";
        }
    }

    @GetMapping("/update-price")
    public String showUpdatePriceForm(@RequestParam String rfqName, @RequestParam String itemCode, 
                                     @RequestParam String supplier, Model model) {
        try {
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            model.addAttribute("rfqName", rfqName);
            model.addAttribute("itemCode", itemCode);
            model.addAttribute("supplier", supplier);
            return "update-price";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching suppliers: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            model.addAttribute("rfqName", rfqName);
            model.addAttribute("itemCode", itemCode);
            model.addAttribute("supplier", supplier);
            return "update-price";
        }
    }

    @PostMapping("/update-price")
    public String updatePrice(@RequestParam String rfqName, @RequestParam String itemCode,
                             @RequestParam double newPrice, @RequestParam String supplier, Model model) {
        try {
            String result = supplierQuotationService.updatePriceAndSubmit(rfqName, itemCode, newPrice, supplier);
            if (result == null) {
                return "redirect:/rfqs?supplier=" + URLEncoder.encode(supplier, StandardCharsets.UTF_8);
            } else {
                model.addAttribute("errorMessage", result);
                model.addAttribute("rfqName", rfqName);
                model.addAttribute("itemCode", itemCode);
                model.addAttribute("supplier", supplier);
                List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
                model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
                return "update-price";
            }
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating price: " + e.getMessage());
            model.addAttribute("rfqName", rfqName);
            model.addAttribute("itemCode", itemCode);
            model.addAttribute("supplier", supplier);
            return "update-price";
        }
    }

    @GetMapping("/update-invoice-status")
    public String showUpdateInvoiceStatusForm(@RequestParam String invoiceName, Model model) {
        try {
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            model.addAttribute("invoiceName", invoiceName);
            return "update-invoice-status";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching suppliers: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            return "update-invoice-status";
        }
    }

    @PostMapping("/update-invoice-status")
    public String updateInvoiceStatus(@RequestParam String invoiceName, @RequestParam String status,
                                     @RequestParam(required = false) String supplier, Model model) {
        try {
            String result = purchaseInvoiceService.updateInvoiceStatus(invoiceName, status, supplier);
            if (result == null) {
                return "redirect:/purchase-invoices?supplier=" + URLEncoder.encode(supplier != null ? supplier : "", StandardCharsets.UTF_8);
            } else {
                model.addAttribute("errorMessage", result);
                model.addAttribute("invoiceName", invoiceName);
                List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
                model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
                return "update-invoice-status";
            }
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating invoice status: " + e.getMessage());
            model.addAttribute("invoiceName", invoiceName);
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            return "update-invoice-status";
        }
    }

    @GetMapping("/rfq-items")
    public String rfqItems(@RequestParam("rfqName") String rfqName, 
                          @RequestParam("supplier") String supplier, 
                          Model model) {
        model.addAttribute("rfqName", rfqName);
        model.addAttribute("supplier", supplier);
        return "rfq-items";
    }

    @GetMapping("/create-payment")
    public String showPaymentForm(@RequestParam String invoiceName, @RequestParam(required = false) String supplier, Model model) {
        try {
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            model.addAttribute("invoiceName", invoiceName);
            model.addAttribute("supplier", supplier);
            return "create-payment";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching suppliers: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            model.addAttribute("invoiceName", invoiceName);
            model.addAttribute("supplier", supplier);
            return "create-payment";
        }
    }

    @PostMapping("/create-payment")
    public String processPayment(
            @RequestParam String invoiceName,
            @RequestParam(required = false) String supplier,
            @RequestParam double paymentAmount,
            @RequestParam String paymentDate,
            @RequestParam(required = false) String referenceNo,
            @RequestParam String paymentAccount,
            Model model) {
        try {
            String result = paymentEntryService.createPaymentEntry(invoiceName, supplier, paymentAmount, paymentDate, referenceNo, paymentAccount);
            if (result == null) {
                return "redirect:/purchase-invoices?supplier=" + URLEncoder.encode(supplier != null ? supplier : "", StandardCharsets.UTF_8);
            } else {
                model.addAttribute("errorMessage", result);
                model.addAttribute("invoiceName", invoiceName);
                model.addAttribute("supplier", supplier);
                List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
                model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
                return "create-payment";
            }
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error creating payment: " + e.getMessage());
            model.addAttribute("invoiceName", invoiceName);
            model.addAttribute("supplier", supplier);
            List<SupplierService.Supplier> suppliers = supplierService.getSuppliers();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            return "create-payment";
        }
    }

    @GetMapping("/invoice-details/{invoiceName}")
    public String showInvoiceDetails(@PathVariable String invoiceName, Model model) {
        try {
            Map<String, Object> invoiceDetails = purchaseInvoiceService.getPurchaseInvoiceDetails(invoiceName);

            if (invoiceDetails == null || invoiceDetails.isEmpty()) {
                model.addAttribute("errorMessage", "Aucune donnée trouvée pour la facture : " + invoiceName);
                return "invoice-details";
            }

            model.addAttribute("invoice", invoiceDetails);
            return "invoice-details";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement de la facture : " + e.getMessage());
            return "invoice-details";
        }
    }
}