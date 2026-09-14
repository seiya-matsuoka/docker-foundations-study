package com.example.unit11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Unit11Application {

  public static void main(String[] args) {
    // Docker / Compose 学習用 Backend Application の起動点。
    SpringApplication.run(Unit11Application.class, args);
  }
}
