package com.example.unit10;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

  private final JdbcTemplate jdbcTemplate;

  public MessageService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public MessageResponse getMessage() {
    // DB 接続確認に必要な最小 SQL だけを実行する。
    // JdbcTemplate が application.properties の DataSource 設定を利用し、
    // Docker Network 経由で postgres Service の PostgreSQL へ接続する。
    return jdbcTemplate.queryForObject(
        "SELECT id, message FROM study_message WHERE id = ?",
        (resultSet, rowNum) ->
            new MessageResponse(resultSet.getInt("id"), resultSet.getString("message")),
        1);
  }
}
