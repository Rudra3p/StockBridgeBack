package com.stockbridge.config;

import com.stockbridge.listings.repository.ListingRepository;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@Profile("test")
public class TestMongoConfig {

    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        return Mockito.mock(MongoTemplate.class);
    }

    @Bean
    @Primary
    public ListingRepository listingRepository() {
        return Mockito.mock(ListingRepository.class);
    }
}
