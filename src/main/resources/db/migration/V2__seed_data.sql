-- V2: Initial Seed Data for Demo & Interview Showcase
-- Passwords are encrypted with BCrypt (password: "password123")
-- Hash: $2a$10$mRz7N/lWzZk0g1n3D7PZ..VpT37X6g9nK1w0pB7b0w4lT2gK5aJ5K

INSERT INTO users (email, password, full_name, role, created_at, updated_at)
VALUES 
('admin@orderpulse.com', '$2a$10$4Gz3k1pT9lQ2mN8vX0wZ1.kP38Y7g5oJ0x1qA6a2v3mT1gJ4bK4J6', 'System Administrator', 'ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('john.doe@example.com', '$2a$10$4Gz3k1pT9lQ2mN8vX0wZ1.kP38Y7g5oJ0x1qA6a2v3mT1gJ4bK4J6', 'John Doe', 'ROLE_CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (name, description, price, stock_quantity, version, created_at, updated_at)
VALUES
('MacBook Pro 16-inch', 'Apple M3 Max, 36GB RAM, 1TB SSD', 3499.0000, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sony WH-1000XM5', 'Wireless Noise Cancelling Headphones', 399.9900, 50, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Keychron Q1 Pro', 'Wireless Custom Mechanical Keyboard (Barebone/Red Switch)', 199.0000, 30, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dell UltraSharp 27 4K', 'USB-C Hub Monitor (U2723QE)', 579.5000, 20, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Limited Edition Flash Item', 'High-concurrency test product for flash sale race conditions', 49.9900, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
