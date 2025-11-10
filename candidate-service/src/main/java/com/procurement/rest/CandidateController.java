package com.procurement.rest;

import com.procurement.model.document.Candidate;
import com.procurement.service.CandidateService;
import com.procurement.service.ResumeParserService;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;
    @Autowired
    private ResumeParserService resumeParserService;

    @PostMapping("/upload")  // create candidate
    public ResponseEntity<Candidate> uploadResume(@RequestParam("file") MultipartFile resume) {
        try {
            Candidate candidate = candidateService.createCandidate(resume);

            return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
        } catch (IOException | TikaException | SAXException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")  // get candidate by mongodbID
    public ResponseEntity<Candidate> getCandidateById(@PathVariable String id) {
        Candidate candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(candidate);
    }

    @GetMapping
    public ResponseEntity<List<Candidate>> getAll() {
        return ResponseEntity.ok(candidateService.getAllCandidates());
    }

    @PatchMapping("/{id}/status")  // update candidate status
    public ResponseEntity<Candidate> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ResponseEntity.ok(candidateService.updateStatus(id, status));
    }

    @PutMapping("/{id}") // update candidate details
    public ResponseEntity<Candidate> updateCandidate(@PathVariable String id, @RequestBody Candidate updatedCandidate) {
        Candidate candidate = candidateService.updateCandidate(id, updatedCandidate);
        return ResponseEntity.ok(candidate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCandidate(@PathVariable String id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.status(HttpStatus.OK).body("Candidate with ID " + id + " deleted successfully.");
    }


}
