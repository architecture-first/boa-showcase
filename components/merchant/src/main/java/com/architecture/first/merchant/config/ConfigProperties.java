package com.architecture.first.merchant.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigProperties {

    @Value("${data.database.connectionString}")
    private String dbConnectionString;

    @Value("${data.database.name}")
    private String databaseName;

    @Bean
    public MongoDatabase mongoDatabase() {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(dbConnectionString));
        CodecRegistry pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        return mongoClient.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
    }
}
