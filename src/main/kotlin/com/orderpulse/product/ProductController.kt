package com.orderpulse.product

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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Products", description = "Product catalog management and inventory endpoints")
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productService: ProductService
) {

    @Operation(summary = "Browse products", description = "Public paginated catalog search by keyword.")
    @GetMapping
    fun getAllProducts(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, sort = ["id"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<Page<ProductResponse>> {
        return ResponseEntity.ok(productService.getAllProducts(search, pageable))
    }

    @Operation(summary = "Get product details", description = "Public endpoint with Caffeine caching.")
    @GetMapping("/{id}")
    fun getProductById(@PathVariable id: Long): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.getProductResponseById(id))
    }

    @Operation(
        summary = "Create product (Admin only)",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@Valid @RequestBody request: CreateProductRequest): ResponseEntity<ProductResponse> {
        val created = productService.createProduct(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @Operation(
        summary = "Update product (Admin only)",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.updateProduct(id, request))
    }

    @Operation(
        summary = "Delete product (Admin only)",
        security = [SecurityRequirement(name = "Bearer Authentication")]
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable id: Long): ResponseEntity<Unit> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}
