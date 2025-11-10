package com.procurement.model.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Document(collection = "candidate_db")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private List<String> skills;
    private String status;

//

}
