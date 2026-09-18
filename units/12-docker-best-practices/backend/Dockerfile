# syntax=docker/dockerfile:1

# ----- Build Stage -----
# Maven / JDK は Application Build にだけ利用する。
FROM maven:3.9.16-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# pom.xml を Source Code より先に Copy して Dependency 解決 Layer を分離する。
# Source Code だけ変更した場合に Dependency Layer を再利用しやすくする。
COPY pom.xml ./

RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -B -DskipTests package

# ----- Runtime Stage -----
# Runtime には Maven / JDK / Source Code を含めず、JRE + JAR のみにする。
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24 AS runtime

# Application を必要以上の root 権限で実行しないため専用 User / Group を作成する。
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Build Stage から実行 JAR だけを Copy し、Runtime User の所有にする。
COPY --from=build --chown=appuser:appgroup /workspace/target/unit12-spring-backend-1.0.0.jar app.jar

# 以降の Application Process は root ではなく appuser で実行する。
USER appuser

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
