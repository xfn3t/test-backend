FROM openjdk:8-jdk-slim

WORKDIR /app

COPY gradlew ./gradlew
COPY gradle /app/gradle
COPY build.gradle.kts settings.gradle gradle.properties ./

# Преобразуем окончания строк в Unix-формат и делаем gradlew исполняемым
RUN apt-get update && apt-get install -y dos2unix \
    && dos2unix gradlew \
    && chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY . .

# Еще раз проверяем и исправляем окончания строк для всех скриптов в проекте
RUN find . -type f -name "*.sh" -exec dos2unix {} + && dos2unix gradlew

RUN ./gradlew shadowJar --no-daemon

CMD ["java", "-jar", "build/libs/test-budget-0.0.1-SNAPSHOT-all.jar"]