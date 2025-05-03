package com.newApp.controller;

import com.newApp.model.ErpNextClient;
import com.newApp.service.ErpNextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping
public class ErpNextController {

    private final ErpNextService erpNextService;
    private final ErpNextClient erpNextClient;

    public ErpNextController(ErpNextService erpNextService, ErpNextClient erpNextClient) {
        this.erpNextService = erpNextService;
        this.erpNextClient = erpNextClient;
        System.out.println("ErpNextController initialized successfully.");
    }

    @GetMapping("/")
    public String redirectToLogin(HttpSession session) {
        System.out.println("Received request for /, checking authentication...");
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        if (isAuthenticated == null || !isAuthenticated) {
            System.out.println("User not authenticated, redirecting to /login");
            return "redirect:/login";
        }
        System.out.println("User authenticated, redirecting to /dashboard");
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, @RequestParam(value = "error", required = false) String error, @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        System.out.println("Received request for /login");
        if (error != null) {
            System.out.println("Error parameter present: " + errorMessage);
            model.addAttribute("error", errorMessage != null ? errorMessage : "Login failed. Please try again.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        System.out.println("Received POST request for /login with username: " + username);
        try {
            String loginError = erpNextClient.validateUserCredentials(username, password);
            if (loginError != null) {
                System.out.println("Login failed: " + loginError);
                return "redirect:/login?error=true&errorMessage=" + java.net.URLEncoder.encode(loginError, java.nio.charset.StandardCharsets.UTF_8);
            }

            System.out.println("Login successful, setting session attributes...");
            session.setAttribute("isAuthenticated", true);
            session.setAttribute("username", username);
            System.out.println("Redirecting to /dashboard");
            return "redirect:/dashboard";
        } catch (Exception e) {
            System.err.println("Unexpected error during login processing: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error=true&errorMessage=" + java.net.URLEncoder.encode("Unexpected error during login: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        System.out.println("Received request for /logout");
        session.invalidate();
        System.out.println("Session invalidated, redirecting to /login");
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        System.out.println("Received request for /dashboard");
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            String username = (String) session.getAttribute("username");
            if (username == null) {
                System.out.println("Username not found in session, redirecting to /login");
                return "redirect:/login";
            }
            System.out.println("User authenticated, rendering dashboard for username: " + username);
            model.addAttribute("username", username);
            return "dashboard";
        } catch (Exception e) {
            System.err.println("Unexpected error while rendering dashboard: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error=true&errorMessage=" + java.net.URLEncoder.encode("Unexpected error while rendering dashboard: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/suppliers")
    public String showSuppliers(HttpSession session, Model model) {
        System.out.println("Received request for /suppliers");
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            List suppliers = erpNextService.fetchSuppliers();
            model.addAttribute("suppliers", suppliers);
            return "suppliers";
        } catch (Exception e) {
            System.err.println("Error while fetching suppliers: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard?error=true&errorMessage=" + java.net.URLEncoder.encode("Error fetching suppliers: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/requests-for-quotation")
    public String showRequestsForQuotation(HttpSession session, Model model) {
        System.out.println("Received request for /requests-for-quotation");
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            List<Map<String, Object>> rfqs = erpNextService.fetchRequestsForQuotation();
            model.addAttribute("rfqs", rfqs);
            return "requests_for_quotation";
        } catch (Exception e) {
            System.err.println("Error while fetching requests for quotation: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard?error=true&errorMessage=" + java.net.URLEncoder.encode("Error fetching requests for quotation: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/update-price")
    public String updatePrice(@RequestParam String rfqName, @RequestParam String itemCode, @RequestParam double newPrice, HttpSession session) {
        System.out.println("Received POST request for /update-price for RFQ: " + rfqName);
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            String error = erpNextService.updatePrice(rfqName, itemCode, newPrice);
            if (error != null) {
                return "redirect:/requests-for-quotation?error=true&errorMessage=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8);
            }
            return "redirect:/requests-for-quotation";
        } catch (Exception e) {
            System.err.println("Error while updating price: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/requests-for-quotation?error=true&errorMessage=" + java.net.URLEncoder.encode("Error updating price: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/purchase-orders")
    public String showPurchaseOrders(HttpSession session, Model model) {
        System.out.println("Received request for /purchase-orders");
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            List<Map<String, Object>> orders = erpNextService.fetchPurchaseOrders();
            model.addAttribute("orders", orders);
            return "purchase-orders";
        } catch (Exception e) {
            System.err.println("Error while fetching purchase orders: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard?error=true&errorMessage=" + java.net.URLEncoder.encode("Error fetching purchase orders: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/purchase-invoices")
    public String showPurchaseInvoices(HttpSession session, Model model) {
        System.out.println("Received request for /purchase-invoices");
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            List<Map<String, Object>> invoices = erpNextService.fetchPurchaseInvoices();
            model.addAttribute("invoices", invoices);
            return "purchase_invoices";
        } catch (Exception e) {
            System.err.println("Error while fetching purchase invoices: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard?error=true&errorMessage=" + java.net.URLEncoder.encode("Error fetching purchase invoices: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/pay-invoice")
    public String payInvoice(@RequestParam String invoiceName, HttpSession session) {
        System.out.println("Received POST request for /pay-invoice for invoice: " + invoiceName);
        try {
            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            if (isAuthenticated == null || !isAuthenticated) {
                System.out.println("User not authenticated, redirecting to /login");
                return "redirect:/login";
            }
            String error = erpNextService.updateInvoiceStatus(invoiceName, "Paid");
            if (error != null) {
                return "redirect:/purchase-invoices?error=true&errorMessage=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8);
            }
            return "redirect:/purchase-invoices";
        } catch (Exception e) {
            System.err.println("Error while paying invoice: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/purchase-invoices?error=true&errorMessage=" + java.net.URLEncoder.encode("Error paying invoice: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}