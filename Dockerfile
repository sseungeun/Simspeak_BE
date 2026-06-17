# 1. 빌드 단계
FROM gradle:8.10-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# bootJar를 명시적으로 실행하여 jar 파일을 생성
RUN gradle bootJar --no-daemon -x test

# 2. 실행 단계
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# build/libs 폴더에 있는 모든 .jar 파일을 app.jar로 복사
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]