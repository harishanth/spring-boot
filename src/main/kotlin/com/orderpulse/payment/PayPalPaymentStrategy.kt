package com.orderpulse.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class PayPalPaymentStrategy : PaymentStrategy {
    private val log = LoggerFactory.getLogger(PayPalPaymentStrategy::class.java)

    override val paymentMethod: PaymentMethod = PaymentMethod.PAYPAL

    override fun process(orderNumber: String, amount: BigDecimal): PaymentResult {
        log.info("Processing PayPal payment for order [{}] of amount [{}]", orderNumber, amount)

        // Simulate PayPal express checkout processing
        return if (amount > BigDecimal.ZERO) {
            val txRef = "PP-TX-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
            log.info("PayPal payment approved: {}", txRef)
            PaymentResult.Success(transactionReference = txRef)
        } else {
            PaymentResult.Failure(reason = "Invalid transaction amount for PayPal payment.")
        }
    }
}
