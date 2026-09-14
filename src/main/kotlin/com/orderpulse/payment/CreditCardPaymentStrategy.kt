package com.orderpulse.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class CreditCardPaymentStrategy : PaymentStrategy {
    private val log = LoggerFactory.getLogger(CreditCardPaymentStrategy::class.java)

    override val paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD

    override fun process(orderNumber: String, amount: BigDecimal): PaymentResult {
        log.info("Processing Credit Card payment for order [{}] of amount [{}]", orderNumber, amount)

        // Simulate credit card gateway logic (e.g. Stripe / Adyen)
        return if (amount <= BigDecimal("50000.00")) {
            val txRef = "CC-TX-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
            log.info("Credit Card payment approved: {}", txRef)
            PaymentResult.Success(transactionReference = txRef)
        } else {
            log.warn("Credit Card payment declined: exceeds single transaction limit")
            PaymentResult.Failure(reason = "Transaction amount exceeds single credit card payment limit.")
        }
    }
}
