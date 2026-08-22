package com.example.loginapp.service;

import com.example.loginapp.model.Product;
import com.example.loginapp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;


    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderBySortOrderAsc();
    }
}
