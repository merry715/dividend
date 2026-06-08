# Dividend Portfolio

주식 거래 내역과 보유 현황을 기반으로 예상 배당금과 확정 배당금을 관리하고, 포트폴리오의 투자 비중과 배당 흐름을 분석할 수 있는 배당 포트폴리오 관리 서비스입니다.

Spring Boot 기반 REST API와 React 기반 SPA(Single Page Application)로 개발되었으며, DART·KRX·공공데이터포털·Naver 금융 데이터를 활용하여 종목 및 배당 정보를 제공합니다.

---

## 개발 기간

**2026.05.13 ~ 2026.06.03**

---

## 팀 구성

| 이름  | 역할                                 |
| --- | ---------------------------------- |
| 김민정 | 프로젝트 기획, DB 설계, 서비스 설계, Backend 개발 |
| 장재영 | Backend 개발, API 문서 작성              |
| 정다운 | Frontend 개발 및 UI 개선                       |
| 박민서 | Frontend 개발                |

---

## 주요 기능

### 사용자 인증

* 회원가입 및 로그인
* JWT 기반 인증
* Refresh Token 발급 및 갱신
* BCrypt 비밀번호 암호화
* 사용자 및 관리자 권한 분리

### 종목 관리

* 종목 검색
* 종목 등록, 조회, 수정
* 섹터 변경
* Soft Delete 및 복원
* DART 및 KRX 기반 종목 검색
* 외부 데이터 기반 주가 및 배당 정보 조회

### 거래 관리

* 매수/매도 거래 등록
* 거래 조회, 수정, 삭제
* 종목별 거래 내역 조회
* 연도별 및 거래 유형별 조회
* 보유 수량 자동 계산

### 배당 관리

* 예상 배당 생성
* 실제 수령 배당 확정
* 배당 일정 및 금액 수정
* 월별·연간·누적 배당 집계
* 종목별 배당 조회
* 외부 데이터 기반 배당 정보 보완

### 대시보드 및 분석

* 총 투자금 조회
* 예상 배당금 조회
* 연간 목표 배당금 설정
* 목표 달성률 확인
* 월별 배당 흐름 제공
* 종목별 투자 비중 분석
* 섹터별 투자 비중 분석
* 연도별 배당 분석

### 관리자 기능

* 전체 사용자 통계
* 활성 사용자 통계
* 인기 종목 통계
* 섹터 분포 통계
* 평균 배당 통계
* 배당 데이터 수동 갱신
* 배당 주기 일괄 보정

---

## 외부 데이터 연동

| 데이터           | 활용 목적               |
| ------------- | ------------------- |
| DART Open API | 기업 및 배당 정보 조회       |
| KRX           | 상장 종목 검색            |
| 공공데이터포털       | 배당 데이터 보완           |
| Naver 금융 API  | 종목 가격 조회 및 일일 종가 갱신 |

---

## 기술 스택

| 구분         | 기술                               |
| ---------- | -------------------------------- |
| Backend    | Java 17, Spring Boot 4.0.6       |
| API        | Spring Web, REST API             |
| Security   | Spring Security, JWT, BCrypt     |
| ORM        | Spring Data JPA, Hibernate       |
| Database   | MySQL                            |
| Cache      | Spring Cache, Caffeine           |
| Batch      | Spring Scheduling                |
| Frontend   | React 19, React Router 7, Vite 8 |
| HTTP       | Axios                            |
| Chart      | Chart.js, react-chartjs-2        |
| Test       | JUnit5, Mockito, MockMvc         |
| Deployment | Docker, Vercel                   |

---

## 주요 도메인

| Entity          | 설명            |
| --------------- | ------------- |
| User            | 사용자 계정 및 권한   |
| Stock           | 종목 정보 및 보유 정보 |
| Transaction     | 매수·매도 거래 내역   |
| Dividend        | 예상·확정 배당 정보   |
| Goal            | 연간 목표 배당금     |
| RefreshToken    | JWT 갱신 토큰     |
| StockPriceCache | 최근 주가 캐시      |

---

## 주요 API

| 영역     | 경로                     |
| ------ | ---------------------- |
| 인증     | `/api/v1/auth`         |
| 종목     | `/api/v1/stocks`       |
| 거래     | `/api/v1/transactions` |
| 배당     | `/api/v1/dividends`    |
| 대시보드   | `/api/v1/dashboard`    |
| 분석     | `/api/v1/analysis`     |
| 관리자 통계 | `/api/v1/admin/stats`  |
| 관리자 배당 | `/api/admin/dividends` |

---

## 실행 환경

### Backend

필수 환경

* JDK 17
* MySQL
* Gradle

```sql
CREATE DATABASE dividend
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

환경 변수

```dotenv
DB_URL=
DB_USERNAME=
DB_PASSWORD=

JWT_SECRET=

DART_API_KEY=
DATA_GO_KR_API_KEY=
KRX_API_KEY=
```

실행

```bash
./gradlew bootRun
```

Windows

```powershell
.\gradlew.bat bootRun
```

---

### Frontend

설치

```bash
cd frontend
npm install
```

실행

```bash
npm run dev
```

환경 변수

```dotenv
VITE_API_URL=http://localhost:8080/api/v1
VITE_USE_MOCK=false
```

빌드

```bash
npm run build
```

---

## 테스트

Backend

```powershell
.\gradlew.bat test
```

Frontend

```bash
npm run lint
```

---

## 향후 개선 계획

본 프로젝트는 웹프로그래밍2 팀 프로젝트 결과물을 기반으로 개발되었습니다.

향후 개인 포트폴리오 프로젝트로 확장하여 다음 기능을 추가 개발할 예정입니다.

* 포트폴리오 리밸런싱 기능 구현
* 관심 종목(Watchlist) 기능 추가
* 섹터 목표 비율 관리 기능 추가
* 포트폴리오 평가 및 수익률 분석 기능 고도화
* 배당 예측 및 투자 분석 기능 강화
* UI/UX 개선
* 테스트 코드 확대 및 품질 개선
* 운영 환경용 DB 마이그레이션 도구 도입
* 실시간 금융 데이터 연동 고도화
