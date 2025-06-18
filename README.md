[![CI](https://github.com/andremunay/HobbyHub-API/actions/workflows/ci.yml/badge.svg)](https://github.com/andremunay/HobbyHub-API/actions/workflows/ci.yml)

# HobbyHub-API

HobbyHub API is a personal-skill tracker designed for hobbyists who want to deepen their expertise and stay motivated. This MVP focuses on two high-impact modules—Spanish vocabulary and weightlifting—to demonstrate how centralized note-taking and progress analytics can drive measurable improvement. By capturing flashcard data and workout stats in structured practice journals, the API helps users identify trends, retain knowledge, and apply deliberate practice. Built with Spring Boot and PostgreSQL, the project showcases clean architecture, real-world analytics, and an extensible foundation for lifelong learning.

---

## Secrets & Configuration

- **GitHub OAuth**  
  - `OAUTH_CLIENT_ID`  
  - `OAUTH_CLIENT_SECRET`  
- **Fly.io**  
  - `FLY_API_TOKEN`  
- **Container Registry**  
  - `REGISTRY_USERNAME`  
  - `REGISTRY_PASSWORD`

## Code Coverage

- We enforce a minimum **80 % instruction coverage** via JaCoCo.  
- To view the full HTML report:
  1. Go to the **Actions** tab → select the latest **CI** run.  
  2. Under **Artifacts**, click **jacoco-report** to download.  
  3. Unzip and open `index.html` in a browser for class-by-class coverage details.

## Developer Docs

- [Swagger UI](https://hobbyhub-api.fly.dev/swagger-ui/index.html) – Live interactive playground
- [Stoplight Elements](https://elements-demo.stoplight.io/?spec=https://hobbyhub-api.fly.dev/v3/api-docs) – Rendered OpenAPI spec for client SDK generation
