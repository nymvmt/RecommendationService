# 멀티스테이지 빌드: 빌드 스테이지
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test --info
RUN echo "=== Build completed, checking files ==="
RUN ls -la build/libs/
RUN echo "=== JAR files found ==="

# 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/recommendation-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
