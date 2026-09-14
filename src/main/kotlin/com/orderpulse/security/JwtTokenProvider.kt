package com.orderpulse.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

/**
 * Service responsible for generating, signing, and validating JWTs using JJWT 0.12.x.
 *
 * Interview Tip:
 * JJWT 0.12 introduced strict cryptographic type safety:
 * - HMAC keys must be SecretKey instances generated via Keys.hmacShaKeyFor() with minimum 256 bits (32 bytes).
 * - Jwts.parser() replaces the legacy Jwts.parserBuilder().
 * - Claims are strongly typed and parsed via parseSignedClaims(token).
 */
@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.expiration-ms}") private val jwtExpirationMs: Long
) {
    private val log = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Generates a JWT token for the authenticated user principal.
     */
    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(userPrincipal.username)
            .claim("userId", userPrincipal.id)
            .claim("fullName", userPrincipal.fullName)
            .claim("roles", userPrincipal.authorities.map { it.authority })
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(signingKey)
            .compact()
    }

    /**
     * Extracts the subject (email) from the JWT token.
     */
    fun getEmailFromToken(token: String): String {
        val claims: Claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject
    }

    /**
     * Validates signature and expiry of the JWT token.
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (ex: JwtException) {
            log.warn("Invalid JWT token: {}", ex.message)
            false
        } catch (ex: IllegalArgumentException) {
            log.warn("JWT claims string is empty: {}", ex.message)
            false
        }
    }
}
