package com.andremunay.hobbyhub;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public home controller for the HobbyHub API.
 *
 * <p>Serves a polished HTML welcome page with links to developer tools, documentation, and
 * observability endpoints. Includes a call-to-action for GitHub authentication.
 */
@RestController
public class HomeController {

  @GetMapping(
      value = {"/", "/welcome"},
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> welcomePage() {
    String html =
        """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>HobbyHub API &ndash; Welcome</title>
        <script src="https://cdn.tailwindcss.com"></script>
      </head>
      <body class="bg-gray-100 text-gray-800 font-sans leading-relaxed">
        <header class="bg-white shadow">
          <div class="container mx-auto px-6 py-4 flex justify-between items-center">
            <h1 class="text-2xl font-bold">HobbyHub API</h1>
            <nav class="space-x-4">
              <a href="/oauth2/authorization/github"
                 class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition">
                Login
              </a>
            </nav>
          </div>
        </header>

        <main class="container mx-auto px-6 py-12">
          <section class="mb-12 text-center">
            <h2 class="text-3xl font-semibold mb-4">Welcome!</h2>
            <p class="text-gray-600">Log in to manage your data or explore our API documentation and metrics.</p>
          </section>

          <section class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="bg-white p-6 rounded-lg shadow hover:shadow-md transition">
              <h3 class="text-xl font-semibold mb-2">Documentation</h3>
              <ul class="list-disc list-inside space-y-1">
                <li><a href="/swagger-ui/index.html" class="text-blue-600 hover:underline">Swagger UI</a></li>
                <li><a href="https://elements-demo.stoplight.io/?spec=https://hobbyhub-api.fly.dev/v3/api-docs" class="text-blue-600 hover:underline">Stoplight API Docs</a></li>
              </ul>
            </div>

            <div class="bg-white p-6 rounded-lg shadow hover:shadow-md transition">
              <h3 class="text-xl font-semibold mb-2">Observability</h3>
              <ul class="list-disc list-inside space-y-1">
                <li><a href="/actuator/metrics" class="text-blue-600 hover:underline">Actuator Metrics</a></li>
                <li><a href="/actuator/health" class="text-blue-600 hover:underline">Actuator Health</a></li>
                <li><a href="/actuator/prometheus" class="text-blue-600 hover:underline">Prometheus Scrape</a></li>
                <li><a href="https://hobbyhub-prometheus.fly.dev" target="_blank" class="text-blue-600 hover:underline">Prometheus Dashboard</a></li>
              </ul>
            </div>
          </section>
        </main>

      </body>
      </html>
      """;

    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }
}
