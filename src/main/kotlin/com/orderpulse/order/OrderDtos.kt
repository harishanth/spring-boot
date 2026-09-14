package com.orderpulse.order

import com.orderpulse.payment.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class OrderItemRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: Long,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int
)

data class CreateOrderRequest(
    @field:NotEmpty(message = "Order must contain at least one item")
    @field:Valid
    val items: List<OrderItemRequest>,

    @field:NotNull(message = "Payment method is required")
    val paymentMethod: PaymentMethod
)

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val priceAtPurchase: BigDecimal,
    val subtotal: BigDecimal
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val customerEmail: String,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod,
    val totalAmount: BigDecimal,
    val items: List<OrderItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Idiomatic Kotlin extension function converting Order entity to OrderResponse DTO.
 */
fun Order.toResponse(): OrderResponse = OrderResponse(
    id = requireNotNull(id) { "Order ID cannot be null" },
    orderNumber = orderNumber,
    userId = requireNotNull(user?.id) { "User cannot be null" },
    customerEmail = user?.email ?: "Unknown",
    status = status,
    paymentMethod = paymentMethod,
    totalAmount = totalAmount,
    items = items.map { item ->
        val price = item.priceAtPurchase
        val qty = item.quantity
        OrderItemResponse(
            id = requireNotNull(item.id) { "OrderItem ID cannot be null" },
            productId = requireNotNull(item.product?.id) { "Product ID cannot be null" },
            productName = item.product?.name ?: "Unknown Product",
            quantity = qty,
            priceAtPurchase = price,
            subtotal = price.multiply(BigDecimal(qty))
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt
)
