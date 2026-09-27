# 배포용 단일 이미지 — frontend를 빌드해 backend의 정적 리소스로 같이 패키징한 뒤 jar 하나로 실행.
# 로컬 개발은 이 파일과 무관 (그대로 backend/frontend 따로 실행).

# ── 1단계: 프론트엔드 빌드 ──────────────────────────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build
# 결과물: /app/frontend/dist

# ── 2단계: 백엔드 빌드(+ 프론트 결과물을 정적 리소스로 포함) ────────────────
FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /app/backend
# gradle wrapper·빌드 스크립트만 먼저 복사 → 의존성 레이어 캐싱
COPY backend/gradlew ./
COPY backend/gradle ./gradle
COPY backend/build.gradle backend/settings.gradle ./
RUN chmod +x gradlew && ./gradlew --version
COPY backend/src ./src
# 1단계 결과물을 static 리소스로 (기동 시 SpaForwardController가 SPA 라우팅을 처리)
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
RUN ./gradlew clean bootJar --no-daemon -x test

# ── 3단계: 실행 전용(JRE만) ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
# Render 등이 PORT를 지정해줌(server.port=${PORT:8080}) — 로컬 docker run은 미설정이라 8080
ENV DB_PATH=/app/data/aitms
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
