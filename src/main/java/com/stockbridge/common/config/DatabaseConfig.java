package com.stockbridge.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url != null) {
            if (url.startsWith("postgres://")) {
                String jdbcUrl = "jdbc:postgresql://" + url.substring("postgres://".length());
                log.info("Sanitized Render postgres:// database URL to standard JDBC format.");
                properties.setUrl(jdbcUrl);
            } else if (url.startsWith("postgresql://")) {
                String jdbcUrl = "jdbc:" + url;
                log.info("Sanitized Render postgresql:// database URL to standard JDBC format.");
                properties.setUrl(jdbcUrl);
            }
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
