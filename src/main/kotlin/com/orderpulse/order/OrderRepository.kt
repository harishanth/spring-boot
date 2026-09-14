package com.orderpulse.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Order Repository demonstrating solutions to the N+1 Query Problem.
 *
 * CRITICAL INTERVIEW TOPIC: Solving the N+1 Problem
 *
 * What is the N+1 problem?
 * When querying 10 Orders, if each Order accesses its items, Hibernate triggers:
 * 1 query for the orders + 10 individual queries for each order's items = 11 queries.
 *
 * Solutions:
 * 1. `@EntityGraph(attributePaths = ["items", "items.product", "user"])`:
 *    Generates a single SQL JOIN query fetching the root entity and its associations eagerly
 *    for this specific method, without permanently changing FetchType to EAGER in the entity.
 * 2. `JOIN FETCH` in JPQL / HQL:
 *    e.g., `@Query("SELECT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.product WHERE o.id = :id")`
 */
@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = ["items", "items.product", "user"])
    fun findByOrderNumber(orderNumber: String): Optional<Order>

    @EntityGraph(attributePaths = ["items", "items.product", "user"])
    fun findWithDetailsById(id: Long): Optional<Order>

    @EntityGraph(attributePaths = ["items", "items.product", "user"])
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Order>
}
