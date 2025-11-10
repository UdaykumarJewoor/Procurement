package com.pradeepit.pit_client_service.repository;

import com.pradeepit.pit_client_service.model.SpocDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpocDetailsRepository extends MongoRepository<SpocDetail, String> {

    @Query("{ 'clientId' : { $regex: ?0, $options: 'i' } }")
    List<SpocDetail> findByClientIdIgnoreCase(String clientId);

    void deleteByClientId(String id);

    @Query("{ 'id' : { $regex: ?0, $options: 'i' } }")
    Optional<SpocDetail> findByIdIgnoreCase(String id);

}
