# OrderPulse ⚡ Master Interview & Architecture Cheatsheet

A rapid-reference, high-density study sheet and technical guide for **OrderPulse** (Spring Boot 3.3 + Kotlin 1.9+). Keep this open during interview prep, code reviews, and technical discussions.

---

## 📑 Quick Navigation
1. [The 60-Second Interview Elevator Pitch](#1-the-60-second-interview-elevator-pitch)
2. [Architecture & Component Reference Matrix](#2-architecture--component-reference-matrix)
3. [Complete REST API Reference Matrix](#3-complete-rest-api-reference-matrix)
4. [Database Schema & Entity Relationship Map](#4-database-schema--entity-relationship-map)
5. [Top 10 Kotlin & Spring Boot Interview Gotchas (Trap Cards)](#5-top-10-kotlin--spring-boot-interview-gotchas-trap-cards)
6. [Design Patterns Implemented](#6-design-patterns-implemented)
7. [application.yml Configuration Matrix](#7-applicationyml-configuration-matrix)
8. [Testing Strategy & MockK Cheat Sheet](#8-testing-strategy--mockk-cheat-sheet)
9. [Ready-to-Run cURL Test Script](#9-ready-to-run-curl-test-script)

---

## 1. The 60-Second Interview Elevator Pitch

When the interviewer asks: **"Walk me through a recent backend project you built or are proud of"**, use one of these calibrated scripts:

### Standard Script (Mid / Senior Level)
> *"I built **OrderPulse**, a high-concurrency order processing and payment orchestration engine powered by **Spring Boot 3.3** and **Kotlin 1.9+**.*
> 
> *The core challenge I addressed was **preventing inventory overselling during high-traffic flash sales without blocking database connections**. I implemented **Optimistic Locking (`@Version`)** on product entities paired with **Caffeine in-memory caching** using Window TinyLFU eviction, and translated concurrency collisions into standardized **RFC 7807 Problem Details** with distributed trace IDs.*
> 
> *Architecturally, it follows Clean Layered Architecture: payment gateways use the **Strategy Pattern** dynamically resolved through Spring DI, transactional audit logs are guaranteed via **`Propagation.REQUIRES_NEW`**, and the **N+1 query problem** is resolved using JPA **`@EntityGraph`**.*
> 
> *For security and testing, it uses stateless **Spring Security 6 with JWT**, and the test suite leverages **MockK** and Spring slice tests (`@DataJpaTest`, `@WebMvcTest`) for fast, isolated verification."*

---

## 2. Architecture & Component Reference Matrix

| Layer / File | Primary Annotations & Interfaces | Responsibility & Interview Talking Point |
| :--- | :--- | :--- |
| [`BaseEntity.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/BaseEntity.kt) | `@MappedSuperclass`, `@EntityListeners` | Base entity for ID & auditing. Explains why JPA entities must **never** be Kotlin `data class`es. |
| [`Exceptions.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/Exceptions.kt) | `sealed class AppException` | Type-safe custom domain exceptions mapped to HTTP statuses. |
| [`GlobalExceptionHandler.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/GlobalExceptionHandler.kt) | `@RestControllerAdvice`, `ResponseEntityExceptionHandler` | RFC 7807 `ProblemDetail` standard error response with MDC trace ID injection. |
| [`MdcLoggingFilter.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/filter/MdcLoggingFilter.kt) | `@Component`, `OncePerRequestFilter`, `@Order` | Generates/propagates `X-Trace-Id` across requests and logs via SLF4J MDC. |
| [`SecurityConfig.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/config/SecurityConfig.kt) | `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity` | Spring Security 6 component-based `SecurityFilterChain` with stateless JWT. |
| [`CacheConfig.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/config/CacheConfig.kt) | `@Configuration`, `@EnableCaching` | Caffeine CacheManager with 500 max items and 10-minute expiry. |
| [`OpenApiConfig.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/config/OpenApiConfig.kt) | `@Configuration`, `OpenAPI` | OpenAPI 3.0 / Swagger UI definition with Bearer JWT scheme. |
| [`JpaAuditingConfig.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/config/JpaAuditingConfig.kt) | `@Configuration`, `@EnableJpaAuditing` | Automated audit timestamps and current authenticated username resolution. |
| [`JwtTokenProvider.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/security/JwtTokenProvider.kt) | `@Component` | JJWT 0.12.x HMAC-SHA256 token generation, signing, claims parsing, validation. |
| [`UserPrincipal.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/security/UserPrincipal.kt) | `UserDetails` | Spring Security user adapter decoupling domain `User` from security context. |
| [`Product.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/product/Product.kt) | `@Entity`, `@Table`, `@Version` | Catalog item with **Optimistic Locking** (`version`) and domain invariants. |
| [`ProductRepository.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/product/ProductRepository.kt) | `JpaRepository<Product, Long>` | Paginated case-insensitive catalog search queries. |
| [`ProductService.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/product/ProductService.kt) | `@Service`, `@Transactional`, `@Cacheable`, `@CacheEvict` | Product business logic with automatic cache eviction on writes. |
| [`Order.kt` & `OrderItem.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/order/Order.kt) | `@Entity`, `@OneToMany`, `@ManyToOne` | Bidirectional relationship management, helper synchronization methods. |
| [`OrderRepository.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/order/OrderRepository.kt) | `JpaRepository`, `@EntityGraph` | Solves the **N+1 query problem** by eagerly joining items, products, and users. |
| [`OrderService.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/order/OrderService.kt) | `@Service`, `@Transactional` | Coordinates stock deduction, payment strategy execution, and metric updates. |
| [`PaymentStrategy.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentStrategy.kt) | `interface PaymentStrategy` | Strategy pattern interface for payment gateways. |
| [`PaymentStrategyFactory.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentStrategyFactory.kt) | `@Component` | Dynamic strategy resolution adhering to Open-Closed Principle (OCP). |
| [`PaymentService.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentService.kt) | `@Service`, `@Transactional(propagation = REQUIRES_NEW)` | Demonstrates isolated transactions for financial audit logs. |
| [`OrderMetrics.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/metrics/OrderMetrics.kt) | `@Component`, `MeterRegistry` | Custom business counters and gauges exposed via Actuator & Prometheus. |

---

## 3. Complete REST API Reference Matrix

Base URL: `http://localhost:8080`

| Method | Endpoint | Auth | Request Body / Params | Response Codes | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | None | `{email, password, fullName, role?}` | `201 Created`, `400 Bad Request`, `409 Conflict` | Registers customer or admin account; returns JWT. |
| `POST` | `/api/v1/auth/login` | None | `{email, password}` | `200 OK`, `401 Unauthorized` | Authenticates credentials; returns Bearer JWT. |
| `GET` | `/api/v1/auth/me` | Bearer | None | `200 OK`, `401 Unauthorized` | Returns current authenticated user profile. |
| `GET` | `/api/v1/products` | None | Query: `search?`, `page?`, `size?`, `sort?` | `200 OK` | Paginated product search (Public). |
| `GET` | `/api/v1/products/{id}` | None | Path: `id` | `200 OK`, `404 Not Found` | Single product details (Caffeine cached). |
| `POST` | `/api/v1/products` | `ADMIN` | `{name, description?, price, stockQuantity}` | `201 Created`, `400 Bad Request`, `403 Forbidden` | Creates product and evicts cache. |
| `PUT` | `/api/v1/products/{id}` | `ADMIN` | `{name, description?, price, stockQuantity}` | `200 OK`, `404 Not Found`, `403 Forbidden` | Updates product and evicts cache. |
| `DELETE`| `/api/v1/products/{id}` | `ADMIN` | Path: `id` | `204 No Content`, `404 Not Found`, `403 Forbidden` | Deletes product and evicts cache. |
| `POST` | `/api/v1/orders` | Bearer | `{items: [{productId, quantity}], paymentMethod}` | `201 Created`, `400 Bad Request`, `402 Payment Required`, `409 Conflict` | Places order with stock deduction & payment strategy. |
| `GET` | `/api/v1/orders/my-orders` | Bearer | Query: `page?`, `size?`, `sort?` | `200 OK`, `401 Unauthorized` | Paginated orders of authenticated user. |
| `GET` | `/api/v1/orders/{id}` | Bearer | Path: `id` | `200 OK`, `403 Forbidden`, `404 Not Found` | Order details (Owner or Admin). |
| `GET` | `/api/v1/orders` | `ADMIN` | Query: `page?`, `size?` | `200 OK`, `403 Forbidden` | All system orders paginated. |
| `PATCH`| `/api/v1/orders/{id}/status` | `ADMIN` | Query: `status` (`PAID`, `SHIPPED`, `CANCELLED`) | `200 OK`, `404 Not Found`, `403 Forbidden` | Updates order fulfillment status. |
| `GET` | `/actuator/health` | None | None | `200 OK` | Detailed application & DB health check. |
| `GET` | `/actuator/metrics/{metric}` | None | Path: `orderpulse.orders.placed.total` | `200 OK` | Custom business metrics. |
| `GET` | `/swagger-ui.html` | None | None | `200 OK` (HTML) | Interactive Swagger UI documentation. |
| `GET` | `/h2-console` | None | None | `200 OK` (HTML) | Embedded database web console. |

---

## 4. Database Schema & Entity Relationship Map

```
   +--------------------+             +----------------------+
   |       users        | 1         * |        orders        |
   +--------------------+-------------+----------------------+
   | id (PK, BIGINT)    |             | id (PK, BIGINT)      |
   | email (UQ, VARCHAR)|             | order_number (UQ)    |
   | password (VARCHAR) |             | user_id (FK -> users)|
   | full_name (VARCHAR)|             | total_amount (DEC)   |
   | role (VARCHAR)     |             | status (VARCHAR)     |
   | created_at (TS)    |             | payment_method (VAR) |
   | updated_at (TS)    |             | created_at (TS)      |
   +--------------------+             +----------------------+
                                                 | 1
                                                 |
                                                 | *
                                      +----------------------+
                                      |     order_items      |
                                      +----------------------+
                                      | id (PK, BIGINT)      |
                                      | order_id (FK->orders)|
                                      | product_id (FK)      |
                                      | quantity (INT)       |
                                      | price_at_purchase    |
                                      +----------------------+
                                                 | *
                                                 |
                                                 | 1
                                      +----------------------+
                                      |       products       |
                                      +----------------------+
                                      | id (PK, BIGINT)      |
                                      | name (VARCHAR, IDX)  |
                                      | description (TEXT)   |
                                      | price (DECIMAL)      |
                                      | stock_quantity (INT) |
                                      | version (BIGINT, OPL)|
                                      | created_at (TS)      |
                                      | updated_at (TS)      |
                                      +----------------------+
```

### Migration History (Flyway)
- **`V1__init_schema.sql`**: Initial DDL creating `users`, `products`, `orders`, `order_items`, `payment_transactions`, and foreign keys/indexes.
- **`V2__seed_data.sql`**: Initial seed data including Admin user (`admin@orderpulse.com`), Customer user (`john.doe@example.com`), and 5 sample catalog products.

---

## 5. Top 10 Kotlin & Spring Boot Interview Gotchas (Trap Cards)

### 🪤 Gotcha 1: Kotlin `data class` with JPA Entities
* **Question**: *"Can we use Kotlin data class for Hibernate entities?"*
* **Trap Answer**: *"Yes, it gives us free equals, hashCode, and toString."*
* **Correct Answer**: **NO, never.**
  1. `data class` includes all constructor fields in `hashCode()`. Before saving, `@Id` is null; after saving, it has a value. This breaks `HashSet` contracts.
  2. `toString()` triggers lazy relations (`order.items`) causing `LazyInitializationException` or `StackOverflowError` on bidirectional relations.
  3. `copy()` duplicates the entity with the same ID, causing Hibernate session identity collisions.
* **OrderPulse Solution**: Regular `open class Product : BaseEntity()`, with `equals()` based solely on non-null ID identity.

### 🪤 Gotcha 2: Bean Validation with Kotlin Constructor Properties
* **Question**: *"Why didn't `@NotBlank` work on my Kotlin data class DTO?"*
* **Correct Answer**: By default, an annotation on a constructor property attaches to the **constructor parameter**, not the generated private field or getter. Use the `@field:` use-site target:
  ```kotlin
  data class RegisterRequest(@field:NotBlank val email: String)
  ```

### 🪤 Gotcha 3: The N+1 Query Problem
* **Question**: *"How did you solve the N+1 problem in your Order retrieval?"*
* **Correct Answer**: In [`OrderRepository.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/order/OrderRepository.kt), using `@EntityGraph(attributePaths = ["items", "items.product", "user"])`. This tells Hibernate to perform a single SQL `LEFT JOIN` without changing the global `FetchType.LAZY` definition.

### 🪤 Gotcha 4: Concurrency & Flash Sales (Optimistic vs Pessimistic)
* **Question**: *"How do you prevent two users from purchasing the last item at the exact same moment?"*
* **Correct Answer**:
  - **Optimistic Locking (`@Version`)**: The `Product` table has a `version` column. Hibernate checks `WHERE id = ? AND version = ?`. If version changed, `OptimisticLockingFailureException` is thrown. Best for read-heavy systems with occasional write conflicts.
  - **Pessimistic Locking (`PESSIMISTIC_WRITE`)**: Generates `SELECT ... FOR UPDATE`, holding a DB row lock. Best for ultra-high contention where retry overhead is unacceptable.

### 🪤 Gotcha 5: Self-Invocation Proxy Trap
* **Question**: *"Why doesn't `@Transactional` or `@Cacheable` work when method A calls method B in the same class?"*
* **Correct Answer**: Spring AOP uses **CGLIB dynamic proxies**. The proxy intercepts calls coming from *external* beans. Inside the class, `this.methodB()` invokes the method directly on the target object, completely bypassing the proxy interceptor! Fix: Move method B to a separate Spring `@Service`.

### 🪤 Gotcha 6: Transaction Propagation (`REQUIRED` vs `REQUIRES_NEW`)
* **Question**: *"When should you use `Propagation.REQUIRES_NEW`?"*
* **Correct Answer**: When an inner operation **must commit even if the outer transaction rolls back**. In OrderPulse, [`PaymentService.recordPaymentAttempt()`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentService.kt) uses `REQUIRES_NEW` so failed payment attempts are permanently logged for financial reconciliation even when the order rolls back.

### 🪤 Gotcha 7: Open Session In View (OSIV) Anti-Pattern
* **Question**: *"What is OSIV and should you leave it enabled?"*
* **Correct Answer**: OSIV keeps the database connection and Hibernate Session open during the entire view/controller rendering phase. It masks lazy loading issues but holds database connections idle during slow HTTP transfers, quickly exhausting HikariCP connection pools.
* **OrderPulse Solution**: Explicitly disabled via `spring.jpa.open-in-view: false` in `application.yml`.

### 🪤 Gotcha 8: MockK vs Mockito in Kotlin
* **Question**: *"Why did you choose MockK over Mockito?"*
* **Correct Answer**:
  1. Kotlin classes and methods are `final` by default. Mockito fails on final classes without the `mock-maker-inline` agent.
  2. MockK natively supports Kotlin coroutines (`coEvery`, `coVerify`).
  3. MockK handles default arguments, extension functions, and sealed classes effortlessly.

### 🪤 Gotcha 9: Spring Security 6 Changes
* **Question**: *"What major architectural changes occurred in Spring Security 6?"*
* **Correct Answer**:
  - `WebSecurityConfigurerAdapter` was completely removed; replaced by `@Bean fun filterChain(http: HttpSecurity): SecurityFilterChain`.
  - `antMatchers()` replaced by `requestMatchers()`.
  - `@EnableGlobalMethodSecurity` replaced by `@EnableMethodSecurity`.
  - Strict lambda DSL requirement for configuration blocks.

### 🪤 Gotcha 10: In-Memory (Caffeine) vs Distributed (Redis) Caching
* **Question**: *"Why Caffeine over Redis in this setup?"*
* **Correct Answer**: Caffeine runs in-process in JVM heap, delivering **sub-microsecond** read latencies with zero serialization or network overhead. Redis is distributed (1-5ms network round-trip), required when multiple application replicas must share a synchronized cache.

---

## 6. Design Patterns Implemented

```mermaid
graph LR
    subgraph Strategy Pattern
        OrderService -->|uses| PaymentStrategyFactory
        PaymentStrategyFactory -->|resolves| PaymentStrategy
        PaymentStrategy <|.. CreditCardPaymentStrategy
        PaymentStrategy <|.. PayPalPaymentStrategy
    end

    subgraph Factory Pattern
        PaymentStrategyFactory -.->|auto-wired list| SpringContext[Spring ApplicationContext]
    end

    subgraph Extension Mapper Pattern
        Product -->|Product.toResponse| ProductResponse
        User -->|User.toSummaryResponse| UserSummaryResponse
        Order -->|Order.toResponse| OrderResponse
    end
```

1. **Strategy Pattern**: Decouples payment gateway integration (`PaymentStrategy`).
2. **Factory Pattern**: [`PaymentStrategyFactory`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentStrategyFactory.kt) collects all Spring beans implementing `PaymentStrategy` using constructor injection.
3. **Result Pattern / Sealed Interface**: [`PaymentResult`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentResult.kt) eliminates runtime `else` branches and avoids exception-driven control flow.
4. **Extension Function Mappers**: Clean, compile-time type-safe domain-to-DTO conversion without reflection overhead.

---

## 7. application.yml Configuration Matrix

| Configuration Key | Value in OrderPulse | Production & Interview Rationale |
| :--- | :--- | :--- |
| `spring.jpa.open-in-view` | `false` | **Critical**: Prevents DB connection pool starvation and forces explicit transactional boundaries. |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Production standard. Flyway manages DDL; Hibernate only validates entity mapping consistency. |
| `spring.flyway.enabled` | `true` | Version-controlled, reproducible database migrations. |
| `spring.cache.type` | `caffeine` | High-performance Window TinyLFU caching with near-optimal hit ratios. |
| `app.jwt.secret` | *(256-bit hex)* | HMAC-SHA256 requirement in JJWT 0.12+ (minimum 32 bytes). |
| `app.jwt.expiration-ms` | `86400000` | 24-hour expiration token duration. |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | Exposes Actuator metrics for monitoring systems (Prometheus/Grafana). |
| `management.endpoint.health.show-details` | `always` | Exposes component-level health (DB, Disk, Liveness). |

---

## 8. Testing Strategy & MockK Cheat Sheet

### Testing Pyramid in OrderPulse

```
        / \
       /   \        OrderPulseIntegrationTest (@SpringBootTest)
      /     \       - Full HTTP flow with real DB & Security
     /-------\
    /         \     ProductRepositoryTest (@DataJpaTest) + AuthControllerTest (@WebMvcTest)
   /           \    - Slice tests isolating persistence & web layers
  /-------------\
 /               \  OrderServiceTest (MockK Unit Tests)
/_________________\ - Sub-millisecond execution, mocks all collaborators
```

### MockK Syntax Quick Reference

```kotlin
// 1. Declare mocks
val orderRepository: OrderRepository = mockk()
val metrics: OrderMetrics = mockk(relaxed = true) // relaxed = auto-stub unit/void methods

// 2. Stubbing returns
every { orderRepository.save(any()) } returns savedOrder
every { userRepository.findById(1L) } returns Optional.of(user)

// 3. Stubbing dynamic answers
every { orderRepository.save(any()) } answers { firstArg<Order>().apply { id = 100L } }

// 4. Stubbing exceptions
every { productService.getProductEntity(any()) } throws InsufficientStockException("Out of stock")

// 5. Verifying invocations
verify(exactly = 1) { metrics.incrementOrdersPlaced() }
verify(exactly = 0) { paymentService.processPayment(any(), any(), any(), any()) }
```

---

## 9. Ready-to-Run cURL Test Script

Run these commands in PowerShell or Bash while the server is running on `http://localhost:8080`:

### Step 1: Login as Demo Customer (Pre-seeded via Flyway)
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"password123"}'
```
*(Copy the `accessToken` from the output)*

### Step 2: Browse Catalog (Caffeine Caching Demo)
```bash
# First call: Cache miss (hits database)
curl -s -X GET http://localhost:8080/api/v1/products/1

# Second call: Cache hit (served from JVM heap in sub-milliseconds)
curl -s -X GET http://localhost:8080/api/v1/products/1
```

### Step 3: Place an Order (Deducts Stock & Executes Payment Strategy)
```bash
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": 1,
        "quantity": 1
      }
    ],
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Step 4: Verify Custom Actuator Counter Incremented
```bash
curl -s -X GET http://localhost:8080/actuator/metrics/orderpulse.orders.placed.total
```

### Step 5: Test Concurrency Conflict (Optimistic Locking)
Try placing an order requesting more than the available stock:
```bash
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": 5,
        "quantity": 999
      }
    ],
    "paymentMethod": "CREDIT_CARD"
  }'
```
*Expected Response: HTTP 400 Bad Request formatted as RFC 7807 `ProblemDetail` with `X-Trace-Id`.*
