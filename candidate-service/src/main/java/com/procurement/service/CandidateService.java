package com.procurement.service;

import com.procurement.model.*;
import com.procurement.model.document.Candidate;
import com.procurement.repo.CandidateRepository;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CandidateService {

    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private ResumeParserService parserService;

    // set candidate details from uploaded resume by using Affinda API
    public Candidate createCandidate(MultipartFile resume) throws IOException, TikaException, SAXException {
        ResumeParseResponse.ResumeData data = parserService.extractResumeDetails(resume).getData();
        Candidate candidate = new Candidate();

        if (data.getName() != null) {
            // String fullName = Stream.of(data.getName().getTitle(), data.getName().getFirst(), data.getName().getMiddle(), data.getName().getLast())
            String fullName = Stream.of(data.getName().getRaw())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
            candidate.setName(fullName);
        }

        candidate.setEmail(data.getEmails() != null && !data.getEmails().isEmpty() ? data.getEmails().get(0) : null);
        candidate.setPhone(data.getPhoneNumbers() != null && !data.getPhoneNumbers().isEmpty() ? data.getPhoneNumbers().get(0) : null);
        candidate.setSkills(data.getSkills() != null ? data.getSkills().stream().map(ResumeParseResponse.Skill::getName).collect(Collectors.toList()) : Collections.emptyList());
        candidate.setStatus("New");  // initially set "NEw" candidate

        return candidateRepository.save(candidate);
    }

    public Candidate getCandidateById(String id) {
        return candidateRepository.findById(id).orElse(null);  // Returns null if not found
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    // Method to update candidate status
    public Candidate updateStatus(String id, String status) {
        Candidate candidate = candidateRepository.findById(id).orElseThrow();
        candidate.setStatus(status);
        return candidateRepository.save(candidate);
    }

    // update candidate keys
    public Candidate updateCandidate(String id, Candidate updatedCandidate) {
        Candidate existingCandidate = candidateRepository.findById(id).orElse(null);
        if (existingCandidate != null) {

            existingCandidate.setName(updatedCandidate.getName());
            existingCandidate.setEmail(updatedCandidate.getEmail());
            existingCandidate.setPhone(updatedCandidate.getPhone());
            existingCandidate.setSkills(updatedCandidate.getSkills());

            return candidateRepository.save(existingCandidate);
        }
        return existingCandidate;
    }

    public void deleteCandidate(String id) {
        candidateRepository.deleteById(id);
    }


    ///TODO       JD-skill matching algorithm  &    Candidate submission to JDs
}
