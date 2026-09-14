package com.orderpulse.auth

import com.orderpulse.user.Role
import com.orderpulse.user.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Authentication DTOs.
 *
 * CRITICAL KOTLIN + SPRING INTERVIEW GOTCHA:
 * Why `@field:NotBlank` instead of `@NotBlank`?
 * In Kotlin constructor properties, `@NotBlank` defaults to annotating the constructor parameter,
 * NOT the generated private field or getter!
 * Using the `@field:` use-site target guarantees Bean Validation inspects the field during validation.
 */
data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    val password: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    val role: Role = Role.ROLE_CUSTOMER
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val user: UserSummaryResponse
)

data class UserSummaryResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: Role
)

/**
 * Idiomatic Kotlin extension function to convert a User entity to a UserSummaryResponse DTO.
 */
fun User.toSummaryResponse(): UserSummaryResponse = UserSummaryResponse(
    id = requireNotNull(id) { "User ID cannot be null when mapping to response" },
    email = email,
    fullName = fullName,
    role = role
)
