package com.stockbridge.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads .env and .env.local files from the project root into Spring Environment properties.
 * Enables simple, unified local configuration matching the frontend .env pattern.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> envProperties = new HashMap<>();

        File[] candidateFiles = new File[] {
                new File(".env"),
                new File(".env.local"),
                new File("StockBridgeBack/.env"),
                new File("StockBridgeBack/.env.local")
        };

        for (File file : candidateFiles) {
            if (file.exists() && file.isFile()) {
                loadFileIntoMap(file, envProperties);
            }
        }

        if (!envProperties.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("stockbridgeDotenvProperties", envProperties)
            );
        }
    }

    private void loadFileIntoMap(File file, Map<String, Object> map) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String rawLine : lines) {
                String line = rawLine.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();

                    // Strip wrapping double or single quotes
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    map.put(key, value);
                }
            }
        } catch (IOException ignored) {
            // Ignore if file cannot be read
        }
    }
}
