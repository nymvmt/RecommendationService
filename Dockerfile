# 멀티스테이지 빌드: 빌드 스테이지
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
