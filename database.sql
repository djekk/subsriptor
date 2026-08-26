-- Create the database
CREATE DATABASE IF NOT EXISTS login_db;
USE login_db;

-- Create the users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create the products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    product_type VARCHAR(30) NOT NULL,
    variant VARCHAR(100),
    description VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_products_code (code),
    INDEX idx_products_type (product_type),
    INDEX idx_products_active_sort (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create the orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    username VARCHAR(100),
    device_number VARCHAR(100),
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_variant VARCHAR(100),
    product_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_provider VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    stripe_session_id VARCHAR(255),
    stripe_payment_status VARCHAR(50),
    failure_reason VARCHAR(255),
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_number (order_number),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_stripe_session (stripe_session_id),
    INDEX idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO products (code, name, product_type, variant, description, price, sort_order, is_active)
VALUES
    ('SUBSCRIPTION_6M', 'Subscription', 'SUBSCRIPTION', 'Half of Year', 'Access for 6 months', 29.99, 1, 1),
    ('SUBSCRIPTION_1Y', 'Subscription', 'SUBSCRIPTION', 'One Year', 'Access for 12 months', 49.99, 2, 1),
--    ('TOKEN', 'Token', 'TOKEN', 'Single', 'One-time token package for quick access', 4.99, 3, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    product_type = VALUES(product_type),
    variant = VALUES(variant),
    description = VALUES(description),
    price = VALUES(price),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active);

-- Create a users_audit table (optional, for tracking login history)
CREATE TABLE IF NOT EXISTS login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create an index on user_id for faster queries
CREATE INDEX idx_login_history_user_id ON login_history(user_id);
