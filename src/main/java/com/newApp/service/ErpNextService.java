package com.newApp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.newApp.model.ErpNextClient;
import com.newApp.model.Supplier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ErpNextService {

    private final ErpNextClient erpNextClient;

    public ErpNextService(ErpNextClient erpNextClient) {
        this.erpNextClient = erpNextClient;
    }

    public List<Supplier> fetchSuppliers() {
        try {
            JsonNode response = erpNextClient.getSuppliers();
            List<Supplier> suppliers = new ArrayList<>();

            if (response.has("data")) {
                for (JsonNode node : response.get("data")) {
                    String name = node.get("name").asText();
                    suppliers.add(new Supplier(name));
                }
            }
            return suppliers;
        } catch (NoSuchMethodError e) {
            throw new RuntimeException("ErpNextClient.getSuppliers() method is not available. Please ensure ErpNextClient.java is updated.", e);
        }
    }
}