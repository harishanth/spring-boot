package com.orderpulse.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Custom Micrometer metrics component for Actuator / Prometheus observability.
 *
 * Interview Tip:
 * Observability is a core topic in Senior/Lead backend interviews.
 * Exposing business-level metrics (orders placed, revenue, stock depletion) via MeterRegistry
 * allows Prometheus to scrape /actuator/prometheus and trigger PagerDuty alerts or Grafana dashboards.
 */
@Component
class OrderMetrics(
    private val meterRegistry: MeterRegistry
) {
    private val ordersPlacedCounter: Counter = Counter.builder("orderpulse.orders.placed.total")
        .description("Total number of orders successfully placed")
        .register(meterRegistry)

    private val ordersFailedCounter: Counter = Counter.builder("orderpulse.orders.failed.total")
        .description("Total number of orders that failed during checkout")
        .register(meterRegistry)

    fun incrementOrdersPlaced() {
        ordersPlacedCounter.increment()
    }

    fun incrementOrdersFailed() {
        ordersFailedCounter.increment()
    }

    fun recordOrderRevenue(amount: BigDecimal) {
        meterRegistry.counter("orderpulse.revenue.total").increment(amount.toDouble())
    }
}
