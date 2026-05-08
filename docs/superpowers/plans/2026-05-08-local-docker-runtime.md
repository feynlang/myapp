# 로컬 Docker 런타임 구현 계획

> **에이전트 워커용:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`(권장) 또는 `superpowers:executing-plans`를 사용해 이 계획을 작업 단위별로 구현합니다. 진행 추적에는 체크박스(`- [ ]`) 문법을 사용합니다.

**목표:** 컨테이너 친화적인 운영 설정을 바탕으로, Spring Boot 앱과 PostgreSQL을 Docker Compose로 함께 실행할 수 있는 최소한의 로컬 Docker 자산을 추가합니다.

**아키텍처:** 앱은 기존 저장소 안에 그대로 두고, 멀티 스테이지 `Dockerfile`로 Spring Boot JAR를 빌드한 뒤 `compose.yaml`로 `app + db`를 함께 실행합니다. datasource와 OAuth 값은 환경변수로 받아서, 나중에 VM 배포와 CI/CD에서도 같은 설정 모델을 재사용할 수 있게 합니다.

**기술 스택:** Spring Boot, Gradle wrapper, Docker, Docker Compose, PostgreSQL 16, GitHub OAuth, Spring Actuator

---

## 파일 구조

### 수정할 파일

- `src/main/resources/application-prod.yml`
  - 운영 환경에 고정된 호스트 가정을 제거하고, 컨테이너 안에서도 동작하는 환경변수 기반 값으로 바꿉니다.

### 생성할 파일

- `Dockerfile`
  - Spring Boot 앱을 위한 멀티 스테이지 이미지 빌드 정의
- `.dockerignore`
  - Docker 빌드 컨텍스트에서 git 메타데이터, 빌드 산출물, IDE 파일, 비밀값을 제외
- `compose.yaml`
  - 로컬 `app`, `db` 서비스와 헬스체크, 포트, 볼륨 정의
- `.env.example`
  - Docker Compose에 필요한 로컬 환경변수 문서화

### 검증 명령어

- `./gradlew test`
- `docker compose config`
- `docker compose up --build -d`
- `docker compose ps`
- `curl -fsS http://localhost:8080/actuator/health`
- `docker compose logs app --tail=100`
- `docker compose down`

### 수동 검증

- `http://localhost:8080` 열기
- 메인 페이지 확인
- 로컬 계정으로 회원가입 또는 로그인
- 질문/답변 데이터 생성, 조회, 수정, 삭제
- 이번 단계에서는 GitHub OAuth 로그인 제외

## Task 1: 운영 설정을 컨테이너 친화적으로 바꾸기

**파일:**
- 수정: `src/main/resources/application-prod.yml`
- 테스트: `./gradlew test`

- [ ] **Step 1: 운영 datasource와 OAuth 값을 환경변수 기반 속성으로 교체**

파일을 아래 내용으로 교체합니다.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    driver-class-name: org.postgresql.Driver
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false

  h2:
    console:
      enabled: false

  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            redirect-uri: ${GITHUB_REDIRECT_URI}

logging:
  level:
    root: INFO

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never

server:
  address: ${SERVER_ADDRESS:127.0.0.1}
```

- [ ] **Step 2: 설정 변경이 기존 테스트를 깨지 않았는지 확인**

실행:

```bash
./gradlew test
```

기대 결과:

- Gradle이 종료 코드 `0`으로 끝남
- 기존 테스트가 모두 통과함

- [ ] **Step 3: 설정 변경만 먼저 커밋**

실행:

```bash
git add src/main/resources/application-prod.yml
git commit -m "chore: make prod config container-friendly"
```

## Task 2: Docker 이미지 빌드 자산 추가

**파일:**
- 생성: `Dockerfile`
- 생성: `.dockerignore`

- [ ] **Step 1: `Dockerfile` 생성**

`Dockerfile`을 아래 내용으로 생성합니다.

```dockerfile
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src src

RUN ./gradlew bootJar --no-daemon
RUN cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /tmp/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /tmp/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 2: Docker 빌드 컨텍스트 제외 파일 생성**

`.dockerignore`를 아래 내용으로 생성합니다.

```gitignore
.git
.github
.gradle
.idea
.settings
.vscode
bin
build
docs
*.log
.classpath
.project
.env
```

- [ ] **Step 3: `Dockerfile`이 정상 동작하는지 이미지 빌드로 검증**

실행:

```bash
docker build -t myapp-local:test .
```

기대 결과:

- Docker 이미지 빌드가 성공적으로 완료됨
- 최종 이미지가 `myapp-local:test` 태그로 생성됨

- [ ] **Step 4: Docker 이미지 자산 변경 커밋**

실행:

```bash
git add Dockerfile .dockerignore
git commit -m "chore: add docker build assets"
```

## Task 3: 로컬 Compose 스택 추가

**파일:**
- 생성: `compose.yaml`
- 생성: `.env.example`

- [ ] **Step 1: 로컬 Compose 스택 정의 작성**

`compose.yaml`을 아래 내용으로 생성합니다.

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-myappdb}
      POSTGRES_USER: ${POSTGRES_USER:-myapp}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-change-me}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-myapp} -d ${POSTGRES_DB:-myappdb}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 10s

  app:
    build:
      context: .
      dockerfile: Dockerfile
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB:-myappdb}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-myapp}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-change-me}
      GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID:-dummy}
      GITHUB_CLIENT_SECRET: ${GITHUB_CLIENT_SECRET:-dummy}
      GITHUB_REDIRECT_URI: ${GITHUB_REDIRECT_URI:-http://localhost:8080/login/oauth2/code/github}
      SERVER_ADDRESS: 0.0.0.0
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:8080/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    restart: unless-stopped

volumes:
  postgres_data:
```

- [ ] **Step 2: 로컬 환경변수 예시 파일 생성**

`.env.example`을 아래 내용으로 생성합니다.

```dotenv
POSTGRES_DB=myappdb
POSTGRES_USER=myapp
POSTGRES_PASSWORD=change-me
GITHUB_CLIENT_ID=replace-me
GITHUB_CLIENT_SECRET=replace-me
GITHUB_REDIRECT_URI=http://localhost:8080/login/oauth2/code/github
```

- [ ] **Step 3: 컨테이너를 띄우기 전에 Compose 설정 검증**

실행:

```bash
docker compose config
```

기대 결과:

- Compose 설정이 문법 오류 없이 정상 렌더링됨

- [ ] **Step 4: 로컬 스택 실행**

실행:

```bash
cp .env.example .env
docker compose up --build -d
```

기대 결과:

- `db`와 `app` 컨테이너가 모두 시작됨
- `db`가 먼저 healthy 상태가 됨
- Spring Boot 기동이 끝난 뒤 `app`도 healthy 상태가 됨

- [ ] **Step 5: Compose 스택 파일 커밋**

실행:

```bash
git add compose.yaml .env.example
git commit -m "feat: add local docker compose stack"
```

## Task 4: 로컬 Docker 런타임 종단 검증

**파일:**
- 테스트: `compose.yaml`
- 테스트: `src/main/resources/application-prod.yml`

- [ ] **Step 1: 컨테이너 상태와 헬스 확인**

실행:

```bash
docker compose ps
```

기대 결과:

- `db`가 `healthy`로 표시됨
- `app`이 `healthy`로 표시됨

- [ ] **Step 2: 호스트에서 앱 헬스 엔드포인트 확인**

실행:

```bash
curl -fsS http://localhost:8080/actuator/health
```

기대 결과:

- `"status":"UP"`를 포함한 JSON 응답이 반환됨

- [ ] **Step 3: 최근 앱 로그에서 datasource 또는 기동 오류 확인**

실행:

```bash
docker compose logs app --tail=100
```

기대 결과:

- datasource 연결 실패가 반복되지 않음
- 앱이 크래시 루프에 빠지지 않음

- [ ] **Step 4: 브라우저 수동 스모크 테스트 수행**

확인 항목:

- `http://localhost:8080` 열기
- 홈 페이지가 정상 렌더링되는지 확인
- 로컬 폼 기반 회원가입 또는 로그인 수행
- 질문 생성
- 질문 상세 페이지 열기
- 답변 생성
- 현재 사용자 흐름에서 지원된다면 수정 또는 삭제도 확인

기대 결과:

- OAuth를 제외한 핵심 기능 흐름이 컨테이너화 이전과 동일하게 동작함

- [ ] **Step 5: 데이터를 지우지 않고 스택 종료**

실행:

```bash
docker compose down
```

기대 결과:

- 컨테이너는 중지되고 제거됨
- PostgreSQL named volume은 다음 실행을 위해 그대로 유지됨

- [ ] **Step 6: 검증 중 수정사항이 생겼다면 마일스톤 상태 확인**

실행:

```bash
git status --short
```

기대 결과:

- 검증 중 실제 수정이 발생하지 않았다면 미커밋 변경이 없음

## 자체 점검

- 스펙 커버리지:
  - 로컬 Docker 이미지 자산 생성은 Task 2에서 다룸
  - `app + db` 로컬 Compose 런타임은 Task 3에서 다룸
  - 헬스체크와 의존성 순서는 Task 3과 Task 4에서 다룸
  - 브라우저 수준 성공 기준은 Task 4에서 다룸
- 플레이스홀더 점검:
  - `TODO`, `TBD`, 혹은 "나중에 구현" 같은 일반 문구가 남아 있지 않음
- 타입 일관성:
  - datasource 환경변수 이름은 일관되게 `SPRING_DATASOURCE_*` 사용
  - OAuth 환경변수 이름은 일관되게 `GITHUB_*` 사용
  - 런타임 포트는 일관되게 `8080` 사용
