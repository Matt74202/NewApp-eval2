package com.newApp.controller;

import com.newApp.model.ErpNextClient;
import com.newApp.service.ErpNextService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

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
            // Validate ERPNext username and password using the API
            String loginError = erpNextClient.validateUserCredentials(username, password);
            if (loginError != null) {
                System.out.println("Login failed: " + loginError);
                return "redirect:/login?error=true&errorMessage=" + java.net.URLEncoder.encode(loginError, java.nio.charset.StandardCharsets.UTF_8);
            }

            // If login is successful (HTTP 200), mark the session as authenticated and redirect to dashboard
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
}