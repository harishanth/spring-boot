package com.orderpulse.user

import com.orderpulse.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * User JPA Entity.
 *
 * Notice:
 * 1. Extends BaseEntity to inherit id, createdAt, and updatedAt.
 * 2. Regular open class (not data class) to avoid proxy and hashCode issues.
 * 3. Protected/no-arg constructor provided by kotlin-jpa plugin or default arguments.
 */
@Entity
@Table(name = "users")
open class User(
    @Column(nullable = false, unique = true)
    open var email: String = "",

    @Column(nullable = false)
    open var password: String = "",

    @Column(name = "full_name", nullable = false)
    open var fullName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    open var role: Role = Role.ROLE_CUSTOMER
) : BaseEntity()
