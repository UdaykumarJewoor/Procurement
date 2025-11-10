package com.pradeepit.pit_auth_service.repository;

import com.pradeepit.pit_auth_service.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    Optional<Role> findByNameIgnoreCase(String name);

    @Query("{ 'id' : { $regex: '^?0$', $options: 'i' } }")
    Optional<Role> findByIdIgnoreCase(String id);

}
