# 🏥 Hospital Management System — Spring Boot Microservices

A production-grade Hospital Management System built with Spring Boot Microservices architecture.

## 🏗️ Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │  :8080
                        │  (JWT Auth)     │
                        └────────┬────────┘
              ┌─────────┬────────┼────────┬─────────┐
              ▼         ▼        ▼        ▼         ▼
        ┌──────────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────────┐
        │ Patient  │ │Doctor│ │Appt  │ │Bill  │ │Notification  │
        │ Service  │ │Svc   │ │Svc   │ │Svc   │ │Service       │
        │  :8081   │ │:8082 │ │:8083 │ │:8084 │ │  :8085       │
        └──────────┘ └──────┘ └──────┘ └──────┘ └──────────────┘
                                    │               ▲
                                    └──── Kafka ────┘
```

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA + Hibernate |
| Messaging | Apache Kafka |
| Database | MySQL |
| Containerization | Docker + Docker Compose |
| Build | Maven |
| API Docs | Swagger UI (OpenAPI 3.0) |
| Testing | JUnit 5 + Mockito |

## 📦 Microservices

| Service | Port | Description |
|---|---|---|
| api-gateway | 8080 | Routes requests, JWT validation |
| patient-service | 8081 | Patient registration, records |
| doctor-service | 8082 | Doctor profiles, availability |
| appointment-service | 8083 | Book/cancel appointments |
| billing-service | 8084 | Generate & manage bills |
| notification-service | 8085 | Kafka consumer, sends alerts |

## ⚡ Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- MySQL 8.0 (or use Docker)

### 1. Clone the project
```bash
git clone https://github.com/abhishek-nichit/hospital-management.git
cd hospital-management
```

### 2. Start infrastructure (MySQL + Kafka + Zookeeper)
```bash
docker-compose up -d
```

### 3. Run each service (open 5 terminals)
```bash
# Terminal 1
cd api-gateway && mvn spring-boot:run

# Terminal 2
cd patient-service && mvn spring-boot:run

# Terminal 3
cd doctor-service && mvn spring-boot:run

# Terminal 4
cd appointment-service && mvn spring-boot:run

# Terminal 5
cd billing-service && mvn spring-boot:run

# Terminal 6
cd notification-service && mvn spring-boot:run
```

### 4. Access Swagger UI
- API Gateway: http://localhost:8080/swagger-ui.html
- Patient Service: http://localhost:8081/swagger-ui.html
- Doctor Service: http://localhost:8082/swagger-ui.html
- Appointment Service: http://localhost:8083/swagger-ui.html
- Billing Service: http://localhost:8084/swagger-ui.html

## 🔐 Authentication

All endpoints (except `/auth/**`) require JWT token.

### Register
```
POST http://localhost:8080/auth/register
{
  "username": "admin",
  "password": "admin123",
  "role": "ADMIN"
}
```

### Login
```
POST http://localhost:8080/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```
Returns: `{ "token": "eyJhbGci..." }`

### Use token in all requests
```
Header: Authorization: Bearer <token>
```

## 📋 Sample API Calls

### Register a Patient
```
POST http://localhost:8080/api/patients
{
  "name": "Rahul Sharma",
  "age": 30,
  "gender": "MALE",
  "phone": "9876543210",
  "email": "rahul@example.com",
  "bloodGroup": "A+"
}
```

### Book Appointment
```
POST http://localhost:8080/api/appointments
{
  "patientId": 1,
  "doctorId": 1,
  "appointmentDate": "2026-06-15",
  "appointmentTime": "10:00",
  "reason": "Fever and cold"
}
```

### Generate Bill
```
POST http://localhost:8080/api/billing
{
  "appointmentId": 1,
  "consultationFee": 500.00,
  "medicationFee": 200.00
}
```

## 🐳 Docker Compose
```bash
# Start everything
docker-compose up -d

# Stop everything
docker-compose down

# View logs
docker-compose logs -f
```

## 🧪 Run Tests
```bash
cd patient-service && mvn test
cd doctor-service && mvn test
cd appointment-service && mvn test
cd billing-service && mvn test
```
