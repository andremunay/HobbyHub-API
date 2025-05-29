# Design Diary

A living log of key design decisions, trade-offs, and reflections.

---

## [2025-05-15] Backend Project Scaffolding & Initial Setup

### Context

This task kicks off the **HobbyHub backend project** by laying down the foundational infrastructure required for effective development. It includes scaffolding a **Spring Boot application** with a clean hexagonal architecture, setting up essential **tooling**, and enforcing **code quality gates** from the start.

The project serves two main domains — `spanish` and `weightlifting` — which are isolated in their own modular structures, along with a `shared` layer for common functionality.

The aim is to:

* Create a robust and scalable foundation
* Ensure a clean, maintainable codebase from day one
* Automate quality checks via CI and enforce best practices
* Set up consistent development environments across contributors

This initial scaffolding enables a high-confidence environment for future feature development by reducing technical debt upfront.

### Lessons Learned

* **Docker Basics & Containerization**
  Gained hands-on experience with downloading and installing Docker. Learned the fundamentals of containerization and how to spin up a PostgreSQL container using `docker compose`, which is now a core part of the local development workflow.

* **WSL2 & Virtualization Setup**
  Set up Windows Subsystem for Linux (WSL2) and enabled virtualization support, providing a more UNIX-like environment that's compatible with most backend development tooling.

* **Git Hygiene & `.gitignore` Best Practices**
  Learned the industry standard contents for `.gitignore` files in Java/Maven projects, helping avoid committing build artifacts, IDE settings, and sensitive files.

* **Lombok, Spotless, and EditorConfig**
  Understood what these tools are and how to configure them:

  * **Lombok**: Reduces boilerplate in Java (e.g., getters/setters).
  * **Spotless**: Ensures code formatting consistency via Maven.
  * **EditorConfig**: Enforces consistent code style across different IDEs.

* **Integrating Spring Initializr into an Existing Project**
  Learned how to generate a Spring Boot project using Spring Initializr and successfully merge its structure into an already-initialized repository with custom domain layout.

* **GitHub Familiarity**
  Became comfortable navigating GitHub’s core features:

  * Repositories and branches
  * Issues and pull requests
  * Projects (Kanban boards)
  * CI integration and README badges

* **Issue Writing Best Practices**
  Learned how to write issues using an industry-standard format, including:

  * Clear, action-based titles
  * Descriptive context
  * Bulletproof acceptance criteria using checklists

---

## [2025-05-27] HobbyHub API Development

### Context

We began by drafting a one‐page architectural sketch to clarify module boundaries (Spanish, Weightlifting, shared) and inbound/outbound flows. Next, we integrated Liquibase—adding the plugin, creating a `master.yaml` changelog, and authoring a baseline migration for our core tables. Then we built out the Spanish module in four parts: defining the `Flashcard` entity and JPA repository, implementing the SM-2 spaced-repetition algorithm as a service, exposing create/list/review endpoints with DTOs in a controller, and finally wiring everything together with comprehensive slice and integration tests plus Swagger documentation.

### Lessons Learned

* **Docs directory organizational power**
  Keeping `docs/` in the repo helped crystallize design decisions, share them as an ADR substitute, and onboard collaborators.
* **Liquibase fundamentals**
  Learned the role of `master.yaml`, change-sets, and the `db/changelog` structure for controlled, repeatable migrations.
* **Integration testing with Testcontainers**
  Wrote my first real integration test against a live Postgres container, wiring in `@DynamicPropertySource` to bootstrap Spring’s `DataSource`.
* **`java.time` mastery**
  Leveraged `LocalDate` for review scheduling, saw how to serialize it in JSON via Jackson config.
* **Code coverage inspection**
  Discovered that `./mvnw clean verify` generates `target/site/jacoco/index.html`, and learned to open it in a browser to verify branch and line coverage.
* **Slice tests demystified**
  Understood how to write fast, focused tests with `@DataJpaTest` for JPA queries and `@WebMvcTest` (or standalone MockMvc) for controller logic.
* **Swagger UI for endpoint verification**
  Integrated Springdoc to expose `/swagger-ui/index.html`, making it trivial to manually explore and validate all REST endpoints.

---
