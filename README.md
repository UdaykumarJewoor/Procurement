# Procurement

## Description
The Procurement project is a Spring Boot–based service procurement system. It manages candidate information, resume parsing, job description matching, and authentication using a modular microservices architecture.

## Project Structure
- **candidate-service:** Handles candidate data, resume parsing, and JD-skill matching.
- **pit-auth-service:** Manages user authentication and authorization.
- **ProcurementApplication.java:** Main entry point of the Spring Boot application.
- **Gradle Wrapper Files:** Used for consistent build and project setup.

## Technology Stack
- Spring Boot  
- Gradle  
- MongoDB (or any NoSQL database)  
- REST APIs  
- External API integration (Affinda for resume parsing)

## Purpose
This system provides a platform for managing candidates, parsing resumes, matching skills with job descriptions, and handling authentication.

## How to Run
1. Clone the repository  
   ```bash
   git clone https://github.com/<your-username>/Procurement.git
Build the project

bash
Copy code
./gradlew clean build
Run the application

bash
Copy code
./gradlew bootRun
Author
Udaykumar Jewoor

yaml
Copy code

---

Would you like me to make it auto-detect both services (`candidate-service` and `pit-auth-service`) with commands for each in the “How to Run” section?
