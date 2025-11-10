package com.pradeepit.pit_auth_service.repository;

import com.pradeepit.pit_auth_service.model.Customer;
import com.pradeepit.pit_auth_service.model.enums.StatusEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
    @Query("{ '_id' : { $regex: ?0, $options: 'i' } }")
    Optional<Customer> findByIdIgnoreCase(String customerId);

    List<Customer> findAllByIsFrozen(boolean frozen);

    List<Customer> findAllByOtpIsNotNull();

    @Query("{ 'customerId': ?0 }")
    StatusEnum findStatusByCustomerId(String customerId);

    List<Customer> findByRoleNameIgnoreCase(String currentUserRole);
}
