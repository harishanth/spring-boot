package com.orderpulse.payment

/**
 * Sealed interface representing the outcome of a payment attempt.
 *
 * CRITICAL KOTLIN INTERVIEW TOPIC: Sealed Classes / Interfaces
 *
 * 1. Exhaustiveness in `when` expressions:
 *    - The compiler knows all permitted subtypes at compile-time.
 *    - No `else` branch is required, eliminating runtime unhandled branch bugs.
 *
 * 2. Expressive Domain Modeling (Result Pattern):
 *    - Instead of throwing exceptions for predictable business outcomes (e.g. card declined),
 *      sealed hierarchies provide type-safe, expressive representations.
 */
sealed interface PaymentResult {
    data class Success(
        val transactionReference: String
    ) : PaymentResult

    data class Failure(
        val reason: String
    ) : PaymentResult
}
