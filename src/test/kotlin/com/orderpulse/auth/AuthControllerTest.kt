package com.orderpulse.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.orderpulse.security.JwtAuthenticationEntryPoint
import com.orderpulse.security.JwtAuthenticationFilter
import com.orderpulse.security.JwtTokenProvider
import com.orderpulse.user.Role
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * Slice test for AuthController using @WebMvcTest and SpringMockK.
 *
 * Interview Tip:
 * @WebMvcTest isolates the Spring MVC infrastructure:
 * - Only instantiates controllers, filters, advice, and Jackson mappers.
 * - Dependencies (like AuthService) are mocked using @MockkBean.
 * - Fast feedback cycle for HTTP status codes, headers, and request body validation.
 */
@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    @DisplayName("Should return 201 Created when register request is valid")
    fun shouldRegisterSuccessfully() {
        // Given
        val request = RegisterRequest(
            email = "jane.doe@example.com",
            password = "securePassword123",
            fullName = "Jane Doe"
        )
        val response = AuthResponse(
            accessToken = "mock-jwt-token",
            user = UserSummaryResponse(
                id = 2L,
                email = "jane.doe@example.com",
                fullName = "Jane Doe",
                role = Role.ROLE_CUSTOMER
            )
        )

        every { authService.register(any()) } returns response

        // When / Then
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accessToken") { value("mock-jwt-token") }
            jsonPath("$.user.email") { value("jane.doe@example.com") }
            jsonPath("$.user.fullName") { value("Jane Doe") }
        }
    }

    @Test
    @DisplayName("Should return 400 Bad Request with ProblemDetail when email is invalid")
    fun shouldReturnBadRequestForInvalidEmail() {
        // Given invalid email format
        val invalidRequest = RegisterRequest(
            email = "not-an-email",
            password = "securePassword123",
            fullName = "Jane Doe"
        )

        // When / Then
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidRequest)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.title") { value("Invalid Request Body") }
            jsonPath("$.invalid_fields.email") { exists() }
        }
    }
}
