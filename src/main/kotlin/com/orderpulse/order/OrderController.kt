package com.orderpulse.order

import com.orderpulse.security.UserPrincipal
import com.orderpulse.user.Role
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Orders", description = "Order placement, payment orchestration, and status management")
@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "Bearer Authentication")
class OrderController(
    private val orderService: OrderService
) {

    @Operation(summary = "Place a new order", description = "Deducts stock optimistically, executes payment strategy, and records order metrics.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun placeOrder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val order = orderService.placeOrder(principal.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @Operation(summary = "Get current customer orders", description = "Returns a paginated list of orders placed by the authenticated customer.")
    @GetMapping("/my-orders")
    fun getMyOrders(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<OrderResponse>> {
        return ResponseEntity.ok(orderService.getOrdersForUser(principal.id, pageable))
    }

    @Operation(summary = "Get order by ID", description = "Accessible by the order owner or users with ROLE_ADMIN.")
    @GetMapping("/{id}")
    fun getOrderById(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<OrderResponse> {
        val userRole = Role.valueOf(principal.authorities.first().authority)
        val order = orderService.getOrderById(id, principal.id, userRole)
        return ResponseEntity.ok(order)
    }

    @Operation(summary = "Get all orders (Admin only)", description = "Returns all system orders paginated.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllOrders(
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<OrderResponse>> {
        return ResponseEntity.ok(orderService.getAllOrders(pageable))
    }

    @Operation(summary = "Update order status (Admin only)", description = "Updates order status (e.g. SHIPPED, CANCELLED).")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateOrderStatus(
        @PathVariable id: Long,
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderResponse> {
        val updated = orderService.updateOrderStatus(id, status)
        return ResponseEntity.ok(updated)
    }
}
