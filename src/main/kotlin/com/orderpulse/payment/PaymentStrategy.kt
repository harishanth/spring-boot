package com.orderpulse.payment

import java.math.BigDecimal

/**
 * Strategy pattern interface for payment processing providers.
 */
interface PaymentStrategy {
    val paymentMethod: PaymentMethod
    fun process(orderNumber: String, amount: BigDecimal): PaymentResult
}
