package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.Product;
import com.noi.subscriptorapp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;


    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderBySortOrderAsc();
    }
}
