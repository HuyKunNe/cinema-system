The project follows a modern cloud-native microservices architecture with asynchronous communication and distributed transaction management.

### Architecture Patterns

- Microservices
- Event-Driven Architecture
- Database per Service
- Saga Pattern (Choreography)
- Outbox Pattern
- Idempotent Consumer Pattern
- CQRS (Search Service only)
- Eventual Consistency

# Technology Stack

- Java 21 LTS
- Spring Boot 4.0.7
- Spring Framework 7.x
- Spring Data JPA
- Hibernate 7
- Spring Validation
- Spring Scheduling
- Jackson
- JavaTimeModule
- ISO-8601 DateTime
- Maven Multi Module

## API

- Spring Cloud Gateway
- REST API
- OpenAPI 3
- Swagger UI

## Security

- Spring Security
- JWT Authentication
- OAuth2 Resource Server
- Refresh Token
- Role-Based Access Control (RBAC)
- Method Security 
- BCrypt Password Encoder
- CORS Configuration
- Rate Limiting

## Database

- MySQL 8.4 LTS
- Flyway

## Messaging

- Apache Kafka
- Retry Topic
- Dead Letter Topic (DLT)
- Batch Consumer
- Idempotent Consumer

## Cache & Distributed Lock

- Redis
- Redisson
- Distributed Lock
- Seat Reservation Lock

## Search

- Elasticsearch

## File Storage

- MinIO (Development)
- AWS S3 (Production abstraction)

## Logging

- SLF4J
- Logback
- MDC
- Correlation ID
- JSON Logging

## Distributed Tracing

- OpenTelemetry
- Micrometer Tracing
- TraceId
- SpanId
- Jaeger / Zipkin Export

## Monitoring

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana

## Validation

- Spring Validation
- Bean Validation

## Object Mapping

- MapStruct

## Exception Handling

- Global Exception Handler 

## Pagination

- Spring Data Pageable
- Page
- Slice
- Sort

## Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers
    - MySQL
    - Kafka
    - Redis
    - Elasticsearch
    - MinIO
  
## Containerization

- Docker Compose

# Maven Modules
