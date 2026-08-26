package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.dto.OrderSummary;
import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.model.Product;
import com.noi.subscriptorapp.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        order.setExpiresAt(calculateExpiresAt(order));
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

    public List<Order> findAllOrdersByCreatedAtDesc() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public boolean userOwnsDevice(Long userId, String deviceNumber) {
        return orderRepository.existsByUserIdAndDeviceNumber(userId, deviceNumber);
    }

    public List<Order> findPaidSubscriptionsByDevice(String deviceNumber) {
        return orderRepository.findByDeviceNumberAndStatusAndProductTypeOrderByCreatedAtDesc(
                deviceNumber, "PAID", "SUBSCRIPTION"
        );
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

    private LocalDateTime calculateExpiresAt(Order order) {
        if (order == null || order.getCreatedAt() == null || !"SUBSCRIPTION".equalsIgnoreCase(order.getProductType())) {
            return null;
        }

        int months = resolveSubscriptionMonths(order.getProductCode());
        if (months <= 0) {
            return null;
        }

        return order.getCreatedAt().plusMonths(months);
    }

    static int resolveSubscriptionMonths(String productCode) {
        if (productCode == null || productCode.trim().isEmpty()) {
            return 0;
        }

        Matcher matcher = Pattern.compile("(?i)^SUBSCRIPTION_(\\d+)([MY])$").matcher(productCode.trim());
        if (!matcher.matches()) {
            return 0;
        }

        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();
        return "Y".equals(unit) ? amount * 12 : amount;
    }
}
