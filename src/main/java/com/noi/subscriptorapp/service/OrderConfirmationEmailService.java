package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.model.User;
import com.noi.subscriptorapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class OrderConfirmationEmailService {
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${order.confirmation-email.enabled:false}")
    private boolean confirmationEmailEnabled;

    @Value("${order.confirmation-email.from:no-reply@subscriptor.local}")
    private String fromEmail;

    public OrderConfirmationEmailService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    public void sendOrderConfirmation(Order order) {
        if (!confirmationEmailEnabled) {
            return;
        }

        if (order == null || order.getUserId() == null) {
            throw new IllegalStateException("Cannot send order confirmation email without user identity");
        }

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for order " + order.getOrderNumber()));
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalStateException("User email is missing for order " + order.getOrderNumber());
        }

        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(resolveGreetingName(user, order)).append(",\n\n");
        body.append("Your payment was successful. Here are your order details:\n\n");
        body.append("Order Number: ").append(valueOrDash(order.getOrderNumber())).append('\n');
        body.append("Product: ").append(valueOrDash(order.getProductName())).append('\n');
        body.append("Variant: ").append(valueOrDash(order.getProductVariant())).append('\n');
        body.append("Quantity: ").append(order.getQuantity() == null ? "-" : order.getQuantity()).append('\n');
        body.append("Unit Price: $").append(order.getUnitPrice() == null ? "-" : order.getUnitPrice()).append('\n');
        body.append("Total Amount: $").append(order.getTotalAmount() == null ? "-" : order.getTotalAmount()).append('\n');
        body.append("Device Number: ").append(valueOrDash(order.getDeviceNumber())).append('\n');
        body.append("Payment Provider: ").append(valueOrDash(order.getPaymentProvider())).append('\n');
        body.append("Status: ").append(valueOrDash(order.getStatus())).append('\n');
        body.append("Date: ").append(order.getCreatedAt() == null
                ? "-"
                : order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append('\n');
        body.append("\nThank you for your order.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email.trim());
        message.setSubject("Order Confirmation " + valueOrDash(order.getOrderNumber()));
        message.setText(body.toString());
        mailSender.send(message);
    }

    private String resolveGreetingName(User user, Order order) {
        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
            return user.getFirstName().trim();
        }
        if (order.getUsername() != null && !order.getUsername().trim().isEmpty()) {
            return order.getUsername().trim();
        }
        return "Customer";
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }
}
