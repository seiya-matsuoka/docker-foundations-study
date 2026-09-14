package com.example.unit10;

/**
 * PostgreSQL から取得した学習用 Data を API Response として返す最小 DTO。
 *
 * <p>Unit 10 では CRUD や複雑な Domain Model を扱わず、 Spring Boot Container から PostgreSQL Container
 * へ接続できることの確認に集中する。
 */
public record MessageResponse(int id, String message) {}
