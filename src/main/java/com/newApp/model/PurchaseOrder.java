package com.newApp.model;

public class PurchaseOrder {
    private String name;
    private String supplier;
    private String status;

    // Constructors
    public PurchaseOrder() {}

    public PurchaseOrder(String name, String supplier, String status) {
        this.name = name;
        this.supplier = supplier;
        this.status = status;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}