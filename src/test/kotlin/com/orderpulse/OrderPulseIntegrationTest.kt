package com.orderpulse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.orderpulse.auth.LoginRequest
import com.orderpulse.auth.RegisterRequest
import com.orderpulse.order.CreateOrderRequest
import com.orderpulse.order.OrderItemRequest
import com.orderpulse.order.OrderStatus
import com.orderpulse.payment.PaymentMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * End-to-End Integration Test for OrderPulse.
 *
 * Covers the full business lifecycle:
 * 1. Register new customer
 * 2. Login to obtain JWT Bearer token
 * 3. Query product catalog
 * 4. Submit an order with authorization header
 * 5. Verify order status, stock deduction, and response structure
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderPulseIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @Test
    @DisplayName("Complete E2E Flow: Register -> Login -> Browse Products -> Place Order")
    fun testCompleteCustomerJourney() {
        val uniqueEmail = "integration.user.${System.currentTimeMillis()}@orderpulse.com"

        // 1. Register User
        val registerReq = RegisterRequest(
            email = uniqueEmail,
            password = "Password123!",
            fullName = "Integration Tester"
        )

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(registerReq)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accessToken") { exists() }
        }

        // 2. Login User to get JWT
        val loginReq = LoginRequest(
            email = uniqueEmail,
            password = "Password123!"
        )

        val loginResult = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginReq)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
        }.andReturn()

        val loginResponseJson: JsonNode = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginResponseJson.get("accessToken").asText()
        assertThat(token).isNotBlank()

        // 3. Browse Products
        val productsResult = mockMvc.get("/api/v1/products") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content") { isArray() }
        }.andReturn()

        val productsJson: JsonNode = objectMapper.readTree(productsResult.response.contentAsString)
        val firstProduct = productsJson.get("content").get(0)
        val productId = firstProduct.get("id").asLong()
        val initialStock = firstProduct.get("stockQuantity").asInt()

        // 4. Place Order
        val orderReq = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = productId, quantity = 1)),
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        val orderResult = mockMvc.post("/api/v1/orders") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(orderReq)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.orderNumber") { exists() }
            jsonPath("$.status") { value(OrderStatus.PAID.name) }
            jsonPath("$.items") { isArray() }
        }.andReturn()

        val orderJson: JsonNode = objectMapper.readTree(orderResult.response.contentAsString)
        val orderId = orderJson.get("id").asLong()

        // 5. Query Customer's Orders
        mockMvc.get("/api/v1/orders/my-orders") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value(orderId) }
        }

        // 6. Verify Product stock decremented
        mockMvc.get("/api/v1/products/$productId") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.stockQuantity") { value(initialStock - 1) }
        }
    }
}
