# 배당금 포트폴리오 관리 앱

보유 주식의 배당금을 추적하고 월별 배당 흐름을 분석하는 웹 애플리케이션입니다.

## 주요 기능

- **대시보드**: 총 투자금, 예상 연 배당금, 목표 달성률, 월별 배당금 차트
- **종목 관리**: 주식 종목 등록/수정/삭제 (티커, 시장, 업종, 배당주기)
- **거래 관리**: 매수/매도 거래 내역 기록
- **배당 정보**: 종목별 1주당 배당금 및 지급월 관리
- **배당 분석**: 연간 목표 설정 및 월별 배당 흐름 분석
- **리밸런싱**: 섹터 비중 분석 및 리밸런싱 방향 제안
- **관심 종목**: 관심 종목 등록 및 분할 매수 방향 제안
- **가상 포트폴리오**: 가상 포트폴리오 매수 시뮬레이션

## 기술 스택

| 항목 | 내용 |
|------|------|
| Backend | Spring Boot 4.0, Java 17 |
| ORM | Spring Data JPA |
| Database | MySQL |
| Security | Spring Security + JWT |
| Frontend | React |
| Chart | Chart.js |

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/dividend?...` |
| `DB_USERNAME` | DB 사용자명 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | (없음) |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | 개발용 기본값 |

## 실행 방법

**JDK 17 이상, MySQL 서버 필요**

```bash
# MySQL에 dividend 데이터베이스 생성
CREATE DATABASE dividend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 환경 변수 설정 후 실행
DB_USERNAME=root DB_PASSWORD=yourpassword ./gradlew bootRun
```

## API 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/health` | 서버 상태 확인 |
| GET | `/api/dashboard` | 대시보드 데이터 |
| GET/POST/PUT/DELETE | `/api/stocks` | 종목 관리 |
| GET/POST/PUT/DELETE | `/api/transactions` | 거래 관리 |
| GET/POST/PUT/DELETE | `/api/dividends` | 배당 정보 관리 |
| GET | `/api/analysis` | 배당 분석 |
| POST | `/api/analysis/goal` | 목표 설정 |

## GitHub 브랜치 전략

| 브랜치 | 역할 |
|--------|------|
| `main` | 최종본 |
| `dev` | 통합 브랜치 |
| `be/auth`, `be/stock`, `be/transaction`, `be/dividend`, `be/analysis`, `be/admin` | 백엔드 기능별 브랜치 |
| `fe/login`, `fe/dashboard`, `fe/stock`, `fe/dividend`, `fe/analysis`, `fe/rebalancing` | 프론트엔드 기능별 브랜치 |

**작업 방식**: 각자 브랜치에서 개발 → `dev`로 PR → 확인 후 merge

## 프로젝트 구조

```
src/main/java/com/example/dividend/
├── config/       # Security 설정
├── controller/   # REST API 컨트롤러
├── entity/       # DB 엔티티 (Stock, Transaction, Dividend, Goal)
├── repository/   # JPA Repository
├── service/      # 비즈니스 로직 (보유 수량, 배당금 계산)
├── util/         # JWT 유틸리티
└── dto/          # 데이터 전달 객체
```
