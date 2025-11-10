# Procurement

## Description
The Procurement project is a Spring Boot–based service procurement system designed with a modular architecture. It manages candidate information, resume parsing, job description matching, and authentication. The project follows a microservices approach with separate modules for different functionalities.

---

## Project Structure

### 1. candidate-service
- Handles candidate data management, resume parsing, and JD-skill matching.
- **CandidateController.java**: Defines REST endpoints for candidate operations.
- **CandidateService.java**: Contains business logic for candidate management.
- **ResumeParserService.java**: Connects to an external API (Affinda) to extract structured data from resumes.
- **application.yml**: Holds service and API configuration details.

### 2. pit-auth-service
- Manages user authentication and authorization services.

### 3. ProcurementApplication.java
- Main entry point for the Spring Boot application.

### 4. Gradle Wrapper Files
- Ensure consistent build and execution across different environments.

---

## Technology Stack
- Spring Boot  
- Gradle  
- MongoDB or another NoSQL database  
- REST APIs  
- External API Integration (Affinda for resume parsing)

---

## Purpose
This system provides an end-to-end platform for service procurement, primarily focusing on candidate management.  
It enables storing and parsing resumes, matching candidate skills to job descriptions, and securing services through authentication.

---

## How to Run
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/Procurement.git
