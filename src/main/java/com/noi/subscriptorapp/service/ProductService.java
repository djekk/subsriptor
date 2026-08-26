package com.noi.subscriptorapp.service;

import com.noi.subscriptorapp.model.Product;
import com.noi.subscriptorapp.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderBySortOrderAsc();
    }

    public Product createProduct(Product product) {
        validateProduct(product);

        if (productRepository.existsByCode(product.getCode())) {
            throw new IllegalArgumentException("A product with this code already exists.");
        }

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        validateProduct(product);

        String requestedCode = product.getCode().trim();
        if (!existing.getCode().equals(requestedCode) && productRepository.existsByCode(requestedCode)) {
            throw new IllegalArgumentException("A product with this code already exists.");
        }

        existing.setCode(requestedCode);
        existing.setName(product.getName().trim());
        existing.setProductType(product.getProductType().trim());
        existing.setVariant(product.getVariant() == null ? null : product.getVariant().trim());
        existing.setDescription(product.getDescription() == null ? null : product.getDescription().trim());
        existing.setPrice(product.getPrice());
        existing.setSortOrder(product.getSortOrder() == null ? 0 : product.getSortOrder());
        existing.setActive(product.getActive() == null || product.getActive());

        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        productRepository.delete(existing);
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product payload is required.");
        }
        if (product.getCode() == null || product.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Product code is required.");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (product.getProductType() == null || product.getProductType().trim().isEmpty()) {
            throw new IllegalArgumentException("Product type is required.");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price is required and must not be negative.");
        }

        product.setCode(product.getCode().trim());
        product.setName(product.getName().trim());
        product.setProductType(product.getProductType().trim());
        product.setVariant(product.getVariant() == null ? null : product.getVariant().trim());
        product.setDescription(product.getDescription() == null ? null : product.getDescription().trim());
        product.setSortOrder(product.getSortOrder() == null ? 0 : product.getSortOrder());
        product.setActive(product.getActive() == null || product.getActive());
    }
}
