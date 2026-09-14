package com.orderpulse.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

enum class PaymentStatus {
    SUCCESS,
    FAILED
}

@Entity
@Table(name = "payment_transactions")
open class PaymentTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    @Column(name = "order_id", nullable = false)
    open var orderId: Long = 0,

    @Column(name = "transaction_reference", nullable = false, unique = true)
    open var transactionReference: String = "",

    @Column(nullable = false, precision = 19, scale = 4)
    open var amount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    open var paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    open var status: PaymentStatus = PaymentStatus.SUCCESS,

    @Column(name = "failure_reason", length = 500)
    open var failureReason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now()
)
