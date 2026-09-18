package com.example.unit12;

/** PostgreSQL から取得した学習用 Data を Frontend へ返す最小 DTO。 */
public record MessageResponse(int id, String message) {}
