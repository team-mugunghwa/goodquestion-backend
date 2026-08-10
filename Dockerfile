# syntax=docker/dockerfile:1

# 1단계: gradle wrapper로 jar 빌드
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 의존성 캐시 활용을 위해 wrapper와 빌드 설정 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 런타임 이미지
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Railway는 PORT 환경변수로 리스닝 포트를 지정한다
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
