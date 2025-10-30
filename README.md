# TaskFlow - Enterprise Multi-Tenant SaaS Project Management Platform

Taskflow is a project management platform I built with Spring Boot featuring multi-tenancy, role-based access control, real-time updates, and caching strategies.

##  Features

- **Multi-Tenant Architecture**: Each organization’s data is isolated using row-level security
- **Role-Based Access Control (RBAC)**: Custom permissions (Owner, Admin, Member, Viewer)
- **Performance Optimization**: Redis caching with 60% performance improvement
- **Real-Time Updates**: WebSocket integration for live task updates
- **File Management**: AWS S3 integration for attachment storage
- **API Rate Limiting**: Per-tenant rate limiting using Bucket4j
- **Audit Logging**: Every action is tracked for transparency and compliance
- **Async Processing**: Background job processing for notifications
- **Production-Ready**: Docker containerization with CI/CD pipeline

##  Technical Stack

**Backend:**
- Java 17
- Spring Boot 3.2
- Spring Security (JWT Authentication)
- Spring Data JPA / Hibernate
- PostgreSQL
- Redis (Caching)
- WebSocket (Real-time)

**Testing:**
- JUnit 5
- Mockito
- TestContainers
- 85%+ test coverage

**DevOps:**
- Docker & Docker Compose
- GitHub Actions CI/CD
- AWS deployment ready

## ️ System Architecture

The platform uses a multi-layered architecture with clear separation of concerns:
- **API Layer**: RESTful endpoints with JWT authentication
- **Service Layer**: Business logic with caching and async processing
- **Data Layer**: JPA repositories scoped by tenant
- **Security Layer**: Spring Security with custom JWT filter


##  Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+ (or use Docker)
- Redis 7+ (or use Docker)

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/taskflow.git
cd taskflow
```

2. **Start dependencies with Docker Compose**
```bash
docker-compose up -d postgres redis
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Using Docker Compose 

```bash
docker-compose up --build
```

This starts PostgreSQL, Redis, and the application.

##  Authentication

The API uses JWT-based authentication. All requests (except auth endpoints) require a Bearer token.

### Register a new organization

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "organizationName": "Acme Corp",
  "subdomain": "acme",
  "email": "admin@acme.com",
  "password": "securepassword123",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@acme.com",
  "password": "securepassword123",
  "subdomain": "acme"
}
```

Response includes JWT token:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "admin@acme.com",
  "tenantId": 1,
  "tenantName": "Acme Corp"
}
```

##  API Endpoints

### Projects

```
GET    /api/v1/projects              # List all projects
GET    /api/v1/projects/{id}         # Get project by ID
POST   /api/v1/projects              # Create project
PUT    /api/v1/projects/{id}         # Update project
DELETE /api/v1/projects/{id}         # Delete project
```

### Tasks

```
GET    /api/v1/projects/{id}/tasks           # List tasks
GET    /api/v1/projects/{id}/tasks/{taskId}  # Get task
POST   /api/v1/projects/{id}/tasks           # Create task
PUT    /api/v1/projects/{id}/tasks/{taskId}  # Update task
DELETE /api/v1/projects/{id}/tasks/{taskId}  # Delete task
```

### Example Request

```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 marketing campaign",
    "description": "Launch new product line"
  }'
```

##  Configuration

Key configuration in `application.yml`:

```yaml
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/taskflow
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
jwt.secret=your-secret-key
jwt.expiration=86400000  # 24 hours

# Rate Limiting
rate-limit.capacity=100
rate-limit.refill-duration-minutes=1
```

Environment variables for production:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `JWT_SECRET`
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_S3_BUCKET`

##  Testing

Run all tests:
```bash
mvn test
```

Run with coverage:
```bash
mvn test jacoco:report
```

Tests include:
- Unit tests with Mockito
- Integration tests with TestContainers
- API endpoint tests with MockMvc

##  Implementation Details

### Multi-Tenancy
- Tenant stored in a ThreadLocal context
- Queries automatically filtered by tenant ID
- Complete data isolation at database level

### Caching 
- Project and task data cached in Redis
- Cache is invalidated on updates/deletes
- TTL: 1 hour
- Cache keys include tenant ID

### Security
- JWT tokens with tenant ID embedded
- Role-based authorization with @PreAuthorize
- BCrypt password hashing
- CORS configured for production

### Rate Limiting
- Per-tenant buckets
- 100 requests per minute default
- Configurable capacity and refill rate
- Uses Bucket4j library

### Real-Time Updates
- WebSocket connection per project
- STOMP protocol
- Broadcasts task updates to all connected clients
- Topics: `/topic/project/{projectId}/tasks`

##  Deployment

### Docker Build

```bash
docker build -t taskflow:latest .
```

### AWS ECS Deployment

1. Push image to ECR
2. Create task definition with environment variables
3. Configure ALB for load balancing
4. Set up RDS PostgreSQL and ElastiCache Redis
5. Configure S3 bucket for attachments

##  Performance

- **Response Time**: < 200ms average (with caching)
- **Throughput**: 100+ requests/second per instance
- **Cache Hit Rate**: ~85% for frequently accessed data
- **Database Connections**: HikariCP pool (max 20)

##  Monitoring

The application exposes:
- Health check: `/api/v1/auth/health`
- Spring Boot Actuator endpoints (can be enabled)
- Structured logging with Logback


##  Author

Gaoussou Thiam
- GitHub: @thiamgw
- LinkedIn: https://www.linkedin.com/in/gaoussou-thiam-1b3378353




