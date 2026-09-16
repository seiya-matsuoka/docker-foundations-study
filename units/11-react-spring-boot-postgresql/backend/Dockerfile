# syntax=docker/dockerfile:1

# Unit 06 / 10 と同じ Java Runtime Image を利用する。
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24

WORKDIR /app

# Host 側の Maven Build で生成した実行可能 JAR を Runtime Image へ取り込む。
# Multi-stage Build は Unit 12 で扱うため、ここでは既習構成を維持する。
COPY target/unit11-spring-backend-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
