# 🚀 심스픽 (SimSpeak) — Spring Boot Backend
> **"외우지 마세요, 시뮬레이션하세요." (Don't just learn. Simulate it.)**
> 
> 유저의 실시간 회화 데이터 유효성을 검증하고, 서비스 비즈니스 로직을 제어하며, 외부 AI 레이어와의 통신을 관리하는 **심스픽 서비스의 핵심 오케스트레이션 백엔드**입니다.

---

## 🛠 1. 기술 스택 (Tech Stack)

* **Language & Runtime**: Java 17 / Spring Boot 4.0.6
* **Data Access**: Spring Data JPA / Hibernate
* **Database**: 원격 PostgreSQL (Neon Cloud) / JSONB 유연 데이터 적재
* **Security & Auth**: Spring Security, BCrypt Encrypt, Stateless Session (JWT / Guest 익명인증)
* **Documentation**: Springdoc OpenAPI (Swagger UI v2.8.5)
* **Infrastructure**: Docker, docker-compose, Cloud Deploy (Railway)
* **Performance Testing**: k6 Load Testing

---

## 🏗 2. 시스템 아키텍처 (System Architecture)

본 백엔드는 데이터 트랜잭션의 안정성과 영속성을 책임지며, `Controller` ➡️ `Service` ➡️ `Repository` 구조의 엄격한 3계층 아키텍처를 준수합니다.

* **Client (React Native)** ──[HTTP REST]──▶ **Spring Boot Backend (Railway)**
  * **ChatController**: 대화 메시지 수신 처리 및 평가 결과 사후 수집용 비동기 콜백 엔드포인트 제공
  * **ChatService**: 트랜잭션 보장 및 외부 AI 인프라와 WebClient를 활용한 투 트랙(Two-Track) 연동 제어
  * **Repository Layer**: 데이터 중복 최소화 및 네트워크 비용 최적화를 위한 쿼리 튜닝 수행
* **Spring Boot Backend** ──[Spring Data JPA]──▶ **Neon PostgreSQL**
  * 원격 환경에 분리된 데이터베이스 아키텍처 바인딩

---

## 📈 3. 성능 개선 및 최적화 성과 (Performance Tuning)

### 🚀 대화 연속성 보장을 위한 2단계 비동기 연동 아키텍처
* **문제 상황**: 유저가 한 발화에 대해 문법 교정, 콩글리시 적발, 발음 분석 등 무거운 연산을 동시에 수행하느라 **최대 14초의 응답 지연(Latency)**이 발생하여 대화가 단절되는 심각한 병목이 존재했습니다.
* **해결 방안**: 대사 및 오디오 스트리밍을 최우선으로 반환하는 **Track 1(동기 수신)**과 무거운 분석 데이터를 나중에 처리하는 **Track 2(비동기 콜백)** 구조로 분리했습니다.
* **매핑 메커니즘**: 유저 대화 저장 시 **대화 ID(PK)를 먼저 데이터베이스에 선발급**하여 AI 레이어에 전달합니다. 이후 백라운드 연산이 완료된 결과가 백엔드의 `/api/chat/callback` 채널로 유입될 때, 발급했던 ID 기반으로 정확히 매핑하여 오답노트 및 발음 평가 데이터를 후적재하도록 설계했습니다.

### 📊 원격 데이터베이스 N+1 쿼리 전면 수정 (k6 부하 테스트 검증)
해외 리전에 위치한 원격 PostgreSQL 환경 특성상 네트워크 왕복(Round Trip) 비용이 속도 저하의 핵심 원인이었습니다. 화면 렌더링 시 스테이지마다 진행도를 개별 조회하는 전형적인 N+1 쿼리 구조를 발견하고 **IN 절 일괄 조회** 구조로 리팩토링했습니다.

* **측정 환경**: k6, 10 VUs 가상 유저 조건, p95 기준 동일 시점 BEFORE / AFTER 측정

| 호출 화면 / 대상 API | 개선 전 응답 속도 | 개선 후 응답 속도 | 쿼리 발생 빈도 | 비고 |
| :--- | :---: | :---: | :---: | :--- |
| **메인 화면 (`main_status`)** | 4.12s | 1.99s | 51% 감소 | 쿼리 최적화 완료 피크 API |
| **스테이지 목록 (`stages`)** | 2.85s | 0.88s | 69% 감소 | 11개 단건 쿼리 ➡️ 4개 통합 통합 튜닝 |
| **오답 노트 (`corrections`)** | 0.85s | 0.82s | 변화 없음 | 대조군 (쿼리 최적화 제외 API) |

> 💡 **인사이트**: 로직이나 네트워크 환경의 우연성이 아닌 대조군(`corrections`)과의 명확한 비교를 통해, 오직 **순수 쿼리 설계 튜닝(IN 절 일괄 처리 및 O(1) Map 바인딩)**만으로 속도를 혁신적으로 향상시켰음을 객관적 지표로 입증했습니다.

---

## ⚡ 4. 핵심 설계 포인트 (Core Design)

* **JSONB 기반의 영속성 보장**: 대화의 뉘앙스, 문법 오류 원인 등 형태가 유동적이고 복잡한 데이터 구조는 정형화된 RDB 테이블 정규화 대신 PostgreSQL의 `JSONB` 타입을 적극 활용하여 유연성을 확보하고 유실을 차단했습니다.
* **계단식 캐릭터 해금 자동화**: 선행 캐릭터 학습 스테이지를 클리어하면 데이터 트랜잭션 단에서 다음 캐릭터 상태가 안전하게 자동 해금되도록 로직을 격리 및 설계했습니다.
* **Stateless 보안 레이어**: Spring Security 인프라를 기반으로 보안 아키텍처를 설계하였으며, 계정 생성 없이 진입 가능한 익명 게스트 가입 모델(`POST /api/auth/guest`) 시스템을 안전하게 지원합니다.

---

## 📂 5. 백엔드 주요 도메인 및 API 명세

| 도메인 | 주요 엔드포인트 | Method | 설명 |
| :--- | :--- | :---: | :--- |
| **인증** | `/api/auth/guest` | `POST` | UUID 기반 소셜 연동 프리 게스트 가입 및 로그인 |
| **인증** | `/api/auth/signup` / `login` | `POST` | ID/PW 기반 고유 계정 생성 (BCrypt 암호화 적합) |
| **캐릭터** | `/api/characters/status` | `GET` | 홈 화면 전용 - 캐릭터 목록 조회 및 계단식 해금 자동화 처리 |
| **캐릭터** | `/api/characters/{id}/stages` | `GET` | 특정 캐릭터에 속한 스테이지 목록 및 개인 최고 점수 조회 |
| **대화** | `/api/chat/sessions` | `POST` | 신규 대화 룸 세션 개설 및 초기 대사 즉각 반환 |
| **대화** | `/api/chat/message` | `POST` | **[핵심 오케스트레이션]** 프론트엔드 오디오 바이너리 S3 업로드 및 AI 연동 |
| **대화** | `/api/chat/callback` | `POST` | **[비동기 연동]** 외부 엔진의 백그라운드 평가 결과 사후 매핑 저작 통로 |
| **리포트** | `/api/reports/sessions/{id}` | `GET` | 세션 종료 리포트 종합 구성 및 단어별 오답노트 렌더링 소스 제공 |
