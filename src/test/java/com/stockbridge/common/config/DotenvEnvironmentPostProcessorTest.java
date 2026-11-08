package com.stockbridge.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DotenvEnvironmentPostProcessorTest {

    @Test
    @DisplayName("Dotenv processor parses comments, unquotes values, and loads keys into map")
    void testDotenvProcessorLoadsKeys() throws IOException {
        File tempEnv = File.createTempFile("test", ".env");
        tempEnv.deleteOnExit();

        Files.writeString(tempEnv.toPath(),
                "# Header Comment\n" +
                "BACKEND_API_KEY=dev-secret-key-999\n" +
                "QUOTED_KEY=\"hello-world\"\n" +
                "EMPTY_LINE_BELOW\n\n" +
                "SINGLE_QUOTED='single-value'\n"
        );

        DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();
        Map<String, Object> map = new HashMap<>();

        ReflectionTestUtils.invokeMethod(processor, "loadFileIntoMap", tempEnv, map);

        assertEquals("dev-secret-key-999", map.get("BACKEND_API_KEY"));
        assertEquals("hello-world", map.get("QUOTED_KEY"));
        assertEquals("single-value", map.get("SINGLE_QUOTED"));
    }
}
