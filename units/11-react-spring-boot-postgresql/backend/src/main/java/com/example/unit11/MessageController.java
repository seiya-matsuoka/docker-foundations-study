package com.example.unit11;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

  private final MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @GetMapping("/api/message")
  public MessageResponse message() {
    // Browser 上の React → Spring Boot → PostgreSQL → JSON Response の最小経路。
    return messageService.getMessage();
  }
}
