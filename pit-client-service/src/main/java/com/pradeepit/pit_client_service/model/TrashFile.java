package com.pradeepit.pit_client_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "trash_files")
public class TrashFile {

    @Id
    private String id;
    private String fileName;
    private String fileType;
    private String originalPath;
    private String trashPath;
    private LocalDateTime movedToTrashAt;

}