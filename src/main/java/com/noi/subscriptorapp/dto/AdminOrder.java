package com.noi.subscriptorapp.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.noi.subscriptorapp.model.Order;

public class AdminOrder {
    private final Order order;
    private final String email;

    public AdminOrder(Order order, String email) {
        this.order = order;
        this.email = email;
    }

    @JsonUnwrapped
    public Order getOrder() {
        return order;
    }

    public String getEmail() {
        return email;
    }
}
