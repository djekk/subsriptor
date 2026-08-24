package com.noi.subscriptorapp.repository;

import com.noi.subscriptorapp.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByStripeSessionId(String stripeSessionId);
    List<Order> findAllByOrderByCreatedAtDesc();
    boolean existsByUserIdAndDeviceNumber(Long userId, String deviceNumber);
    List<Order> findByDeviceNumberAndStatusAndProductTypeOrderByCreatedAtDesc(
            String deviceNumber, String status, String productType);
}
