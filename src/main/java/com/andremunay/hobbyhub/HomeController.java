package com.andremunay.hobbyhub;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public home controller for the HobbyHub API.
 *
 * <p>Serves a styled HTML welcome page with links to Swagger, Stoplight, and observability tools.
 * Useful for devs and ops engineers as a landing page in non-headless deployments.
 */
@RestController
public class HomeController {

  /**
   * Serves a static HTML welcome page with links to API docs and diagnostics.
   *
   * @return HTTP 200 OK with HTML content
   */
  @GetMapping("/welcome")
  public ResponseEntity<String> welcomePage() {
    String html =
        """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>HobbyHub API - Welcome</title>
        <script src="https://cdn.tailwindcss.com"></script>
      </head>
      <body class="bg-gray-50 text-gray-900 font-sans">
        <div class="max-w-3xl mx-auto py-12 px-4">
          <h1 class="text-3xl font-bold mb-6">Welcome to HobbyHub API</h1>
          <p class="mb-4">Explore tools and diagnostics:</p>

          <div class="grid gap-4">
            <a href="/swagger-ui/index.html" class="text-blue-600 hover:underline">Swagger UI</a>
            <a href="https://elements-demo.stoplight.io/?spec=https://hobbyhub-api.fly.dev/v3/api-docs"
              class="text-blue-600 hover:underline">Stoplight API Docs</a>

            <h2 class="text-xl font-semibold mt-6">Ops</h2>
            <a href="/actuator/metrics" class="text-blue-600 hover:underline">Actuator Metrics</a>
            <a href="/actuator/health" class="text-blue-600 hover:underline">Actuator Health</a>
            <a href="/actuator/prometheus" class="text-blue-600 hover:underline">Prometheus Scrape</a>
            <a href="https://hobbyhub-prometheus.fly.dev" class="text-blue-600 hover:underline" target="_blank">Prometheus Dashboard</a>
          </div>
        </div>
      </body>
      </html>
      """;

    return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
  }
}
