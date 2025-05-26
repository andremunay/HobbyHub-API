package com.andremunay.hobbyhub.spanish;

import static org.assertj.core.api.Assertions.assertThat;

import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class SpanishIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgreSQL =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("hobbyhub")
          .withUsername("hobbyuser")
          .withPassword("hobbysecret");

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQL::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQL::getUsername);
    registry.add("spring.datasource.password", postgreSQL::getPassword);
    registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/master.yaml");
  }

  @LocalServerPort int port;

  private final TestRestTemplate rest = new TestRestTemplate();

  @Test
  void fullFlow_postAndReview() {
    String url = "http://localhost:" + port + "/flashcards";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> postReq =
        new HttpEntity<>("{\"front\":\"test\",\"back\":\"test\"}", headers);
    ResponseEntity<Void> postResp = rest.postForEntity(url, postReq, Void.class);
    assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<FlashcardReviewDto[]> getResp =
        rest.getForEntity(url + "/review?due=true", FlashcardReviewDto[].class);
    assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = getResp.getBody();
    assertThat(body).isNotNull().isNotEmpty();
    assertThat(body[0].getNextReviewOn()).isAfterOrEqualTo(LocalDate.now());
  }
}
