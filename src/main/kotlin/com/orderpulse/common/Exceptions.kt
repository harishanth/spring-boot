package com.orderpulse.common

import org.springframework.http.HttpStatus

/**
 * Base custom application exception with HTTP status mapping.
 */
sealed class AppException(
    override val message: String,
    val status: HttpStatus,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class ResourceNotFoundException(message: String) : 
    AppException(message, HttpStatus.NOT_FOUND)

class InsufficientStockException(message: String) : 
    AppException(message, HttpStatus.BAD_REQUEST)

class ConcurrentUpdateException(message: String, cause: Throwable? = null) : 
    AppException(message, HttpStatus.CONFLICT, cause)

class PaymentFailedException(message: String) : 
    AppException(message, HttpStatus.PAYMENT_REQUIRED)

class BadRequestException(message: String) : 
    AppException(message, HttpStatus.BAD_REQUEST)

class UnauthorizedException(message: String) : 
    AppException(message, HttpStatus.UNAUTHORIZED)

class DuplicateResourceException(message: String) : 
    AppException(message, HttpStatus.CONFLICT)
