package com.orderpulse.common

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.time.Instant

/**
 * Centralized Global Exception Handler using Spring Boot 3's RFC 7807 ProblemDetail standard.
 *
 * Interview Highlights:
 * 1. Extends ResponseEntityExceptionHandler to leverage Spring's built-in MVC exception handling.
 * 2. ProblemDetail (introduced in Spring 6 / Spring Boot 3) provides a standardized machine-readable format.
 * 3. Handles OptimisticLockingFailureException gracefully with HTTP 409 Conflict.
 * 4. Extracts MDC trace IDs to correlate client errors with backend logs.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handles custom application exceptions.
     */
    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ProblemDetail> {
        log.warn("Application exception [{}]: {}", ex.status, ex.message)
        val problem = ProblemDetail.forStatusAndDetail(ex.status, ex.message)
        enrichProblemDetail(problem)
        return ResponseEntity.status(ex.status).body(problem)
    }

    /**
     * Handles JPA/Hibernate optimistic locking concurrency failures (e.g. flash sale stock collisions).
     */
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: OptimisticLockingFailureException): ResponseEntity<ProblemDetail> {
        log.warn("Optimistic lock collision detected: {}", ex.message)
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The resource was updated concurrently by another request. Please retry your operation."
        )
        problem.title = "Concurrent Conflict"
        problem.type = URI.create("urn:problem-type:concurrent-conflict")
        enrichProblemDetail(problem)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem)
    }

    /**
     * Handles Spring Security authentication failures.
     */
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password.")
        problem.title = "Authentication Failed"
        enrichProblemDetail(problem)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem)
    }

    /**
     * Handles Spring Security access denied (403).
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions.")
        problem.title = "Forbidden"
        enrichProblemDetail(problem)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem)
    }

    /**
     * Handles Bean Validation errors (@Valid / @NotNull / @NotBlank).
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as? FieldError)?.field ?: error.objectName
            val errorMessage = error.defaultMessage ?: "Validation failed"
            fieldName to errorMessage
        }

        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields.")
        problem.title = "Invalid Request Body"
        problem.setProperty("invalid_fields", errors)
        enrichProblemDetail(problem)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }

    /**
     * Catch-all fallback for unexpected server errors (500).
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ProblemDetail> {
        log.error("Unhandled internal server error", ex)
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please contact support."
        )
        problem.title = "Internal Server Error"
        enrichProblemDetail(problem)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }

    private fun enrichProblemDetail(problem: ProblemDetail) {
        problem.setProperty("timestamp", Instant.now().toString())
        val traceId = MDC.get("traceId")
        if (traceId != null) {
            problem.setProperty("traceId", traceId)
        }
    }
}
