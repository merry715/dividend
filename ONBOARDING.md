# 배당금 포트폴리오 관리 앱 — 온보딩 가이드

보유 주식의 배당금을 추적하고 월별 배당 흐름을 분석하는 웹 애플리케이션입니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Backend | Spring Boot 4.0, Java 17 |
| ORM | Spring Data JPA |
| Database | MySQL |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Frontend | React (별도 레포) |
| Chart | Chart.js |

---

## 프로젝트 구조

```
dividend/
├── build.gradle
├── settings.gradle
├── README.md
│
└── src/
    ├── main/
    │   ├── java/com/example/dividend/
    │   │   ├── DividendApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java        # Spring Security 설정
    │   │   │
    │   │   ├── controller/                    # REST API (/api/v1/*)
    │   │   │   ├── HomeController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── StockController.java
    │   │   │   ├── DividendController.java
    │   │   │   ├── TransactionController.java
    │   │   │   └── AnalysisController.java
    │   │   │
    │   │   ├── entity/
    │   │   │   ├── User.java
    │   │   │   ├── Stock.java
    │   │   │   ├── Transaction.java
    │   │   │   ├── Dividend.java
    │   │   │   └── Goal.java
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── StockRepository.java
    │   │   │   ├── TransactionRepository.java
    │   │   │   ├── DividendRepository.java
    │   │   │   └── GoalRepository.java
    │   │   │
    │   │   ├── service/
    │   │   │   └── DashboardService.java      # 보유 수량·배당금 계산
    │   │   │
    │   │   ├── util/
    │   │   │   └── JwtUtil.java               # JWT 생성·검증
    │   │   │
    │   │   └── dto/
    │   │       ├── HoldingDto.java
    │   │       └── MonthlyDividendDto.java
    │   │
    │   └── resources/
    │       └── application.yml
    │
    └── test/
        └── java/com/example/dividend/
            └── DividendApplicationTests.java
```

---

## 로컬 개발 환경 세팅

### 1. 사전 요구사항

- Java 17+
- MySQL 8.0+
- Gradle (또는 `./gradlew` 래퍼 사용)

### 2. MySQL 데이터베이스 생성

```sql
CREATE DATABASE dividend
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. 환경 변수 설정

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/dividend?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | DB 사용자명 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | `yourpassword` |
| `JWT_SECRET` | JWT 서명 키 **(32자 이상, 운영 시 필수 변경)** | `your-256-bit-secret-key-change-in-production` |

> 값을 설정하지 않으면 `application.yml`의 기본값이 적용됩니다. 운영 환경에서는 반드시 환경 변수로 주입하세요.

#### IntelliJ 환경 변수 설정 방법

`Run > Edit Configurations > Environment variables`에 아래 형식으로 입력:

```
DB_USERNAME=root;DB_PASSWORD=yourpassword;JWT_SECRET=your-secret-key
```

#### 터미널 실행 시

```bash
DB_USERNAME=root DB_PASSWORD=yourpassword ./gradlew bootRun
```

### 4. 서버 실행

```bash
./gradlew bootRun
```

서버 기본 포트: `http://localhost:8080`

---

## API 엔드포인트

> 모든 경로는 `/api/v1/` 프리픽스를 사용합니다.

### 공통

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/health` | 서버 상태 확인 |

### 대시보드

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/dashboard` | 총 투자금, 보유 종목, 배당 달성률, 월별 배당 |

### 종목 (Stocks)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/stocks` | 전체 종목 조회 |
| POST | `/api/v1/stocks` | 종목 등록 |
| PUT | `/api/v1/stocks/{id}` | 종목 수정 |
| DELETE | `/api/v1/stocks/{id}` | 종목 삭제 |

```json
// POST /api/v1/stocks - Request Body
{
  "stockName": "삼성전자",
  "ticker": "005930",
  "market": "KOSPI",
  "sector": "반도체",
  "dividendCycle": "분기"
}
```

### 거래 (Transactions)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/transactions` | 전체 거래 내역 조회 |
| POST | `/api/v1/transactions` | 거래 등록 |
| PUT | `/api/v1/transactions/{id}` | 거래 수정 |
| DELETE | `/api/v1/transactions/{id}` | 거래 삭제 |

```json
// POST /api/v1/transactions - Request Body
{
  "stockId": 1,
  "tradeDate": "2025-01-15",
  "tradeType": "매수",
  "quantity": 10,
  "price": 70000,
  "fee": 350
}
```

### 배당 정보 (Dividends)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/dividends` | 전체 배당 정보 조회 |
| POST | `/api/v1/dividends` | 배당 정보 등록 |
| PUT | `/api/v1/dividends/{id}` | 배당 정보 수정 |
| DELETE | `/api/v1/dividends/{id}` | 배당 정보 삭제 |

```json
// POST /api/v1/dividends - Request Body
{
  "stockId": 1,
  "dividendPerShare": 361,
  "paymentMonth": 3,
  "status": "확정"
}
```

### 배당 분석 (Analysis)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/analysis` | 목표 대비 달성률, 월별 배당 현황 |
| POST | `/api/v1/analysis/goal` | 연간 배당 목표 설정 |

```json
// POST /api/v1/analysis/goal - Request Body
{ "targetDividend": 3000000 }
```

---

## 엔티티 관계

```
User
 └─ id, email, password, name, role, createdAt, lastLoginAt
    (향후 JWT 인증/인가에 사용)

Stock
 ├─ Transaction (1:N) — 매수/매도 거래
 └─ Dividend   (1:N) — 지급월별 배당 정보

Goal
 └─ 연간 배당 목표 (단독)
```

---

## Security 현황

- CSRF 비활성화 (REST API)
- 세션 Stateless (JWT 기반 인증 준비 완료)
- 현재 모든 엔드포인트 `permitAll` — 추후 JWT 필터 연결 예정
- `JwtUtil`: 토큰 생성(`generateToken`), 검증(`isTokenValid`), 사용자 추출(`extractUsername`) 구현 완료

---

## 주요 변경 이력

### 2026-05-19

| 항목 | 변경 내용 |
|------|-----------|
| DB | H2 → MySQL 전환, 접속 정보 환경변수 분리 |
| 의존성 | Thymeleaf 제거, Spring Security + JWT 추가 |
| 컨트롤러 | MVC(`@Controller`) → REST API(`@RestController`) 전환 |
| API 경로 | `/api/*` → `/api/v1/*` 로 버전 프리픽스 통일 |
| HTTP 메서드 | GET/POST 위주 → GET/POST/PUT/DELETE RESTful 구조 |
| 신규 파일 | `SecurityConfig`, `JwtUtil`, `User` 엔티티, `UserRepository` 추가 |
| 삭제 | HTML 템플릿, CSS, H2 DB 파일, `data/` 폴더 제거 |
