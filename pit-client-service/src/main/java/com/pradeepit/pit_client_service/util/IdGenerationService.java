package com.pradeepit.pit_client_service.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class IdGenerationService {

    @Autowired
    private MongoTemplate mongoTemplate;
    private final Random random = new Random();

    private String generateId(String type, String prefix) {
        Query query = new Query(Criteria.where("_id").is(type)); // Use _id to match MongoDB conventions
        Update update = new Update().inc("counter", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        IdCounter counter = mongoTemplate.findAndModify(query, update, options, IdCounter.class);

        assert counter != null;
        int newCounterValue = counter.getCounter();
        return String.format("%s%02d", prefix, newCounterValue);
    }


    public String generateClientId() {
        return generateId("Clients", "CLPIT");
    }

    public String generateSpocId() {
        return generateId("Spocs", "SPOCPIT");
    }

    public String generateTrashFileId() {
        return generateId("Trash", "TRPIT");
    }
}
