package com.example.loginapp.repository;

import com.example.loginapp.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByStripeSessionId(String stripeSessionId);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByDeviceNumberAndStatusAndProductTypeOrderByCreatedAtDesc(
            String deviceNumber, String status, String productType);
}
