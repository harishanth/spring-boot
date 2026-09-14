package com.orderpulse.order

import com.orderpulse.common.BaseEntity
import com.orderpulse.payment.PaymentMethod
import com.orderpulse.product.Product
import com.orderpulse.user.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Order JPA Entity.
 *
 * CRITICAL INTERVIEW TOPIC: JPA Relationships & The N+1 Problem
 *
 * 1. FetchType.LAZY:
 *    - Always use FetchType.LAZY on @ManyToOne and @OneToMany relationships.
 *    - EAGER fetching causes Cartesian product joins or triggers N+1 queries when loading multiple rows.
 *
 * 2. Bidirectional Relationship Invariants:
 *    - When managing @OneToMany (Order -> OrderItem) and @ManyToOne (OrderItem -> Order),
 *      always provide helper methods (`addItem`, `removeItem`) to sync BOTH sides of the memory graph.
 *
 * 3. CascadeType.ALL + orphanRemoval = true:
 *    - Ensures persisting/removing Order cascades to its lifecycle-dependent child OrderItems.
 */
@Entity
@Table(name = "orders")
open class Order(
    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    open var orderNumber: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    open var user: User? = null,

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    open var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    open var status: OrderStatus = OrderStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    open var paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD
) : BaseEntity() {

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    open var items: MutableList<OrderItem> = mutableListOf()

    fun addItem(item: OrderItem) {
        items.add(item)
        item.order = this
    }

    fun removeItem(item: OrderItem) {
        items.remove(item)
        item.order = null
    }

    fun calculateTotal(): BigDecimal {
        this.totalAmount = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.priceAtPurchase.multiply(BigDecimal(item.quantity)))
        }
        return this.totalAmount
    }
}

@Entity
@Table(name = "order_items")
open class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    open var order: Order? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    open var product: Product? = null,

    @Column(nullable = false)
    open var quantity: Int = 1,

    @Column(name = "price_at_purchase", nullable = false, precision = 19, scale = 4)
    open var priceAtPurchase: BigDecimal = BigDecimal.ZERO
) : BaseEntity()
