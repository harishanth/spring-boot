package com.orderpulse.order

import com.orderpulse.common.InsufficientStockException
import com.orderpulse.common.PaymentFailedException
import com.orderpulse.metrics.OrderMetrics
import com.orderpulse.payment.PaymentMethod
import com.orderpulse.payment.PaymentResult
import com.orderpulse.payment.PaymentService
import com.orderpulse.product.Product
import com.orderpulse.product.ProductService
import com.orderpulse.user.Role
import com.orderpulse.user.User
import com.orderpulse.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

/**
 * Unit test for OrderService using MockK.
 *
 * CRITICAL INTERVIEW TOPIC: MockK vs Mockito in Kotlin
 *
 * Why MockK is preferred in Kotlin projects:
 * 1. Native handling of Kotlin's `final` classes and methods without `mock-maker-inline` hacks.
 * 2. Idiomatic Kotlin syntax (`every { ... } returns ...`, `verify { ... }`).
 * 3. Built-in support for Coroutines (`coEvery`, `coVerify`).
 * 4. First-class support for extension functions, top-level functions, and sealed classes.
 * 5. Handles Kotlin default parameter values and `value class` seamlessly.
 */
class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val productService: ProductService = mockk()
    private val paymentService: PaymentService = mockk()
    private val orderMetrics: OrderMetrics = mockk(relaxed = true)

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(
            orderRepository = orderRepository,
            userRepository = userRepository,
            productService = productService,
            paymentService = paymentService,
            orderMetrics = orderMetrics
        )
    }

    @Test
    @DisplayName("Should successfully place order when stock is available and payment succeeds")
    fun shouldPlaceOrderSuccessfully() {
        // Given
        val userId = 1L
        val user = User("user@test.com", "hash", "Test User", Role.ROLE_CUSTOMER).apply { id = userId }

        val product = Product(
            name = "Test Laptop",
            price = BigDecimal("1000.00"),
            stockQuantity = 5
        ).apply { id = 10L }

        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 10L, quantity = 2)),
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { productService.getProductEntity(10L) } returns product
        every { orderRepository.save(any()) } answers {
            val saved = firstArg<Order>()
            saved.id = 100L
            saved
        }
        every {
            paymentService.processPayment(eq(100L), any(), any(), eq(PaymentMethod.CREDIT_CARD))
        } returns PaymentResult.Success("TX-123456")

        // When
        val response = orderService.placeOrder(userId, request)

        // Then
        assertThat(response.orderNumber).isNotNull()
        assertThat(response.status).isEqualTo(OrderStatus.PAID)
        assertThat(response.totalAmount).isEqualByComparingTo(BigDecimal("2000.00"))
        assertThat(product.stockQuantity).isEqualTo(3) // 5 - 2 = 3

        verify(exactly = 1) { orderMetrics.incrementOrdersPlaced() }
        verify(exactly = 1) { orderMetrics.recordOrderRevenue(any()) }
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested quantity exceeds available stock")
    fun shouldThrowWhenStockIsInsufficient() {
        // Given
        val userId = 1L
        val user = User("user@test.com", "hash", "Test User", Role.ROLE_CUSTOMER).apply { id = userId }

        val product = Product(
            name = "Rare Item",
            price = BigDecimal("500.00"),
            stockQuantity = 1
        ).apply { id = 20L }

        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 20L, quantity = 3)),
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { productService.getProductEntity(20L) } returns product

        // When / Then
        assertThatThrownBy { orderService.placeOrder(userId, request) }
            .isInstanceOf(InsufficientStockException::class.java)
            .hasMessageContaining("Insufficient stock")

        verify(exactly = 0) { orderRepository.save(any()) }
        verify(exactly = 0) { paymentService.processPayment(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("Should restore stock and mark order CANCELLED when payment fails")
    fun shouldRestoreStockWhenPaymentFails() {
        // Given
        val userId = 1L
        val user = User("user@test.com", "hash", "Test User", Role.ROLE_CUSTOMER).apply { id = userId }

        val product = Product(
            name = "Test Phone",
            price = BigDecimal("800.00"),
            stockQuantity = 10
        ).apply { id = 30L }

        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 30L, quantity = 2)),
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { productService.getProductEntity(30L) } returns product
        every { orderRepository.save(any()) } answers {
            val saved = firstArg<Order>()
            saved.id = 200L
            saved
        }
        every {
            paymentService.processPayment(eq(200L), any(), any(), eq(PaymentMethod.CREDIT_CARD))
        } returns PaymentResult.Failure("Card declined by bank")

        // When / Then
        assertThatThrownBy { orderService.placeOrder(userId, request) }
            .isInstanceOf(PaymentFailedException::class.java)
            .hasMessageContaining("Card declined by bank")

        // Verify stock was restored back to 10
        assertThat(product.stockQuantity).isEqualTo(10)
        verify(exactly = 1) { orderMetrics.incrementOrdersFailed() }
    }
}
