package com.stockbridge.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyStartupFailureTest {

    @Test
    @DisplayName("Startup validation: ApiKeyAuthenticationFilter throws IllegalStateException if BACKEND_API_KEY is missing or empty")
    void testMissingApiKey_ThrowsIllegalStateException() {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter();

        ReflectionTestUtils.setField(filter, "configuredApiKey", null);
        assertThrows(IllegalStateException.class, filter::validateConfiguration);

        ReflectionTestUtils.setField(filter, "configuredApiKey", "   ");
        assertThrows(IllegalStateException.class, filter::validateConfiguration);
    }
}
