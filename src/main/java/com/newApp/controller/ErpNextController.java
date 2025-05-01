package com.newApp.controller;

import com.newApp.model.ErpNextClient;
import com.newApp.model.Supplier;
import com.newApp.service.ErpNextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping
public class ErpNextController {

    private final ErpNextService erpNextService;
    private final ErpNextClient erpNextClient;

    public ErpNextController(ErpNextService erpNextService, ErpNextClient erpNextClient) {
        this.erpNextService = erpNextService;
        this.erpNextClient = erpNextClient;
    }

    @GetMapping("/")
    public String redirectToLogin(HttpSession session) {
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        if (isAuthenticated == null || !isAuthenticated) {
            return "redirect:/login";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, @RequestParam(value = "error", required = false) String error, @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        if (error != null) {
            model.addAttribute("error", errorMessage != null ? errorMessage : "Invalid ERPNext username or password. Please try again.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        // Step 1: Validate ERPNext username and password
        String loginError = erpNextClient.validateUserCredentials(username, password);
        if (loginError != null) {
            return "redirect:/login?error=true&errorMessage=" + java.net.URLEncoder.encode(loginError, java.nio.charset.StandardCharsets.UTF_8);
        }

        // Step 2: Test the hardcoded API credentials
        String apiError = erpNextClient.testCredentials();
        if (apiError == null) {
            session.setAttribute("isAuthenticated", true);
            session.setAttribute("username", username);
            return "redirect:/dashboard";
        } else {
            return "redirect:/login?error=true&errorMessage=" + java.net.URLEncoder.encode("API credentials invalid: " + apiError, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        if (isAuthenticated == null || !isAuthenticated) {
            return "redirect:/login";
        }
        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
        return "dashboard";
    }

    @GetMapping("/suppliers")
    public String showSuppliers(Model model, HttpSession session) {
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        if (isAuthenticated == null || !isAuthenticated) {
            return "redirect:/login";
        }
        List<Supplier> suppliers = erpNextService.fetchSuppliers();
        model.addAttribute("suppliers", suppliers);
        return "suppliers";
    }
}