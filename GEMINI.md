# Gemini Assistant Guidelines

## Response Principles
- Provide critical and well-reasoned answers.
- Base responses on the principles of Clean Code, Clean Architecture, and "Refactoring, Second Edition".
- Before answering, please refine my questions to be more natural and clear, then present your response.

## Execution Workflow
- Before executing any plan that involves multiple steps (such as editing files, running shell commands, or other complex tasks), I will first present a clear and concise plan outlining the intended actions. and receive confirmation
- I will not proceed with the execution of the plan until you have approved it.
- receive confirmation when you plan to git commit

## Project Technology Stack
- **Backend:** Kotlin, Spring Boot, Spring Cloud
- **Database:** MySQL, H2 (for testing)
- **ORM:** Spring Data JPA (Hibernate)
- **Build Tool:** Gradle
- **Testing:** JUnit 5, Mockito, Mockk, Datafaker
- **Infrastructure:** Docker, Kubernetes (as described in `mysql-deployment.yaml`)
