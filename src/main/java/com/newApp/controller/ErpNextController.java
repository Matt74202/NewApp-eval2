package com.newApp.controller;

import com.newApp.model.Supplier;
import com.newApp.service.ErpNextService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ErpNextService erpNextService;

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        String result = erpNextService.validateUserCredentials(username, password);
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
            List<Supplier> suppliers = erpNextService.getSuppliers();
            model.addAttribute("suppliers", suppliers != null ? suppliers : new ArrayList<>());
            return "dashboard";
        } catch (IllegalStateException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error fetching suppliers: " + e.getMessage());
            model.addAttribute("suppliers", new ArrayList<>());
            return "dashboard";
        }
    }

    @GetMapping("/rfqs")
    public String showRFQs(@RequestParam(required = false) String supplier, Model model) {
        try {
            List<Supplier> suppliers = erpNextService.getSuppliers();
            List<Map<String, Object>> rfqs = supplier != null && !supplier.isEmpty() 
                ? erpNextService.getRequestsForQuotation(supplier) 
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
            List<Map<String, Object>> rfqs = erpNextService.getRequestsForQuotation(supplier);
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
            List<Supplier> suppliers = erpNextService.getSuppliers();
            List<Map<String, Object>> orders = erpNextService.getPurchaseOrders(supplier);
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
            List<Supplier> suppliers = erpNextService.getSuppliers();
            List<Map<String, Object>> invoices = erpNextService.getPurchaseInvoices(supplier);
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
            List<Supplier> suppliers = erpNextService.getSuppliers();
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
            String result = erpNextService.updatePrice(rfqName, itemCode, newPrice, supplier);
            if (result == null) {
                return "redirect:/rfqs?supplier=" + URLEncoder.encode(supplier, StandardCharsets.UTF_8);
            } else {
                model.addAttribute("errorMessage", result);
                model.addAttribute("rfqName", rfqName);
                model.addAttribute("itemCode", itemCode);
                model.addAttribute("supplier", supplier);
                List<Supplier> suppliers = erpNextService.getSuppliers();
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
            List<Supplier> suppliers = erpNextService.getSuppliers();
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
    @GetMapping("/rfq-items")
    public String rfqItems(@RequestParam("rfqName") String rfqName, 
                          @RequestParam("supplier") String supplier, 
                          Model model) {
        // Add rfqName and supplier to the model if needed in the template
        model.addAttribute("rfqName", rfqName);
        model.addAttribute("supplier", supplier);
        return "rfq-items"; // Maps to src/main/resources/templates/rfq-items.html
    }

    // @PostMapping("/update-invoice-status")
    // public String updateInvoiceStatus(@RequestParam String invoiceName, @RequestParam String status,
    //                                  @RequestParam String supplier, Model model) {
    //     try {
    //         String result = erpNextService.updateInvoiceStatus(invoiceName, status, supplier);
    //         if (result == null) {
    //             return "redirect:/purchase-invoices?supplier=" + URLEncoder.encode(supplier, StandardCharsets.UTF_8);
    //         } else {
    //             model.addAttribute("errorMessage", result);
    //             model.addAttribute("invoiceName", invoiceName);
    //             List<Supplier> suppliers = erpNextService.getSuppliers();
    //             model.addAttribute("suppliers", suppliers != null ? suppliers :Venues.addAttribute("suppliers", new ArrayList<>());
    //             return "update-invoice-status";
    //         }
    //     } catch (IllegalStateException e) {
    //         return "redirect:/login";
    //     } catch (Exception e) {
    //         model.addAttribute("errorMessage", "Error updating invoice status: " + e.getMessage());
    //         return "update-invoice-status";
    //     }
    // }
}