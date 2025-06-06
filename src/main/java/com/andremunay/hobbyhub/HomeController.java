package com.andremunay.hobbyhub;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping("/welcome")
  public String welcome(@AuthenticationPrincipal OAuth2User principal) {
    return "Welcome, " + principal.getAttribute("login");
  }
}
