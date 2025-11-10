package com.procurement.client;

import com.procurement.model.ResumeParseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
@FeignClient(name = "affindaApiClient", url = "https://api.affinda.com/v1")
public interface AffindaApiClient {

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ResumeParseResponse> parseResume(
            @RequestHeader("Authorization") String apiKey,
            @RequestPart("file") MultipartFile file);
}


