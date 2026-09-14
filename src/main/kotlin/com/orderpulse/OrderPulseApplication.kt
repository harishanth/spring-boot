package com.orderpulse

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main entry point for the OrderPulse application.
 *
 * Interview Tip:
 * Notice that in Kotlin, top-level functions (like main) are compiled into a static method
 * in a generated class (e.g., OrderPulseApplicationKt).
 * Spring Boot 3 + Kotlin uses runApplication<OrderPulseApplication>(*args) which provides
 * an idiomatic, reified-type-parameter extension function over SpringApplication.run.
 */
@SpringBootApplication
class OrderPulseApplication

fun main(args: Array<String>) {
    runApplication<OrderPulseApplication>(*args)
}
