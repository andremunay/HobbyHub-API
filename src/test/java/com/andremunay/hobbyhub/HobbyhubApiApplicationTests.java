package com.andremunay.hobbyhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HobbyhubApplicationTests {

  @Test
  void contextLoads() {
    // This test ensures the Spring context loads without issues.
    // It is intentionally left blank.
  }
}
