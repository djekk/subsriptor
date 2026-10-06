package com.noi.subscriptorapp.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.noi.subscriptorapp.model.Order;

public class AdminOrder {
    private final Order order;
    private final String firstName;
    private final String lastName;
    private final String email;

    public AdminOrder(Order order, String firstName, String lastName, String email) {
        this.order = order;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    @JsonUnwrapped
    public Order getOrder() {
        return order;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
