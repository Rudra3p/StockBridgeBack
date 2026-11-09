package com.stockbridge.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("!test")
@EnableMongoRepositories(basePackages = "com.stockbridge.listings.repository")
@EnableMongoAuditing
public class MongoConfig {
}
