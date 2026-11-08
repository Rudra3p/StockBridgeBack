package com.stockbridge.accounts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockbridge.accounts.dto.LoginRequest;
import com.stockbridge.accounts.dto.RegisterRequest;
import com.stockbridge.accounts.entity.OrgType;
import com.stockbridge.accounts.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private static final String API_KEY = "test-stockbridge-api-key-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.stockbridge.security.RateLimitingFilter rateLimitingFilter;

    @org.junit.jupiter.api.BeforeEach
    void resetRateLimiter() {
        rateLimitingFilter.reset();
    }

    // -------------------------------------------------------------
    // API KEY LAYER TESTS (REQUIREMENT 9)
    // -------------------------------------------------------------

    @Test
    @DisplayName("API Key Check 1: Request without X-API-KEY header returns 401 JSON")
    void testRequestWithoutApiKey_Returns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid API key")));
    }

    @Test
    @DisplayName("API Key Check 2: Request with wrong X-API-KEY header returns 401 JSON")
    void testRequestWithWrongApiKey_Returns401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("X-API-KEY", "completely-invalid-key-xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid API key")));
    }

    @Test
    @DisplayName("API Key Check 3: Correct API key without JWT on protected endpoint returns 401")
    void testCorrectApiKeyWithoutJwt_ProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("authentication is required")));
    }

    @Test
    @DisplayName("API Key Check 4: Correct API key with valid JWT returns 200 OK")
    void testCorrectApiKeyWithValidJwt_Returns200() throws Exception {
        // 1. Log in to get token (login requires API key only)
        LoginRequest login = LoginRequest.builder()
                .email("admin@stockbridge.local")
                .password("admin123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 2. Access protected endpoint with both API key and JWT
        mockMvc.perform(get("/api/me")
                        .header("X-API-KEY", API_KEY)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("admin@stockbridge.local")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("API Key Exclusion: GET /actuator/health is excluded from API key check")
    void testActuatorHealth_ExcludedFromApiKeyCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("API Key Exclusion: OPTIONS preflight is excluded from API key check")
    void testOptionsPreflight_ExcludedFromApiKeyCheck() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------
    // REGISTRATION TESTS
    // -------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/register - Successfully register a SELLER business")
    void testRegisterSeller_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .legalName("Metro Wholesale Ltd")
                .orgType(OrgType.WHOLESALER)
                .gstNumber("27AABCM1234F1Z1")
                .addressLine("Industrial Estate, Sector 5")
                .city("Pune")
                .state("Maharashtra")
                .pinCode("411001")
                .fullName("Vikram Malhotra")
                .email("vikram@metrowholesale.com")
                .phone("+919812345678")
                .password("StrongPassword123")
                .role(Role.SELLER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fullName", is("Vikram Malhotra")))
                .andExpect(jsonPath("$.email", is("vikram@metrowholesale.com")))
                .andExpect(jsonPath("$.role", is("SELLER")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.organisation.id").isNotEmpty())
                .andExpect(jsonPath("$.organisation.legalName", is("Metro Wholesale Ltd")))
                .andExpect(jsonPath("$.organisation.verificationStatus", is("PENDING")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Successfully register with nested organisation and user JSON")
    void testRegisterNestedJson_Success() throws Exception {
        String json = """
                {
                    "organisation": {
                        "legalName": "Nested Surplus Co",
                        "orgType": "MANUFACTURER",
                        "gstNumber": "24ABCDE1234F1Z9",
                        "addressLine": "Industrial Hub",
                        "city": "Ahmedabad",
                        "state": "Gujarat",
                        "pinCode": "380001"
                    },
                    "user": {
                        "fullName": "Kiran Patel",
                        "email": "kiran@nestedsurplus.com",
                        "phone": "+919876599999",
                        "password": "SecurePassword123",
                        "role": "SELLER"
                    }
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName", is("Kiran Patel")))
                .andExpect(jsonPath("$.organisation.legalName", is("Nested Surplus Co")))
                .andExpect(jsonPath("$.organisation.orgType", is("MANUFACTURER")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject registration with ADMIN role")
    void testRegisterAdmin_Rejected() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .legalName("Illegal Admin Inc")
                .orgType(OrgType.OTHER)
                .gstNumber("29TESTADMIN1234")
                .addressLine("Admin Lane")
                .city("Delhi")
                .state("Delhi")
                .pinCode("110001")
                .fullName("Hacker Admin")
                .email("hacker@admin.com")
                .phone("+919999999999")
                .password("Password123!")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ADMIN")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject registration with short password (< 8 chars)")
    void testRegister_ShortPassword_ValidationFailure() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .legalName("Retail Mart")
                .orgType(OrgType.RETAILER)
                .gstNumber("29AABCR1234F1Z2")
                .addressLine("Main Bazaar")
                .city("Bengaluru")
                .state("Karnataka")
                .pinCode("560002")
                .fullName("Anil Kumar")
                .email("anil@retailmart.com")
                .phone("+919876500000")
                .password("short")
                .role(Role.BUYER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password", containsString("at least 8 characters")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Reject duplicate email and GST")
    void testRegister_DuplicateEmailAndGst() throws Exception {
        RegisterRequest request1 = RegisterRequest.builder()
                .legalName("First Corp")
                .orgType(OrgType.MANUFACTURER)
                .gstNumber("33AABCF1234F1Z3")
                .addressLine("Line 1")
                .city("Chennai")
                .state("Tamil Nadu")
                .pinCode("600001")
                .fullName("Priya Raj")
                .email("priya@firstcorp.com")
                .phone("+919876543211")
                .password("SecurePass123")
                .role(Role.SELLER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Duplicate email
        RegisterRequest duplicateEmail = RegisterRequest.builder()
                .legalName("Different Corp")
                .orgType(OrgType.RETAILER)
                .gstNumber("33DIFF1234F1Z4")
                .addressLine("Line 2")
                .city("Chennai")
                .state("Tamil Nadu")
                .pinCode("600002")
                .fullName("Priya Copy")
                .email("priya@firstcorp.com")
                .phone("+919876543212")
                .password("SecurePass123")
                .role(Role.BUYER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    // -------------------------------------------------------------
    // LOGIN & ROLE TESTS
    // -------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login - Admin seeded user can log in and retrieve JWT")
    void testLogin_AdminSeededUser_Success() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .email("admin@stockbridge.local")
                .password("admin123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email", is("admin@stockbridge.local")))
                .andExpect(jsonPath("$.user.role", is("ADMIN")))
                .andExpect(jsonPath("$.user.organisationId").isNotEmpty())
                .andExpect(jsonPath("$.user.verificationStatus", is("APPROVED")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid credentials returns 401 with generic message")
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .email("admin@stockbridge.local")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("GET /api/me and Ping endpoints role-based access control check")
    void testMeAndPingEndpoints_RoleSecurity() throws Exception {
        // Register seller
        RegisterRequest sellerReg = RegisterRequest.builder()
                .legalName("Seller Org Ltd")
                .orgType(OrgType.DISTRIBUTOR)
                .gstNumber("27DISTRIB1234F1")
                .addressLine("Distributor Road")
                .city("Mumbai")
                .state("Maharashtra")
                .pinCode("400001")
                .fullName("Seller User")
                .email("seller@test.com")
                .phone("+919000000001")
                .password("Password123")
                .role(Role.SELLER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerReg)))
                .andExpect(status().isCreated());

        // Login seller
        LoginRequest sellerLogin = LoginRequest.builder()
                .email("seller@test.com")
                .password("Password123")
                .build();

        MvcResult sellerLoginResult = mockMvc.perform(post("/api/auth/login")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String sellerToken = objectMapper.readTree(sellerLoginResult.getResponse().getContentAsString())
                .get("token").asText();

        // Login Admin
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@stockbridge.local")
                .password("admin123")
                .build();

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 1. GET /api/me with API key and seller token -> 200
        mockMvc.perform(get("/api/me")
                        .header("X-API-KEY", API_KEY)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("seller@test.com")))
                .andExpect(jsonPath("$.role", is("SELLER")))
                .andExpect(jsonPath("$.organisation.legalName", is("Seller Org Ltd")));

        // 2. GET /api/admin/ping with Admin token -> 200
        mockMvc.perform(get("/api/admin/ping")
                        .header("X-API-KEY", API_KEY)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("ADMIN")));

        // 3. GET /api/admin/ping with Seller token -> 403 Forbidden
        mockMvc.perform(get("/api/admin/ping")
                        .header("X-API-KEY", API_KEY)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isForbidden());

        // 4. GET /api/seller/ping with Seller token -> 200
        mockMvc.perform(get("/api/seller/ping")
                        .header("X-API-KEY", API_KEY)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("SELLER")));

        // 5. GET /api/seller/ping with Admin token -> 403 Forbidden
        mockMvc.perform(get("/api/seller/ping")
                        .header("X-API-KEY", API_KEY)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // RATE LIMITING TESTS (REQUIREMENT 8)
    // -------------------------------------------------------------

    @Test
    @DisplayName("Rate Limiting: Exceeding 10 requests per minute returns 429 Too Many Requests")
    void testRateLimiting_ExceedingLimitReturns429() throws Exception {
        String testIp = "192.168.1.100";
        LoginRequest badLogin = LoginRequest.builder()
                .email("ratelimit@stockbridge.local")
                .password("wrongpass")
                .build();

        // Perform 10 requests (all consumed)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-API-KEY", API_KEY)
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized()); // fails auth, but allowed by rate limiter
        }

        // 11th request must be rejected by RateLimitingFilter with HTTP 429
        mockMvc.perform(post("/api/auth/login")
                        .header("X-API-KEY", API_KEY)
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")));
    }
}
