package com.example.loginapp.service;

import com.example.loginapp.dto.OrderSummary;
import com.example.loginapp.model.Order;
import com.example.loginapp.model.Product;
import com.example.loginapp.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createPendingOrder(Product product, int quantity, String provider, Long userId, String username, String deviceNumber) {
        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUserId(userId);
        order.setUsername(username);
        order.setDeviceNumber(deviceNumber);
        order.setProductCode(product.getCode());
        order.setProductName(product.getName());
        order.setProductVariant(product.getVariant());
        order.setProductType(product.getProductType());
        order.setQuantity(quantity);
        order.setUnitPrice(product.getPrice());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setPaymentProvider(provider);
        order.setStatus("PENDING");
        return orderRepository.save(order);
    }

    public Order attachStripeSession(String orderNumber, String stripeSessionId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStripeSessionId(stripeSessionId);
        return orderRepository.save(order);
    }

    public Order markPaidByStripeSession(String stripeSessionId, String stripePaymentStatus) {
        Order order = orderRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for session"));
        order.setStatus("PAID");
        order.setStripePaymentStatus(stripePaymentStatus);
        order.setFailureReason(null);
        return orderRepository.save(order);
    }

    public Order markFailed(String orderNumber, String reason) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus("FAILED");
        order.setFailureReason(reason);
        return orderRepository.save(order);
    }

    public Order markCancelled(String orderNumber, String reason) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus("CANCELLED");
        order.setFailureReason(reason);
        return orderRepository.save(order);
    }

    public Optional<Order> findByStripeSessionId(String stripeSessionId) {
        return orderRepository.findByStripeSessionId(stripeSessionId);
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public OrderSummary toSummary(Order order) {
        return new OrderSummary(
                order.getOrderNumber(),
                order.getProductCode(),
                order.getStatus(),
                order.getStripeSessionId(),
                order.getPaymentProvider()
        );
    }
}
