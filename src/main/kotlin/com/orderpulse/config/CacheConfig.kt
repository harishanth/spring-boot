package com.orderpulse.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * High-performance in-memory caching with Caffeine.
 *
 * Interview Tip:
 * Caffeine is the spiritual successor to Google Guava Cache, offering near-optimal hit ratios
 * using the Window TinyLFU eviction policy. In interviews, explain when to use in-memory caching (Caffeine)
 * vs distributed caching (Redis):
 * - Caffeine: Sub-millisecond latency, zero network overhead, ideal for read-heavy static/reference data.
 * - Redis: Distributed cache shared across horizontal replicas, prevents cache divergence.
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val PRODUCTS_CACHE = "products"
        const val PRODUCT_DETAILS_CACHE = "product-details"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager(PRODUCTS_CACHE, PRODUCT_DETAILS_CACHE)
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
        )
        return cacheManager
    }
}
