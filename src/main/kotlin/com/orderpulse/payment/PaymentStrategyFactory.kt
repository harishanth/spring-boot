package com.orderpulse.payment

import org.springframework.stereotype.Component

/**
 * Factory that dynamically discovers and provides PaymentStrategy instances.
 *
 * Interview Tip:
 * Notice Spring DI magic: injecting `List<PaymentStrategy>` automatically gathers all Spring-managed
 * beans implementing `PaymentStrategy`. This cleanly adheres to the Open-Closed Principle (OCP):
 * to add a new payment provider (e.g. Apple Pay, Crypto), simply create a new `@Component class ApplePayStrategy`
 * without altering existing code!
 */
@Component
class PaymentStrategyFactory(
    strategies: List<PaymentStrategy>
) {
    private val strategyMap: Map<PaymentMethod, PaymentStrategy> =
        strategies.associateBy { it.paymentMethod }

    fun getStrategy(method: PaymentMethod): PaymentStrategy {
        return strategyMap[method]
            ?: throw IllegalArgumentException("No payment strategy registered for method: $method")
    }
}
