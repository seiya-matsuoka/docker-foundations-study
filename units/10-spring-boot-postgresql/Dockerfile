# syntax=docker/dockerfile:1

# Unit 06 と同じ Runtime Image を使い、Spring Boot + PostgreSQL の連携学習へ集中する。
# Java 21.0.12 / Alpine Linux 3.24 を明示的に固定する。
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24

# Spring Boot Application を配置・実行する基準 Directory。
WORKDIR /app

# Host 側の Maven Build で生成した実行可能 JAR を Runtime Image へ取り込む。
# Multi-stage Build は Unit 12 で扱うため、Unit 10 では Unit 06 と同じ責務分離を維持する。
COPY target/unit10-spring-postgresql-1.0.0.jar app.jar

# Spring Boot が Container 内で Port 8080 を利用する想定であることを Metadata として示す。
EXPOSE 8080

# Container 起動時に JRE で Spring Boot JAR を実行する。
CMD ["java", "-jar", "app.jar"]
