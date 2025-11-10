package com.pradeepit.pit_client_service.repository;

import com.pradeepit.pit_client_service.model.TrashFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrashFileRepository extends MongoRepository<TrashFile, String> {
    void deleteByTrashPath(String trashPathString);

    List<TrashFile> findAllByMovedToTrashAtBefore(LocalDateTime cutoffTime);
}
