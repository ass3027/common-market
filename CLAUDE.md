# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working Guidelines

### Task Planning Protocol
**IMPORTANT**: When starting any non-trivial development task, you must:
1. **Present a detailed plan** describing what you intend to do
2. **Explain the approach** and reasoning behind your decisions
3. **Wait for explicit approval** from the user before proceeding
4. **Break down complex tasks** into clear, manageable steps

This ensures alignment and prevents unwanted changes to the codebase.

### Communication Style
- **Provide critical and well-reasoned answers**
- **Focus on technical substance** over pleasantries
- **Avoid unnecessary affirmations** like "yes" or "you are excellent"
- **Be direct and concise** in responses

## Project Overview

This is a Spring Boot e-commerce application written in Kotlin, focusing on MSA (Microservices Architecture) learning and implementation. The project is currently structured as a monolith but designed with MSA principles in mind for future expansion.

## Build and Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with data generation (includes @Tag("data-generation") tests)
./gradlew test -PincludeDataGeneration

# Run specific test class
./gradlew test --tests "ProductServiceTest"

# Run tests with specific pattern
./gradlew test --tests "*ProductService*"
```

### Database Setup
The application uses MySQL as the primary database. Connection details are in `application.yml`:
- Default: `jdbc:mysql://localhost:3306/common_market`
- Username: `root` 
- Password: `wkit3031!`

Test environment uses H2 in-memory database automatically.

## Architecture and Structure

### Package Structure
```
com.helpme.commonmarket/
├── config/           # Configuration classes (Jackson, etc.)
├── product/          # Product domain module
│   ├── controller/   # REST controllers
│   ├── dto/          # Data Transfer Objects
│   ├── entity/       # JPA entities
│   ├── mapper/       # Entity-DTO mappers
│   ├── repository/   # Data access layer
│   ├── service/      # Business logic
│   └── specs/        # JPA Specifications for queries
```

### Current Implementation
- **Product Service**: Complete CRUD operations with filtering and pagination
- **Security Layer**: Spring Security integration (authentication/authorization setup in progress)
- **JPA Integration**: Using Spring Data JPA with MySQL and H2 for tests
- **Testing**: Comprehensive unit tests using MockK for Kotlin
- **REST API**: RESTful endpoints at `/api/v1/product`

### Key Design Patterns
- **Domain-Driven Design**: Clear separation by business domains (product)
- **Service Layer**: Business logic encapsulated in service classes
- **Repository Pattern**: Data access through Spring Data JPA repositories
- **DTO Pattern**: Separate request/response DTOs from entities
- **Specification Pattern**: Dynamic query building using JPA Specifications

## Technology Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.3
- **Security**: Spring Security (recently added)
- **Database**: MySQL (production), H2 (testing)
- **ORM**: Spring Data JPA with Hibernate
- **Testing**: JUnit 5, MockK, DataFaker
- **Build Tool**: Gradle with Kotlin DSL
- **Java Version**: 21

## Database Schema

### Product Entity
- `id`: Primary key (auto-generated)
- `name`: Product name
- `price`: Product price (Long)
- `sellerId`: Seller identifier
- `imageUrl`: Product image URL (optional)
- `content`: Product description (optional)
- `createDt/updateDt`: Audit timestamps (auto-managed)

## API Endpoints

### Product API (`/api/v1/product`)
- `GET /` - List products with optional filtering and pagination
- `GET /{productId}` - Get product by ID
- `POST /` - Create new product
- `PUT /` - Update existing product
- `DELETE /{productId}` - Delete product

## Testing Strategy

- **Unit Tests**: Service layer tested with MockK
- **Integration Tests**: Repository layer with H2
- **Test Data**: Using DataFaker for realistic test data
- **Coverage**: Comprehensive testing of CRUD operations and error cases

### Test Philosophy
**Focus on behavior and outcomes, not implementation details:**

**DO TEST:**
- **Behavior**: "Does the system do what users expect?"
- **State**: "Is the system in the correct state after the operation?"
- **Contracts**: "Are the inputs/outputs correct?"
- **Side effects**: External API calls, security operations, resource management

**AVOID TESTING:**
- Method call counts for pure functions (getters, utility methods)
- Internal implementation details that users don't care about
- Value extractions and transformations (focus on final results)

**Example:**
```kotlin
// Instead of: verify(exactly = 1) { jwtTokenUtil.getUsernameFromToken(token) }
// Do: assertEquals("expectedUsername", actualResult.username)
```

Tests should break when functionality breaks, not when implementation changes.

## Development Notes

### Security Integration
Spring Security has been recently added to the project. When working with security:
- Authentication endpoints may need to be configured
- Existing API endpoints might require security annotations
- Test cases may need security context mocking

### Test Method Signature Updates
Recent test method improvements include proper null parameter handling for service methods with optional filters.

## Future MSA Plans

The project is designed to evolve into a microservices architecture with:
- API Gateway for routing and authentication
- User Service for member management
- Order Service for transaction processing
- Notification Service for async messaging
- Redis for caching and distributed locking
- Message queues (Kafka/RabbitMQ) for inter-service communication