package com.example.unit11;

/**
 * PostgreSQL から取得した学習用 Data を React へ返す最小 DTO。
 *
 * <p>Unit 11 では CRUD や複雑な Domain Model を扱わず、 React → Spring Boot → PostgreSQL の通信確認に集中する。
 */
public record MessageResponse(int id, String message) {}
