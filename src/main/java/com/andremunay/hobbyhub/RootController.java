package com.andremunay.hobbyhub;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects root URL requests to the HobbyHub welcome page.
 *
 * <p>Improves UX by ensuring "/" is never a dead route during development or deployment.
 */
@Controller
public class RootController {

  /**
   * Redirects the root path ("/") to the static welcome page.
   *
   * @return a redirect instruction to "/welcome"
   */
  @GetMapping("/")
  public String home() {
    return "redirect:/welcome";
  }
}
