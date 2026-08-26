package com.noi.subscriptorapp.controller;

import com.noi.subscriptorapp.dto.ApiResponse;
import com.noi.subscriptorapp.model.Order;
import com.noi.subscriptorapp.model.Product;
import com.noi.subscriptorapp.service.OrderService;
import com.noi.subscriptorapp.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final OrderService orderService;
    private final ProductService productService;

    public AdminController(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse> getOrders(HttpSession session) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        List<Order> orders = orderService.findAllOrdersByCreatedAtDesc();
        return ResponseEntity.ok(new ApiResponse(true, "Orders loaded", orders));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse> getProducts(HttpSession session) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(new ApiResponse(true, "Products loaded", products));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse> createProduct(@RequestBody Product product, HttpSession session) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        try {
            Product createdProduct = productService.createProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Product created", createdProduct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable Long id,
                                                    @RequestBody Product product,
                                                    HttpSession session) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        try {
            Product updatedProduct = productService.updateProduct(id, product);
            return ResponseEntity.ok(new ApiResponse(true, "Product updated", updatedProduct));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id, HttpSession session) {
        ResponseEntity<ApiResponse> adminCheck = requireAdmin(session);
        if (adminCheck != null) {
            return adminCheck;
        }

        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(new ApiResponse(true, "Product deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse> requireAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Please login"));
        }
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "Administrator access required"));
        }

        return null;
    }
}
