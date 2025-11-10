package com.pradeepit.pit_auth_service.repository;

import com.pradeepit.pit_auth_service.model.PermissionHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionHistoryRepository extends MongoRepository<PermissionHistory, String> {
}
