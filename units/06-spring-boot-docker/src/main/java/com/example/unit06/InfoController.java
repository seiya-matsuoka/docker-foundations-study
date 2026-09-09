package com.example.unit06;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

  private final String message;

  public InfoController(@Value("${app.message}") String message) {
    this.message = message;
  }

  @GetMapping("/api/info")
  public Map<String, String> info() {
    // APP_MESSAGE が Container の Environment Variable から Spring Boot 設定へ渡ることを確認する。
    return Map.of("application", "unit06-spring-boot", "message", message);
  }
}
