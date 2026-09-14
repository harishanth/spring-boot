package com.orderpulse.auth

import com.orderpulse.common.DuplicateResourceException
import com.orderpulse.security.JwtTokenProvider
import com.orderpulse.security.UserPrincipal
import com.orderpulse.user.User
import com.orderpulse.user.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val tokenProvider: JwtTokenProvider
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("An account with email '${request.email}' already exists.")
        }

        val user = User(
            email = request.email.trim().lowercase(),
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName.trim(),
            role = request.role
        )

        val savedUser = userRepository.save(user)
        val principal = UserPrincipal.create(savedUser)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        val token = tokenProvider.generateToken(authentication)

        return AuthResponse(
            accessToken = token,
            user = savedUser.toSummaryResponse()
        )
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.email.trim().lowercase(),
                request.password
            )
        )

        val principal = authentication.principal as UserPrincipal
        val token = tokenProvider.generateToken(authentication)

        return AuthResponse(
            accessToken = token,
            user = UserSummaryResponse(
                id = principal.id,
                email = principal.username,
                fullName = principal.fullName,
                role = com.orderpulse.user.Role.valueOf(principal.authorities.first().authority)
            )
        )
    }
}
