package com.orderpulse.product

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal

/**
 * Slice test for ProductRepository using @DataJpaTest.
 *
 * Interview Tip:
 * Why use slice tests (@DataJpaTest, @WebMvcTest) instead of @SpringBootTest for everything?
 * 1. Test Speed: @DataJpaTest only spins up JPA repositories, EntityManager, and embedded DB,
 *    skipping Security, Web servers, and business services.
 * 2. Isolation: Focuses strictly on SQL queries, schema mapping, and entity constraints.
 * 3. Automatic Rollback: Every test method runs in a transaction that rolls back automatically.
 */
@DataJpaTest
class ProductRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val productRepository: ProductRepository
) {

    @Test
    @DisplayName("Should find products matching case-insensitive partial name")
    fun shouldFindByNameContainingIgnoreCase() {
        // Given
        val p1 = Product(name = "Apple iPhone 15 Pro", price = BigDecimal("999.00"), stockQuantity = 20)
        val p2 = Product(name = "Samsung Galaxy S24", price = BigDecimal("899.00"), stockQuantity = 15)
        val p3 = Product(name = "Apple iPad Air", price = BigDecimal("599.00"), stockQuantity = 10)

        entityManager.persist(p1)
        entityManager.persist(p2)
        entityManager.persist(p3)
        entityManager.flush()

        // When
        val result = productRepository.findByNameContainingIgnoreCase("apple", PageRequest.of(0, 10))

        // Then
        assertThat(result.totalElements).isGreaterThanOrEqualTo(2)
        assertThat(result.content).extracting("name").contains("Apple iPhone 15 Pro", "Apple iPad Air")
    }

    @Test
    @DisplayName("Should increment version number upon product update (Optimistic Locking)")
    fun shouldIncrementVersionOnUpdate() {
        // Given
        val product = Product(name = "Mechanical Keyboard", price = BigDecimal("150.00"), stockQuantity = 10)
        val persisted = entityManager.persistFlushFind(product)
        val initialVersion = persisted.version

        // When
        persisted.stockQuantity = 8
        val updated = entityManager.persistFlushFind(persisted)

        // Then
        assertThat(updated.version).isGreaterThan(initialVersion)
    }
}
