package com.procurement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
@Data
public class  ResumeParseResponse {
    private ResumeData data;

    // mirror response keys from Affinda API
    @Data
    public static class ResumeData {
        @JsonProperty("name")
        private Name name;
        @JsonProperty("emails")
        private List<String> emails;  // Email list
        @JsonProperty("phoneNumbers")
        private List<String> phoneNumbers;  // Phone number list
        private List<Skill> skills;
    }
    @Data
    public static class Skill {
        private String name;
    }
    @Data
    public static class Name {
        private String raw;
    }
}




//        private String first;
//        private String last;
//        private String middle;
//        private String title;