package com.orderpulse.security

import com.orderpulse.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Custom UserDetails implementation wrapping our User entity.
 */
class UserPrincipal(
    val id: Long,
    private val email: String,
    private val passwordHash: String,
    val fullName: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    companion object {
        fun create(user: User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority(user.role.name))
            return UserPrincipal(
                id = requireNotNull(user.id) { "User ID cannot be null when creating UserPrincipal" },
                email = user.email,
                passwordHash = user.password,
                fullName = user.fullName,
                authorities = authorities
            )
        }
    }

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
