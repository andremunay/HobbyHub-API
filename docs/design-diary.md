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

---

## Retrospective (Post-Implementation)

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
