package com.andremunay.hobbyhub;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

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

            <h2 class="text-xl font-semibold mt-6">Ops</h2>
            <a href="/actuator/metrics" class="text-blue-600 hover:underline">Actuator Metrics</a>
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
