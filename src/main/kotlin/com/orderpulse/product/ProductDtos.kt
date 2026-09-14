package com.orderpulse.product

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class CreateProductRequest(
    @field:NotBlank(message = "Product name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    val price: BigDecimal,

    @field:NotNull(message = "Stock quantity is required")
    @field:Min(value = 0, message = "Stock quantity cannot be negative")
    val stockQuantity: Int
)

data class UpdateProductRequest(
    @field:NotBlank(message = "Product name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    val price: BigDecimal,

    @field:NotNull(message = "Stock quantity is required")
    @field:Min(value = 0, message = "Stock quantity cannot be negative")
    val stockQuantity: Int
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val stockQuantity: Int,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Idiomatic Kotlin extension function converting Product entity to ProductResponse DTO.
 */
fun Product.toResponse(): ProductResponse = ProductResponse(
    id = requireNotNull(id) { "Product ID cannot be null when mapping to response" },
    name = name,
    description = description,
    price = price,
    stockQuantity = stockQuantity,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt
)
