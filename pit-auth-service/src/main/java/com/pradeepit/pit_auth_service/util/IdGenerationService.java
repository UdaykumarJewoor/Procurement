package com.pradeepit.pit_auth_service.util;


import com.pradeepit.pit_auth_service.model.Customer;
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

    public String generateCustomerId() {
        return generateId("Customers", "CUPIT");
    }

    public String generateRoleId() {
        return generateId("Roles", "RSPIT");
    }

    public String generatePermissionId() {
        return generateId("Permissions", "PPIT");
    }

    public String generatePermissionHistoryId() {
        return generateId("PermissionHistory", "PHPIT");
    }

    public String generateCustomerNumber(String role) {
        String prefix;

        switch (role) {
            case "RECRUITER":
                prefix = "HRPIT";
                break;
            case "LEAD-HR":
                prefix = "LHRPIT";
                break;
            case "CLIENT":
                prefix = "CLPIT";
                break;
            case "ADMIN":
                prefix = "MGRPIT";
                break;
            case "GUEST-USER":
                prefix = "VIPIT";
                break;
            case "MASTER-ADMIN":
                prefix = "MAPIT";
                break;
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }

        return generateUniqueNumber(prefix);
    }

    private String generateUniqueNumber(String prefix) {
        int randomNum;
        String customerNo;
        Query query;

        do {
            randomNum = 1000 + random.nextInt(9000); // Generate a 4-digit random number
            customerNo = prefix + randomNum;
            query = new Query(Criteria.where("customerNo").is(customerNo));
        } while (mongoTemplate.findOne(query, Customer.class) != null); // Ensure uniqueness

        return customerNo;
    }

}
