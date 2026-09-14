package com.orderpulse.product

import com.orderpulse.common.ResourceNotFoundException
import com.orderpulse.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository
) {
    private val log = LoggerFactory.getLogger(ProductService::class.java)

    @Transactional(readOnly = true)
    fun getAllProducts(search: String?, pageable: Pageable): Page<ProductResponse> {
        val page = if (!search.isNullOrBlank()) {
            productRepository.findByNameContainingIgnoreCase(search.trim(), pageable)
        } else {
            productRepository.findAll(pageable)
        }
        return page.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.PRODUCT_DETAILS_CACHE], key = "#id")
    fun getProductResponseById(id: Long): ProductResponse {
        log.debug("Cache miss: Fetching product {} from database", id)
        return getProductEntity(id).toResponse()
    }

    @Transactional(readOnly = true)
    fun getProductEntity(id: Long): Product {
        return productRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Product not found with id: $id")
        }
    }

    @Transactional
    @CacheEvict(value = [CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_DETAILS_CACHE], allEntries = true)
    fun createProduct(request: CreateProductRequest): ProductResponse {
        val product = Product(
            name = request.name.trim(),
            description = request.description?.trim(),
            price = request.price,
            stockQuantity = request.stockQuantity
        )
        val saved = productRepository.save(product)
        log.info("Created product [id={}]: {}", saved.id, saved.name)
        return saved.toResponse()
    }

    @Transactional
    @CacheEvict(value = [CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_DETAILS_CACHE], allEntries = true)
    fun updateProduct(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = getProductEntity(id)
        product.name = request.name.trim()
        product.description = request.description?.trim()
        product.price = request.price
        product.stockQuantity = request.stockQuantity

        val updated = productRepository.save(product)
        log.info("Updated product [id={}]: {}", updated.id, updated.name)
        return updated.toResponse()
    }

    @Transactional
    @CacheEvict(value = [CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_DETAILS_CACHE], allEntries = true)
    fun deleteProduct(id: Long) {
        val product = getProductEntity(id)
        productRepository.delete(product)
        log.info("Deleted product [id={}]: {}", id, product.name)
    }

    /**
     * Stock deduction protected by Optimistic Locking (@Version in Product entity).
     */
    @Transactional
    @CacheEvict(value = [CacheConfig.PRODUCT_DETAILS_CACHE], key = "#id")
    fun deductProductStock(id: Long, quantity: Int): Product {
        val product = getProductEntity(id)
        product.deductStock(quantity)
        return productRepository.save(product)
    }
}
