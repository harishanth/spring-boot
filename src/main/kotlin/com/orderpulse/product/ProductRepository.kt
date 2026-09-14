package com.orderpulse.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findByNameContainingIgnoreCase(query: String, pageable: Pageable): Page<Product>
    fun findAllByStockQuantityGreaterThan(minStock: Int, pageable: Pageable): Page<Product>
}
