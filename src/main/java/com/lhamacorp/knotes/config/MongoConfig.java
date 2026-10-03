package com.lhamacorp.knotes.config;

import com.lhamacorp.knotes.client.SecretsClient;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
public class MongoConfig {

    private record MongoSecret(String connection) {
    }

    @Value("${secrets.mongo}")
    private String mongoSecretName;

    @Bean
    public MongoClient mongoClient(SecretsClient secrets) {
        String uri = secrets.fetch(mongoSecretName, MongoSecret.class).connection();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(15)
                        .minSize(0)
                        .maxConnectionIdleTime(60, SECONDS)
                        .maxWaitTime(5, SECONDS))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "knotes");
    }

}