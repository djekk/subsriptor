package com.noi.subscriptorapp.repository;

import com.noi.subscriptorapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderBySortOrderAsc();
    List<Product> findAllByOrderBySortOrderAsc();
    Optional<Product> findByCode(String code);
    boolean existsByCode(String code);
}
