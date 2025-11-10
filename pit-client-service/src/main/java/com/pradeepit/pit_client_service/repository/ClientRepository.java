package com.pradeepit.pit_client_service.repository;

import com.pradeepit.pit_client_service.model.Clients;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends MongoRepository<Clients, String> {

    Optional<Clients> findByCompanyIgnoreCase(String company);

    @Query("{ '_id' : { $regex: ?0, $options: 'i' } }")
    Optional<Clients> findByIdIgnoreCase(String id);

    Optional<Clients> findByCustomerId(String customerId);
}
