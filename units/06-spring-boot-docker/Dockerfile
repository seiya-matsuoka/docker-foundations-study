# syntax=docker/dockerfile:1

# Maven で Build 済みの JAR を実行するため、JDK ではなく JRE Image を利用する。
# Java 21.0.12 / Alpine Linux 3.24 を明示的に固定する。
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24

# Spring Boot Application を配置・実行する基準 Directory。
WORKDIR /app

# Host 側の Maven Build で生成した実行可能 JAR を Image へ取り込む。
# Source Code 自体ではなく Build 成果物を Runtime Image に含める。
COPY target/unit06-spring-boot-1.0.0.jar app.jar

# Container 実行時に利用できる Default の Environment Variable。
# docker container run -e APP_MESSAGE=... で Runtime に上書きできる。
ENV APP_MESSAGE="Hello from Docker image"

# Spring Boot の Default Port 8080 を利用する想定であることを Metadata として示す。
EXPOSE 8080

# Container 起動時に JRE で Spring Boot JAR を実行する。
CMD ["java", "-jar", "app.jar"]
