package com.orderpulse.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * Base abstract entity providing generated ID and auditing timestamps.
 *
 * CRITICAL INTERVIEW GOTCHA:
 * Why should JPA entities NEVER be Kotlin `data class`es?
 *
 * 1. equals() & hashCode():
 *    - Kotlin `data class` automatically generates equals/hashCode using all constructor properties.
 *    - For new JPA entities, `id` is null before persisting! When persisted, the ID changes, breaking HashSet/HashMap contracts.
 *    - If lazy-loaded collections or relations are part of the data class, calling equals()/hashCode()
 *      triggers database queries (LazyInitializationException outside transaction, or severe N+1 queries).
 *
 * 2. toString():
 *    - A data class generates toString() using all fields. If an entity has a bidirectional relationship
 *      (e.g., Order -> OrderItem -> Order), toString() causes a fatal StackOverflowError or triggers lazy loading.
 *
 * 3. copy():
 *    - Data classes provide copy(). In JPA, copying an entity creates a shallow copy sharing the same ID
 *      or detached state, causing dirty checking anomalies and duplicate persistence exceptions.
 *
 * 4. Proxies & Final Classes:
 *    - Kotlin classes and methods are `final` by default. Hibernate requires non-final classes to create CGLIB runtime proxies.
 *    - The `kotlin-spring` and `kotlin-jpa` plugins open entities, but regular `open class` remains the safe, idiomatic standard.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    open var updatedAt: Instant = Instant.now()

    /**
     * Proper JPA equals based on database ID identity.
     * Two unpersisted entities (id == null) are distinct instances.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val otherEntity = other as BaseEntity
        return id != null && id == otherEntity.id
    }

    override fun hashCode(): Int {
        // Must return a constant for entities to preserve consistency across persisted/unpersisted states
        return javaClass.hashCode()
    }
}
