# Corporate Action Trade Lifecycle - Java Backend

This repository contains the Java-based backend for the Corporate Action Trade Lifecycle system. It is designed with **Proper Enterprise Architecture** principles, utilizing **Hexagonal Architecture (Ports and Adapters)** and **Domain-Driven Design (DDD)** to ensure a decoupled, resilient, and maintainable core.

## **Technical Stack**
- **Java 21**: Leveraging modern language features.
- **Spring Boot 3.3+**: Utilizing `RestClient` for modern HTTP interactions.
- **Spring Data JPA & H2**: For persistence with a rich domain model.
- **Resilience4j**: Implementing Circuit Breaker and Retry patterns for AI reasoning agent boundaries.
- **Maven**: Project lifecycle and dependency management.

## **Architecture Overview**
The project follows Hexagonal Architecture to strictly isolate core business logic from external infrastructure and agents:

- **Domain Layer** (`com.demo.ca.domain`): Contains the rich domain model, aggregate roots (e.g., `CorporateAction`), and domain service interfaces. Enforces domain invariants and encapsulates state.
- **Application Layer** (`com.demo.ca.application`): Defines ports (interfaces) and use cases. Orchestrates the flow between the domain and infrastructure layers.
- **Infrastructure Layer** (`com.demo.ca.infrastructure`): Implements adapters for persistence (JPA), external AI agent communication (`RestClient`), and resilience configurations.
- **Web Layer** (`com.demo.ca.web`): REST controllers providing an API for the frontend, following RFC 7807 `ProblemDetail` for standardized error handling.

## **Key Features**
- **Rich Domain Model**: Avoids anemic domain models by encapsulating business rules within entities.
- **Resilient AI Integration**: Communicates with a Python-based AI Reasoning Agent via `AIAgentClient`, protected by Resilience4j circuit breakers and retries.
- **Hexagonal Isolation**: The domain layer has zero dependencies on external frameworks or libraries (other than standard Java).
- **Modern HTTP**: Replaces legacy `RestTemplate` with the modern Spring `RestClient`.
- **Short Transactions**: OSIV (Open-Session-In-View) is disabled to ensure transactional integrity and prevent performance bottlenecks.

## **Development**

### **Prerequisites**
- JDK 21+
- Maven 3.9+

### **Running the Application**
```bash
mvn spring-boot:run
```
The application will be available at `http://localhost:8080`.

### **Testing**
The project follows a rigorous testing strategy including unit, integration, and persistence tests.
```bash
mvn test
```

### **API Documentation**
The API follows RESTful principles and returns standardized error responses. Key endpoints:
- `GET /api/ca`: List corporate actions.
- `POST /api/ca/analyze`: Trigger AI analysis for a new corporate action.
- `POST /api/ca/{id}/approve`: Approve a pending trade execution (requires Human-in-the-Loop).

## **Project Structure**
- `src/main/java/com/demo/ca/domain`: Core business logic and aggregate roots.
- `src/main/java/com/demo/ca/application`: Ports and use cases.
- `src/main/java/com/demo/ca/infrastructure`: Adapters and configuration.
- `src/main/java/com/demo/ca/web`: REST endpoints and exception handling.
- `src/main/resources/application.properties`: Configuration for persistence, resilience, and AI endpoints.
