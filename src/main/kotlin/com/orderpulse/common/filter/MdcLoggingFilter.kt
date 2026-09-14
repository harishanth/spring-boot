package com.orderpulse.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Filter that attaches a correlation/trace ID to the logging MDC (Mapped Diagnostic Context)
 * and propagates it via the 'X-Trace-Id' response header.
 *
 * Interview Tip:
 * In distributed microservices, correlation IDs allow engineers to trace a single request
 * across multiple services, log aggregators (ELK, Loki), and exception handlers.
 * Crucially, MDC context is thread-local, so it must be cleaned up in a finally block!
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcLoggingFilter : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_KEY = "traceId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        MDC.put(TRACE_ID_KEY, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TRACE_ID_KEY)
        }
    }
}
