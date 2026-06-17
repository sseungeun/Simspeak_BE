# 1. 빌드 단계 (Gradle 빌드)
FROM gradle:8.10-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

# 2. 실행 단계 (경량화된 자바 이미지)
FROM openjdk:17-jdk-slim
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일 복사
# 💡 'build/libs/3rdProject-0.0.1-SNAPSHOT.jar' 부분은 실제 생성되는 jar 파일 이름으로 확인해주세요!
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 컨테이너가 뜰 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]