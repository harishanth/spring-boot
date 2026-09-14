package com.orderpulse.order

import com.orderpulse.common.PaymentFailedException
import com.orderpulse.common.ResourceNotFoundException
import com.orderpulse.common.UnauthorizedException
import com.orderpulse.metrics.OrderMetrics
import com.orderpulse.payment.PaymentResult
import com.orderpulse.payment.PaymentService
import com.orderpulse.product.ProductService
import com.orderpulse.user.Role
import com.orderpulse.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Core domain service orchestrating the Order placement lifecycle.
 *
 * CRITICAL INTERVIEW TOPICS COVERED:
 * 1. Transaction Boundaries (@Transactional):
 *    - Atomicity across multiple repository updates (User check, Stock deduction, Order persistence).
 * 2. Concurrency Control:
 *    - Product stock deduction relies on Product entity's @Version (Optimistic Locking).
 * 3. Strategy Pattern Integration:
 *    - Pluggable payment handling decoupled from the order domain.
 * 4. Observability:
 *    - Custom Micrometer counters tracking business metrics.
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productService: ProductService,
    private val paymentService: PaymentService,
    private val orderMetrics: OrderMetrics
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    @Transactional
    fun placeOrder(userId: Long, request: CreateOrderRequest): OrderResponse {
        log.info("Initiating order placement for user [id={}] with {} items", userId, request.items.size)

        val user = userRepository.findById(userId).orElseThrow {
            ResourceNotFoundException("User not found with id: $userId")
        }

        val orderNumber = "ORD-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
        val order = Order(
            orderNumber = orderNumber,
            user = user,
            paymentMethod = request.paymentMethod,
            status = OrderStatus.PENDING
        )

        // Deduct inventory and build OrderItems
        val deductedProducts = mutableListOf<Pair<Long, Int>>()
        for (itemReq in request.items) {
            val product = productService.getProductEntity(itemReq.productId)
            product.deductStock(itemReq.quantity)
            deductedProducts.add(product.id!! to itemReq.quantity)

            val orderItem = OrderItem(
                order = order,
                product = product,
                quantity = itemReq.quantity,
                priceAtPurchase = product.price
            )
            order.addItem(orderItem)
        }

        order.calculateTotal()
        val savedOrder = orderRepository.save(order)

        // Process payment via Strategy Pattern
        val paymentResult = paymentService.processPayment(
            orderId = savedOrder.id!!,
            orderNumber = savedOrder.orderNumber,
            amount = savedOrder.totalAmount,
            method = savedOrder.paymentMethod
        )

        return when (paymentResult) {
            is PaymentResult.Success -> {
                savedOrder.status = OrderStatus.PAID
                val finalOrder = orderRepository.save(savedOrder)
                orderMetrics.incrementOrdersPlaced()
                orderMetrics.recordOrderRevenue(finalOrder.totalAmount)
                log.info("Order [{}] placed and paid successfully. TxRef={}", finalOrder.orderNumber, paymentResult.transactionReference)
                finalOrder.toResponse()
            }
            is PaymentResult.Failure -> {
                savedOrder.status = OrderStatus.CANCELLED
                orderRepository.save(savedOrder)

                // Compensating action: restore stock
                deductedProducts.forEach { (productId, qty) ->
                    val prod = productService.getProductEntity(productId)
                    prod.addStock(qty)
                }

                orderMetrics.incrementOrdersFailed()
                log.warn("Payment failed for order [{}]: {}", savedOrder.orderNumber, paymentResult.reason)
                throw PaymentFailedException("Order placement failed: ${paymentResult.reason}")
            }
        }
    }

    @Transactional(readOnly = true)
    fun getOrderById(orderId: Long, currentUserId: Long, userRole: Role): OrderResponse {
        val order = orderRepository.findWithDetailsById(orderId).orElseThrow {
            ResourceNotFoundException("Order not found with id: $orderId")
        }

        if (userRole != Role.ROLE_ADMIN && order.user?.id != currentUserId) {
            throw UnauthorizedException("You are not authorized to view this order.")
        }

        return order.toResponse()
    }

    @Transactional(readOnly = true)
    fun getOrdersForUser(userId: Long, pageable: Pageable): Page<OrderResponse> {
        return orderRepository.findAllByUserId(userId, pageable).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getAllOrders(pageable: Pageable): Page<OrderResponse> {
        return orderRepository.findAll(pageable).map { it.toResponse() }
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus): OrderResponse {
        val order = orderRepository.findById(orderId).orElseThrow {
            ResourceNotFoundException("Order not found with id: $orderId")
        }
        order.status = newStatus
        val updated = orderRepository.save(order)
        log.info("Updated order [{}] status to {}", order.orderNumber, newStatus)
        return updated.toResponse()
    }
}
