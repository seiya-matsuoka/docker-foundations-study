package com.example.unit06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Unit06Application {

  public static void main(String[] args) {
    // Spring Boot 自体の学習ではなく、生成した JAR を Container で実行するための起動点。
    SpringApplication.run(Unit06Application.class, args);
  }
}
