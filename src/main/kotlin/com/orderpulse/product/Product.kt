package com.orderpulse.product

import com.orderpulse.common.BaseEntity
import com.orderpulse.common.InsufficientStockException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

/**
 * Product Entity featuring Optimistic Locking via @Version.
 *
 * CRITICAL INTERVIEW TOPIC: Optimistic vs Pessimistic Locking
 *
 * 1. Optimistic Locking (@Version):
 *    - Uses a version counter in the database.
 *    - On UPDATE: "UPDATE products SET stock = ?, version = version + 1 WHERE id = ? AND version = ?"
 *    - If another transaction modified the row in the meantime, the row count returned is 0,
 *      causing Hibernate to throw ObjectOptimisticLockingFailureException (translated to 409 Conflict).
 *    - Best for: Read-heavy systems with occasional concurrent writes; avoids holding costly DB locks.
 *
 * 2. Pessimistic Locking (@Lock(LockModeType.PESSIMISTIC_WRITE)):
 *    - Issues "SELECT ... FOR UPDATE" in SQL.
 *    - Acquires an exclusive row-level lock immediately.
 *    - Best for: Extremely high-contention writes where conflict rates exceed 20-30%, or where retrying is unacceptable.
 */
@Entity
@Table(name = "products")
open class Product(
    @Column(nullable = false)
    open var name: String = "",

    @Column(length = 1000)
    open var description: String? = null,

    @Column(nullable = false, precision = 19, scale = 4)
    open var price: BigDecimal = BigDecimal.ZERO,

    @Column(name = "stock_quantity", nullable = false)
    open var stockQuantity: Int = 0,

    @Version
    @Column(nullable = false)
    open var version: Long = 0
) : BaseEntity() {

    /**
     * Domain method enforcing stock invariants.
     */
    fun deductStock(quantity: Int) {
        require(quantity > 0) { "Deduction quantity must be positive" }
        if (stockQuantity < quantity) {
            throw InsufficientStockException(
                "Insufficient stock for product '$name'. Requested: $quantity, Available: $stockQuantity"
            )
        }
        this.stockQuantity -= quantity
    }

    fun addStock(quantity: Int) {
        require(quantity > 0) { "Stock addition quantity must be positive" }
        this.stockQuantity += quantity
    }
}
