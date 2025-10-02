package dev.peemtanapat.thaiticketmaster.event_api.health;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api")
public class HealthController {

  @GetMapping("/health")
  public String health() {
    return "event-api is OK";
  }

}
