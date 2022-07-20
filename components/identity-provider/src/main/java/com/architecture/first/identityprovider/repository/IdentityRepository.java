package com.architecture.first.identityprovider.repository;

import com.architecture.first.framework.security.model.UserToken;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
public class IdentityRepository {
    private MongoClient mongoClient;
    private final MongoDatabase database;

    @Value("${data.database.connectionString}")
    private String dbConnectionString;

    @Value("${data.database.name}")
    private String databaseName;

    @Autowired
    public IdentityRepository(MongoDatabase mongoDatabase) {
        database = mongoDatabase;
    }

    public Optional<UserToken> findUserId(String username) {
        MongoCollection<UserToken> collection = database.getCollection("customers", UserToken.class);

        String filter = """
                {"emailAddress": ":username","isActive": true, "isRegistered": true},
                """;
        filter = filter.replace(":username", username);
        BasicDBObject bfilter = BasicDBObject.parse(filter);

        String projection = """
                    {_id: 0, "userId": 1, firstName: 1}
                """;
        BasicDBObject bProjection = BasicDBObject.parse(projection);

        return Optional.ofNullable(collection.find(bfilter).projection(bProjection).first());
    }
}
