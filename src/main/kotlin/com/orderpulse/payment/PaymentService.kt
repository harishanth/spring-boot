package com.orderpulse.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * Service managing payment processing and audit logging.
 *
 * CRITICAL INTERVIEW TOPIC: Transaction Propagation (REQUIRES_NEW)
 *
 * Notice the recordPaymentAttempt method uses `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
 * - Propagation.REQUIRED (default): Joins existing transaction or creates a new one if none exists.
 * - Propagation.REQUIRES_NEW: Suspends the active transaction and starts a completely independent
 *   physical transaction with its own database connection.
 *
 * Why use it here?
 * If an order process encounters an error downstream (e.g. inventory conflict or notification failure)
 * and rolls back the outer transaction, our payment audit record MUST NOT roll back!
 * Financial compliance and auditability require a permanent record of all payment gateway interactions.
 */
@Service
class PaymentService(
    private val paymentStrategyFactory: PaymentStrategyFactory,
    private val paymentTransactionRepository: PaymentTransactionRepository
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    fun processPayment(
        orderId: Long,
        orderNumber: String,
        amount: BigDecimal,
        method: PaymentMethod
    ): PaymentResult {
        val strategy = paymentStrategyFactory.getStrategy(method)
        val result = strategy.process(orderNumber, amount)

        when (result) {
            is PaymentResult.Success -> {
                recordPaymentAttempt(
                    orderId = orderId,
                    txRef = result.transactionReference,
                    amount = amount,
                    method = method,
                    status = PaymentStatus.SUCCESS,
                    failureReason = null
                )
            }
            is PaymentResult.Failure -> {
                recordPaymentAttempt(
                    orderId = orderId,
                    txRef = "FAILED-${UUID.randomUUID().toString().substring(0, 8).uppercase()}",
                    amount = amount,
                    method = method,
                    status = PaymentStatus.FAILED,
                    failureReason = result.reason
                )
            }
        }

        return result
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordPaymentAttempt(
        orderId: Long,
        txRef: String,
        amount: BigDecimal,
        method: PaymentMethod,
        status: PaymentStatus,
        failureReason: String?
    ): PaymentTransaction {
        val tx = PaymentTransaction(
            orderId = orderId,
            transactionReference = txRef,
            amount = amount,
            paymentMethod = method,
            status = status,
            failureReason = failureReason
        )
        val saved = paymentTransactionRepository.save(tx)
        log.info("Recorded payment audit record [id={}]: status={}, txRef={}", saved.id, status, txRef)
        return saved
    }
}
