package com.orderpulse.auth

import com.orderpulse.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Authentication", description = "Endpoints for user registration, login, and profile info")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @Operation(summary = "Register a new user", description = "Creates a new customer or admin account and returns a JWT token.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "User login", description = "Authenticates user credentials and returns a Bearer JWT token.")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Get current authenticated user profile",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<UserSummaryResponse> {
        return ResponseEntity.ok(
            UserSummaryResponse(
                id = principal.id,
                email = principal.username,
                fullName = principal.fullName,
                role = com.orderpulse.user.Role.valueOf(principal.authorities.first().authority)
            )
        )
    }
}
