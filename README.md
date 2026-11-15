# BackendHub — Java Backend Interview Preparation Platform

A full-stack Java backend interview preparation platform built with **Java 21, Spring Boot, Spring Security, React, MySQL, Redis, and Apache Kafka**.

BackendHub provides a structured workspace for developers preparing for Java backend interviews. It combines a searchable question library, secure authentication, timed mock examinations, personal bookmarks, performance analytics, and an administrator content-management interface.

---

## Overview

BackendHub transforms a traditional console-based quiz application into a modern web-based learning platform.

The application is designed around two primary experiences:

- **Learners:** Explore interview questions, practise MCQs, take timed mock exams, bookmark questions, and monitor learning progress.
- **Administrators:** Manage interview questions, import question collections, maintain the content library, and monitor platform statistics.

The backend follows a **modular monolithic architecture**, with clear separation between application domains. Redis and Kafka provide caching and event-integration capabilities.

## Key Features

### Learner Platform

- User registration and secure login.
- JWT-based authentication and authorization.
- BCrypt password hashing.
- Searchable and filterable Java backend question library.
- Questions organized by technology, category, and difficulty.
- Detailed explanations and optional code examples.
- Personal question bookmarks.
- Timed mock examinations.
- Randomized MCQ questions.
- Configurable exam duration and question count.
- Automatic deadline enforcement and exam submission.
- Server-side answer persistence.
- Exam history and answer review.
- Topic-wise performance analytics.
- Account profile and password management.

### Administration Platform

- Admin-only access to management features.
- Create and update interview questions.
- Soft archive questions.
- Bulk question import through JSON.
- Protected answer-key management.
- Platform overview and question statistics.
- Environment-based initial administrator creation.

### User Interface

- Modern dark-themed interface.
- Responsive React application.
- Cyan and violet visual accents.
- Glass-inspired interface elements.
- Dedicated exam and learning layouts.
- Lucide icon integration.

---

## Technology Stack

| Layer | Technologies |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot 3.5.16 |
| Backend Architecture | Modular Monolith |
| REST API | Spring Web |
| Security | Spring Security, JWT, OAuth2 Resource Server |
| Password Hashing | BCrypt |
| ORM | Hibernate |
| Data Access | Spring Data JPA |
| Database | MySQL 8.4 |
| Caching | Redis 7 |
| Messaging | Apache Kafka 4.1 |
| Frontend | React 19 |
| Build Tool | Vite 6 |
| Icons | Lucide |
| API Documentation | Springdoc OpenAPI / Swagger UI |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus |
| Containerization | Docker, Docker Compose |
| CI | GitHub Actions |

---

## Architecture

BackendHub uses a modular monolithic backend with separate application responsibilities.

```text
                    BACKENDHUB
                        |
             +----------+----------+
             |                     |
        React Frontend        Spring Boot API
             |                     |
          Nginx              Spring Security
                                   |
                    +--------------+--------------+
                    |              |              |
                Identity       Questions       Practice
                    |              |              |
                    +--------------+--------------+
                                   |
                         Spring Data JPA
                                   |
                    +--------------+--------------+
                    |              |              |
                  MySQL          Redis          Kafka
                Database         Cache         Events
```

### Main Application Modules

- **Identity:** Registration, authentication, authorization, and account management.
- **Question Management:** Question discovery, filtering, administration, and bulk import.
- **Practice:** Mock exam creation, answer persistence, scoring, and review.
- **Bookmarks:** Personal saved-question management.
- **Analytics:** Learning statistics and performance tracking.

> **Architecture note:** This is a modular monolith, not an independently deployed microservices system. Its module boundaries provide potential future extraction points.

---

## Getting Started

### Prerequisites

For Docker-based deployment:

- Docker Desktop or Docker Engine.
- Docker Compose plugin.
- Git.

For local development:

- JDK 21.
- Maven 3.9+.
- Node.js 22+.
- npm.
- MySQL 8.4.
- Redis 7.
- Kafka 4.x.

### 1. Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
cd java-backend-interview-hub-final
```

Replace the repository URL with your actual GitHub repository URL.

### 2. Configure Environment Variables

Create your local environment file:

```bash
cp .env.example .env
```

Open `.env` and configure the required values.

```env
DB_PASSWORD=your_unique_database_password

JWT_SECRET=your_base64_encoded_jwt_secret

ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your_secure_admin_password
ADMIN_FULL_NAME=Platform Admin
```

Generate a JWT secret using:

```bash
openssl rand -base64 48
```

Security requirements:

- Use unique credentials.
- Keep `.env` out of Git.
- Never publish actual passwords or JWT secrets.
- Use a bootstrap administrator password of at least 12 characters.
- The JWT secret must decode to at least 32 bytes.

### 3. Build and Start the Application

```bash
docker compose up --build -d
```

Docker Compose starts the following services:

- MySQL
- Redis
- Apache Kafka
- Spring Boot Backend
- React Frontend with Nginx

### 4. Verify Container Status

```bash
docker compose ps
```

View backend logs:

```bash
docker compose logs backend --tail=100
```

View frontend logs:

```bash
docker compose logs frontend --tail=100
```

### 5. Access BackendHub

| Component | Local URL |
|---|---|
| Frontend Application | http://localhost:3001 |
| Backend API | http://localhost:8086 |
| Swagger UI | http://localhost:8086/swagger-ui/index.html |
| Backend Health | http://localhost:8086/actuator/health |

### Infrastructure Ports

| Service | Host Port | Container Port |
|---|---:|---:|
| MySQL | 3308 | 3306 |
| Redis | 6382 | 6379 |
| Kafka | 9095 | 29092 |
| Backend | 8086 | 8080 |
| Frontend | 3001 | 80 |

These host ports are configured to avoid conflicts with other locally running applications.

### 6. Stop the Application

```bash
docker compose down
```

To remove the project's persistent database, cache, and Kafka data:

```bash
docker compose down -v
```

**Warning:** The second command permanently deletes the project's Docker volumes and stored data.

---

## Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | MySQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address |
| `JWT_SECRET` | Base64-encoded JWT signing secret |
| `JWT_TTL_SECONDS` | JWT access-token lifetime |
| `ADMIN_EMAIL` | Initial administrator email |
| `ADMIN_PASSWORD` | Initial administrator password |
| `ADMIN_FULL_NAME` | Administrator display name |
| `FRONTEND_ORIGIN` | Permitted frontend origin |
| `DDL_AUTO` | Hibernate schema-management mode |

For Docker Compose, the frontend origin is:

```text
http://localhost:3001
```

The backend connects to MySQL, Redis, and Kafka through their Docker service names.

---

## REST API Overview

All request and response bodies use JSON unless otherwise specified.

Protected endpoints require a JWT access token:

```http
Authorization: Bearer <access-token>
```

### Authentication and Account Management

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a learner |
| POST | `/api/auth/login` | Authenticate and receive JWT |
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/me/password` | Change account password |

### Question Management

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/questions` | Public |
| GET | `/api/questions/{id}` | Public |
| POST | `/api/questions` | Admin |
| PUT | `/api/questions/{id}` | Admin |
| DELETE | `/api/questions/{id}` | Admin |
| GET | `/api/admin/questions/{id}` | Admin |
| POST | `/api/admin/questions/bulk` | Admin |
| GET | `/api/admin/overview` | Admin |

Example question search:

```http
GET /api/questions?q=hashmap&technology=Java&difficulty=Intermediate&page=0&size=12
```

### Bookmarks

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/bookmarks` | Get saved questions |
| POST | `/api/bookmarks/{questionId}` | Bookmark a question |
| DELETE | `/api/bookmarks/{questionId}` | Remove bookmark |

### Mock Examinations and Practice

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/practice/start` | Start a mock examination |
| GET | `/api/practice/{attemptId}` | Resume an active examination |
| PUT | `/api/practice/{attemptId}/answers` | Save examination answers |
| POST | `/api/practice/{attemptId}/submit` | Submit and score examination |
| GET | `/api/practice/attempts` | Get examination history |
| GET | `/api/practice/attempts/{id}` | Review completed examination |

Example:

```http
POST /api/practice/start?count=10&durationMinutes=20
```

### Analytics

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/analytics/me` | Get personal performance analytics |

---

## Security Design

BackendHub incorporates several security mechanisms:

- JWT-based authentication.
- Role-based authorization.
- BCrypt password hashing.
- Protected administrator operations.
- Server-side exam scoring.
- Protected MCQ answer keys.
- Exam ownership validation.
- Deadline enforcement.
- Learner-only public registration.

Public registration always assigns the `USER` role. The bootstrap administrator is created through environment configuration when the corresponding account does not already exist.

The public question API does not expose correct MCQ answers.

---

## Testing and Validation

### Backend

```bash
cd backend

mvn test
mvn verify
```

### Frontend

```bash
cd frontend

npm install
npm run build
```

### Docker Configuration

```bash
docker compose config
docker compose up --build
```

The project includes automated tests covering authentication, account management, bookmarks, analytics, question validation, answer-key protection, practice scoring, exam deadlines, and ownership checks.

GitHub Actions is configured for backend verification and frontend production builds.

---

## Current Architecture and Production Considerations

BackendHub is designed as a serious learning and portfolio project. It should not be considered a security-certified production service.

Before a public production deployment, additional work is recommended:

- Upgrade to a currently supported Spring Boot release.
- Configure HTTPS and managed secrets.
- Add refresh-token rotation and revocation.
- Implement email verification and password recovery.
- Add login rate limiting.
- Introduce Flyway or Liquibase database migrations.
- Use Hibernate schema validation in production.
- Add comprehensive integration testing with Testcontainers.
- Introduce Kafka consumers, retries, and dead-letter handling where required.
- Add monitoring dashboards, tracing, backups, and operational alerting.
- Review privacy and data-retention requirements.

---

## Future Enhancements

Potential improvements include:

- More Java, Spring Boot, SQL, and system-design questions.
- Coding challenges with automated evaluation.
- Personalized learning recommendations.
- Interview preparation roadmaps.
- Advanced analytics and progress visualization.
- More comprehensive integration and end-to-end testing.
- Independently deployable domain services if future scale requires them.

---

## Author

**MD Faijan Ali**

B.Tech Graduate — Computer Science and Engineering (AI & ML)

Interests: Java Backend Development, Spring Boot, REST APIs, Distributed Systems, Generative AI, and Agentic AI.

---

## License

This project is available under the MIT License. See the `LICENSE` file for details if included in the repository.

---

**BackendHub — Learn. Practise. Prepare. Build Your Backend Career.**
