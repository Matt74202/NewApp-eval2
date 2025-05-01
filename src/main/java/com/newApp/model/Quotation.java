package com.newApp.model;

public class Quotation {
    private String name;
    private String customerName;
    private String transactionDate;
    private double grandTotal;

    public Quotation() {}

    public Quotation(String name, String customerName, String transactionDate, double grandTotal) {
        this.name = name;
        this.customerName = customerName;
        this.transactionDate = transactionDate;
        this.grandTotal = grandTotal;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }
}