package com.orderpulse.payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, Long> {
    fun findByOrderId(orderId: Long): List<PaymentTransaction>
    fun findByTransactionReference(transactionReference: String): PaymentTransaction?
}
