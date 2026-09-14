package com.example.unit10;

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
    // HTTP Request → Spring Boot → PostgreSQL → JSON Response の最小経路を確認する。
    return messageService.getMessage();
  }
}
