package com.example.unit12;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

  private final JdbcTemplate jdbcTemplate;

  public MessageService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public MessageResponse getMessage() {
    return jdbcTemplate.queryForObject(
        "SELECT id, message FROM study_message WHERE id = ?",
        (resultSet, rowNum) ->
            new MessageResponse(resultSet.getInt("id"), resultSet.getString("message")),
        1);
  }
}
