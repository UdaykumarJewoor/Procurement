package com.procurement.service;

import com.procurement.client.AffindaApiClient;
import com.procurement.model.ResumeParseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeParserService {

    @Autowired
    private AffindaApiClient affindaApiClient;

    private static final String API_KEY = "aff_7d13e910abc25ffb3cf2323a461643c326a3ea75";  // Affinda API Key

    // Method to extract details from the resume using Affinda API
    public ResumeParseResponse extractResumeDetails(MultipartFile file) {
        // Pass the API key and file to Affinda API
        ResponseEntity<ResumeParseResponse> response = affindaApiClient.parseResume("Bearer " + API_KEY, file);
        System.out.println("Raw response: " + response.getBody());
        return response.getBody();  // Return parsed details from Affinda API
    }
}
