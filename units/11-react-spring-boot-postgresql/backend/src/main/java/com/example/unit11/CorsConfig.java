package com.example.unit11;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final String allowedOrigin;

  public CorsConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
    this.allowedOrigin = allowedOrigin;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // React は Host Browser の http://localhost:5173 で表示され、
    // API は http://localhost:8080 へ Request するため Origin が異なる。
    // Unit 11 では GET /api/** に必要な最小 CORS 設定だけを許可する。
    registry.addMapping("/api/**").allowedOrigins(allowedOrigin).allowedMethods("GET");
  }
}
