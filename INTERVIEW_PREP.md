# Master Spring Boot 3 + Kotlin Backend Interview Guide

> **Project**: OrderPulse  
> **Target Roles**: Junior, Mid, Senior, and Lead Backend / Kotlin Engineers  
> **Tech Stack**: Spring Boot 3.3, Kotlin 1.9+, Spring Data JPA, Spring Security 6, Flyway, Caffeine Cache, Actuator, MockK

---

## Table of Contents
1. [Kotlin Idioms & Language Mechanics](#1-kotlin-idioms--language-mechanics)
2. [Spring Boot Core, IoC & Proxy Mechanics](#2-spring-boot-core-ioc--proxy-mechanics)
3. [JPA, Hibernate & Concurrency (The Most Common Trap Questions)](#3-jpa-hibernate--concurrency)
4. [Spring Security 6 & Stateless Auth](#4-spring-security-6--stateless-auth)
5. [Caching, Performance & Observability](#5-caching-performance--observability)
6. [Design Patterns in Practice](#6-design-patterns-in-practice)
7. [Testing Strategy (MockK vs Mockito)](#7-testing-strategy)
8. [Quick-Fire Technical Questions Checklist](#8-quick-fire-technical-questions-checklist)

---

## 1. Kotlin Idioms & Language Mechanics

### Q1. Why should JPA entities NEVER be declared as Kotlin `data class`es?
**Answer:**
This is one of the most famous Kotlin + Spring interview questions. Declaring JPA entities as `data class` leads to subtle, high-impact production bugs:

1. **Broken `equals()` and `hashCode()` contracts**:
   - A `data class` automatically generates `equals()` and `hashCode()` using all primary constructor fields.
   - For a newly instantiated JPA entity, the `@Id` is `null`. When `entityManager.persist()` is called, Hibernate assigns a generated ID. If the entity was added to a `HashSet` or `HashMap` before persistence, its hash code changes, making it impossible to retrieve from the set.
   - Comparing all fields triggers lazy-loading proxies across relational fields (e.g. `order.items`), throwing `LazyInitializationException` outside a transaction or creating catastrophic **N+1 SQL queries**.
2. **Infinite Recursion in `toString()`**:
   - A `data class` generates `toString()` containing all properties. In bidirectional relationships (`Order` -> `OrderItem` -> `Order`), printing the entity triggers an immediate `StackOverflowError`.
3. **Unexpected Behavior of `copy()`**:
   - The generated `copy()` creates a shallow clone sharing the same `@Id`. Having two instances with identical IDs in the same Hibernate Persistence Context creates session conflicts and dirty-checking anomalies.
4. **Final Classes by Default**:
   - Kotlin classes and methods are `final` by default. Hibernate requires classes to be `open` to generate runtime CGLIB lazy-loading proxies.

*See in OrderPulse:* [`BaseEntity.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/BaseEntity.kt) and [`Product.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/product/Product.kt). Entities use `open class` and define `equals`/`hashCode` based strictly on ID identity.

---

### Q2. Why is the `@field:` use-site target required for Bean Validation in Kotlin?
**Answer:**
In Kotlin, a property defined in a constructor like:
```kotlin
data class RegisterRequest(@NotBlank val email: String)
```
actually compiles down to three bytecode elements:
1. A constructor parameter (`email`)
2. A private field (`private final String email`)
3. A getter method (`public final String getEmail()`)

By default, an annotation placed on the property applies to the constructor parameter. However, Hibernate Validator / Jakarta Bean Validation scans **fields** or **getters**. 
Using `@field:NotBlank` explicitly targets the underlying private field:
```kotlin
data class RegisterRequest(@field:NotBlank val email: String)
```
Without `@field:`, validation may silently fail to execute when Jackson deserializes the JSON request body.

*See in OrderPulse:* [`AuthDtos.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/auth/AuthDtos.kt) and [`OrderDtos.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/order/OrderDtos.kt).

---

### Q3. What are Kotlin Sealed Interfaces and when do you choose them over Enums?
**Answer:**
- **Enums**: Represent a fixed set of constant values with the exact same fields and structure across all entries (e.g. `Role.ROLE_ADMIN`, `Role.ROLE_CUSTOMER`).
- **Sealed Classes / Interfaces**: Represent restricted class hierarchies where each subclass can have **different state and properties**, while maintaining compile-time exhaustiveness in `when` expressions.

*Example in OrderPulse:* [`PaymentResult.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentResult.kt)
```kotlin
sealed interface PaymentResult {
    data class Success(val transactionReference: String) : PaymentResult
    data class Failure(val reason: String) : PaymentResult
}
```
In `OrderService.kt`, pattern matching on `PaymentResult` requires no `else` branch. If a new result subtype is added in the future, the compiler forces every caller to handle it.

---

### Q4. What do the `kotlin-spring` and `kotlin-jpa` compiler plugins do?
**Answer:**
1. **`kotlin-spring` (`all-open`)**:
   - Automatically opens (removes `final`) classes and methods annotated with Spring stereotypes: `@Component`, `@Service`, `@Repository`, `@Controller`, `@Configuration`, and `@Transactional`.
   - Without this, Spring CGLIB proxies cannot subclass your beans, causing proxy-based features (transactions, caching, security) to fail or behave erratically.
2. **`kotlin-jpa` (`no-arg`)**:
   - Automatically generates a synthetic zero-argument (no-arg) constructor for classes annotated with `@Entity`, `@MappedSuperclass`, and `@Embeddable`.
   - JPA / Hibernate requires a no-arg constructor to instantiate entities via reflection when reading rows from the database.

---

## 2. Spring Boot Core, IoC & Proxy Mechanics

### Q5. How does Spring implement `@Transactional` and `@Cacheable` under the hood?
**Answer:**
Spring utilizes **CGLIB dynamic proxies** (or JDK Dynamic Proxies if interfaces are used):
1. When Spring initializes the `OrderService` bean, it creates a proxy subclass (`OrderService$$SpringCGLIB$$0`).
2. When an external caller invokes `orderService.placeOrder()`, the call hits the **Proxy first**.
3. The proxy invokes the `TransactionInterceptor`, which acquires a database connection, begins the transaction (`setAutoCommit(false)`), and forwards the call to the target method.
4. If the method completes normally, the proxy commits the transaction; if an unchecked exception (`RuntimeException` or `Error`) is thrown, it executes a rollback.

**Crucial Trap Question**: *What happens if method A calls method B within the same class, where method B has `@Transactional(propagation = Propagation.REQUIRES_NEW)`?*
**Answer**: Method B's transaction annotation is **completely ignored**! This is called **self-invocation**. Because the call is executed directly on the `this` reference inside the instance, it bypasses the Spring CGLIB proxy. To fix this, extract method B to a separate service bean.

---

### Q6. Why is Constructor Injection preferred over Field Injection (`@Autowired`)?
**Answer:**
1. **Immutability**: Dependencies can be declared as `val` (or `final` in Java), ensuring they cannot be altered after instantiation.
2. **Testability**: In unit tests (e.g. `OrderServiceTest`), you can instantiate the class using `OrderService(repo, service, ...)` with MockK without needing Spring runner or reflection hacks.
3. **Prevents NullPointerExceptions**: Guarantees all dependencies are provided when the object is instantiated; no partially initialized beans.
4. **Highlights SRP (Single Responsibility Principle) Violations**: Having 8+ constructor parameters is an immediate visual code smell indicating the class does too much.

*Notice in OrderPulse:* Not a single `@Autowired` field exists. Every service and controller utilizes idiomatic Kotlin primary constructor injection.

---

## 3. JPA, Hibernate & Concurrency

### Q7. What is the N+1 Query Problem, and how did you resolve it in OrderPulse?
**Answer:**
- **Problem**: When fetching $N$ orders, accessing `order.items` lazily causes Hibernate to fire 1 query for orders, plus $N$ individual queries for each order's items ($1 + N$ queries). Under high traffic, this exhausts database connection pools and introduces massive latency.
- **Solution in OrderPulse**:
  In [`OrderRepository.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/order/OrderRepository.kt), we used Spring Data JPA's `@EntityGraph`:
  ```kotlin
  @EntityGraph(attributePaths = ["items", "items.product", "user"])
  fun findAllByUserId(userId: Long, pageable: Pageable): Page<Order>
  ```
  Hibernate executes a single SQL `LEFT OUTER JOIN`, fetching the entire entity graph in one database round-trip without changing the global `FetchType.LAZY` contract.

---

### Q8. What is Open Session in View (OSIV), and why should it be disabled in production?
**Answer:**
- **What it is**: Spring Boot enables `spring.jpa.open-in-view=true` by default. It keeps the Hibernate `Session` / `EntityManager` open throughout the entire HTTP request lifecycle (including the controller and view rendering).
- **The Danger**:
  1. It allows controllers and serializers to lazily query the database outside of `@Transactional` boundaries, causing database calls to happen during JSON serialization.
  2. Database connections from HikariCP are held hostage for the entire duration of the HTTP request (including slow network transfers to clients), rapidly exhausting the pool.
- **Production Standard**: Set `spring.jpa.open-in-view=false` in `application.yml` and ensure all required associations are fetched within the `@Transactional` service layer (via DTOs or `@EntityGraph`).

*See in OrderPulse:* [`application.yml`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/resources/application.yml) explicitly sets `open-in-view: false`.

---

### Q9. Optimistic Locking vs Pessimistic Locking: When would you use each?
**Answer:**

| Feature | Optimistic Locking (`@Version`) | Pessimistic Locking (`PESSIMISTIC_WRITE`) |
| :--- | :--- | :--- |
| **Mechanism** | Checks version column on `UPDATE ... WHERE version = ?` | `SELECT ... FOR UPDATE` (database row lock) |
| **Lock Held** | No locks held during reading or processing | Locks row from query until transaction ends |
| **Collision Handling** | Throws `OptimisticLockingFailureException` | Threads wait in queue for the lock |
| **Best Used When** | Read-heavy workloads, low-to-medium contention (<15% collisions) | High contention (e.g. Flash sales, seat booking, ticket release) |
| **Deadlock Risk** | Low / None | High (if multiple rows locked in different order) |

*Implementation in OrderPulse:* [`Product.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/product/Product.kt) contains `@Version open var version: Long = 0`. When two users attempt to purchase the last unit simultaneously, Hibernate detects the mismatch and [`GlobalExceptionHandler.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/GlobalExceptionHandler.kt) maps it to an HTTP 409 Conflict ProblemDetail.

---

### Q10. When should you use `Propagation.REQUIRES_NEW`?
**Answer:**
`Propagation.REQUIRES_NEW` suspends any existing transaction and starts a completely fresh, independent physical transaction with its own database connection.

**Real-world Use Case in OrderPulse:**
In [`PaymentService.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentService.kt), `recordPaymentAttempt()` is marked with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
If the customer's payment fails and an exception causes the outer `placeOrder` transaction to roll back, the payment transaction audit record **must still be persisted** for financial compliance and fraud analysis.

---

## 4. Spring Security 6 & Stateless Auth

### Q11. What changed in Spring Security 6 (Spring Boot 3)?
**Answer:**
1. **Removal of `WebSecurityConfigurerAdapter`**: Configuration is now entirely component-based via `@Bean fun filterChain(http: HttpSecurity): SecurityFilterChain`.
2. **Method Security**: `@EnableGlobalMethodSecurity` is replaced by `@EnableMethodSecurity(prePostEnabled = true)`.
3. **Modern Matchers**: `antMatchers()` and `mvcMatchers()` were consolidated into `requestMatchers()`.
4. **Lambda DSL**: Configuration strictly enforces lambdas (e.g. `http.csrf { it.disable() }`).

*See in OrderPulse:* [`SecurityConfig.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/config/SecurityConfig.kt).

---

### Q12. Why can CSRF be disabled in a stateless REST API?
**Answer:**
CSRF (Cross-Site Request Forgery) exploits the browser's automatic behavior of sending session cookies with cross-origin requests.
In a stateless REST API with JWT:
- Authentication credentials are not stored in browser cookies.
- Every request transmits a custom HTTP header: `Authorization: Bearer <token>`.
- Browsers never automatically attach custom authorization headers across domains; JavaScript must explicitly set them.
- Therefore, CSRF attacks are fundamentally impossible in this architecture.

---

## 5. Caching, Performance & Observability

### Q13. In-memory caching (Caffeine) vs Distributed caching (Redis)?
**Answer:**
- **Caffeine (In-Memory)**:
  - Latency: Sub-microsecond (stored in JVM heap).
  - Overhead: Zero network hops, zero serialization overhead.
  - Drawback: Cache is local to a single JVM instance. If you run 5 instances, their caches can diverge.
  - Ideal for: Static catalogs, config data, reference tables.
- **Redis (Distributed)**:
  - Latency: 1-5 milliseconds (network round-trip).
  - Advantage: Shared single source of truth across all horizontal application pods.
  - Ideal for: User sessions, dynamic shopping carts, distributed rate-limiting.

*See in OrderPulse:* [`CacheConfig.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/config/CacheConfig.kt) configures Caffeine with Window TinyLFU eviction, 500 max size, and 10-minute expiry.

---

### Q14. What is MDC and how does distributed tracing work?
**Answer:**
**MDC (Mapped Diagnostic Context)** provides a way to enrich log entries with contextual key-value pairs (like `traceId` or `userId`) on a `ThreadLocal` basis.
In [`MdcLoggingFilter.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/filter/MdcLoggingFilter.kt):
1. Intercepts incoming requests and extracts or generates an `X-Trace-Id`.
2. Puts the trace ID in `MDC.put("traceId", traceId)`.
3. Every log statement (`log.info()`) automatically includes the trace ID.
4. If an exception occurs, [`GlobalExceptionHandler.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/common/GlobalExceptionHandler.kt) includes the same trace ID in the `ProblemDetail` response.
5. In a `finally` block, `MDC.remove()` cleans up the thread to prevent memory leaks in thread pools.

---

## 6. Design Patterns in Practice

### Q15. How is the Strategy Pattern implemented dynamically in OrderPulse?
**Answer:**
Instead of hardcoding `if (method == CREDIT_CARD) ... else if (method == PAYPAL)`:
1. Define a common interface: [`PaymentStrategy`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentStrategy.kt).
2. Create concrete Spring beans: `CreditCardPaymentStrategy` and `PayPalPaymentStrategy`.
3. In [`PaymentStrategyFactory.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/main/kotlin/com/orderpulse/payment/PaymentStrategyFactory.kt), inject `List<PaymentStrategy>`. Spring automatically collects all beans implementing the interface.
4. Map them by `PaymentMethod`. To support Apple Pay tomorrow, simply add a new `@Component class ApplePayStrategy : PaymentStrategy` — zero existing classes need modification (**Open-Closed Principle**).

---

## 7. Testing Strategy

### Q16. Why is MockK superior to Mockito for Kotlin test suites?
**Answer:**
1. **Final by Default**: Mockito fails on Kotlin's default final classes unless `mock-maker-inline` is configured. MockK works out of the box.
2. **Coroutines**: MockK natively supports suspending functions via `coEvery { service.call() } returns result` and `coVerify`.
3. **Idiomatic DSL**: `every { ... } returns ...` and `verify { ... }` read naturally in Kotlin compared to `Mockito.`when`()`.
4. **Extension & Top-Level Functions**: MockK can mock Kotlin extension functions and top-level functions directly.

*See in OrderPulse:* [`OrderServiceTest.kt`](file:///c:/Users/hshan/Documents/antigravity/beautiful-babbage/src/test/kotlin/com/orderpulse/order/OrderServiceTest.kt).

---

## 8. Quick-Fire Technical Questions Checklist

Before your interview, review this rapid checklist:

- [x] **Can you explain why `@Transactional` does not rollback on checked exceptions by default?**  
  *(Default rollback is on `RuntimeException` and `Error`. Use `@Transactional(rollbackFor = [Exception::class])` for checked exceptions).*
- [x] **What is the difference between `val` and `const val` in Kotlin?**  
  *(`val` is read-only evaluated at runtime; `const val` is a compile-time constant primitive or String).*
- [x] **What is the difference between Spring's Bean Scopes?**  
  *(`singleton` (default, 1 per application context), `prototype` (new instance every injection), `request`, `session`).*
- [x] **How does Flyway guarantee database integrity across team environments?**  
  *(Calculates CRC32 checksums for each migration file in `flyway_schema_history`; fails startup if an applied script was tampered with).*
- [x] **What HTTP status code should validation errors return?**  
  *(HTTP 400 Bad Request or HTTP 422 Unprocessable Entity, formatted using RFC 7807 ProblemDetail).*
