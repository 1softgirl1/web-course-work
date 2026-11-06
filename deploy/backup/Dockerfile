# 1. Vue UI
FROM node:22-alpine AS ui
WORKDIR /ui
COPY ui/package.json ui/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY ui ./
RUN npm run build

# 2. Spring Boot service with the UI as static resources
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS service
WORKDIR /workspace
COPY service/gradlew service/settings.gradle service/build.gradle service/gradle.properties ./
COPY service/gradle ./gradle
COPY service/src ./src
COPY --from=ui /ui/dist ./src/main/resources/static
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# 3. Runtime: Postgres 17 tools (same major as the registry DB) for dump and the restore check,
#    age for encryption, aws-cli for the bucket, a JRE for the service.
FROM postgres:17-alpine
RUN apk add --no-cache age aws-cli curl tzdata openjdk21-jre-headless

# Yandex Object Storage (like most non-AWS S3) does not accept the CRC checksums newer aws-cli sends by default.
ENV AWS_REQUEST_CHECKSUM_CALCULATION=when_required \
    AWS_RESPONSE_CHECKSUM_VALIDATION=when_required

COPY backup.sh /usr/local/bin/backup
COPY verify.sh /usr/local/bin/verify
COPY restore.sh /usr/local/bin/restore
COPY cloud.sh /usr/local/bin/cloud
COPY network-storage.sh /usr/local/bin/network-storage
RUN chmod +x /usr/local/bin/backup /usr/local/bin/verify /usr/local/bin/restore /usr/local/bin/cloud /usr/local/bin/network-storage
COPY --from=service /workspace/build/libs/backup-service-*.jar /app/backup-service.jar

# Runs as root: the restore check starts a throwaway Postgres via `su postgres`.
# The UI port is published on 127.0.0.1 only (see compose).
EXPOSE 8090
ENTRYPOINT ["java", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "/app/backup-service.jar"]
