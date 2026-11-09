package com.stockbridge.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url != null && !url.isBlank()) {
            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(url);
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isEmpty()) {
                        String[] parts = userInfo.split(":", 2);
                        if (parts.length > 0 && !parts[0].isBlank()) {
                            String user = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                            properties.setUsername(user);
                        }
                        if (parts.length > 1 && !parts[1].isBlank()) {
                            String pass = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                            properties.setPassword(pass);
                        }
                    }

                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath() != null ? uri.getPath() : "";
                    String query = uri.getQuery() != null && !uri.getQuery().isBlank() ? "?" + uri.getQuery() : "";

                    // Supabase requires SSL
                    if (uri.getHost() != null && uri.getHost().contains("supabase") && !query.contains("sslmode")) {
                        query = query.isEmpty() ? "?sslmode=require" : query + "&sslmode=require";
                    }

                    String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path + query;
                    properties.setUrl(jdbcUrl);
                    log.info("Sanitized Render database URL to JDBC format for host: {}:{} (user: {})", uri.getHost(), port, properties.getUsername());
                } catch (Exception e) {
                    log.warn("Could not parse URI directly, falling back to prefix replacement: {}", e.getMessage());
                    if (url.startsWith("postgres://")) {
                        properties.setUrl("jdbc:postgresql://" + url.substring("postgres://".length()));
                    } else if (url.startsWith("postgresql://")) {
                        properties.setUrl("jdbc:" + url);
                    }
                }
            }
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
