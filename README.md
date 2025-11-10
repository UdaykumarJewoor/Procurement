# Procurement

The Procurement project is a Spring Boot–based service procurement system designed with a modular architecture. It manages candidate information, resume parsing, job description matching, and authentication. The project follows a microservices approach with separate modules for different functionalities.

It includes two main services: 
1. **candidate-service** – Handles candidate data management, resume parsing using the Affinda API, and JD-skill matching.  
2. **pit-auth-service** – Manages user authentication and authorization.

The project uses Spring Boot, Gradle, MongoDB (or another NoSQL database), and REST APIs for communication between services. It provides an end-to-end platform for managing candidates, parsing resumes, matching skills to job descriptions, and securing access through authentication.

Main class: **ProcurementApplication.java**  
Build tool: **Gradle**  
External Integration: **Affinda API (for resume parsing)**  

Developed by **Udaykumar Jewoor**
