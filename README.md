# 배당금 포트폴리오 관리 앱

보유 주식의 배당금을 추적하고 월별 배당 흐름을 분석하는 웹 애플리케이션입니다.

## 주요 기능

- **대시보드**: 총 투자금, 예상 연 배당금, 목표 달성률, 월별 배당금 차트
- **종목 관리**: 주식 종목 등록/수정/삭제 (티커, 시장, 업종, 배당주기)
- **거래 관리**: 매수/매도 거래 내역 기록
- **배당 정보**: 종목별 1주당 배당금 및 지급월 관리
- **배당 분석**: 연간 목표 설정 및 월별 배당 흐름 분석

## 기술 스택

| 항목 | 내용 |
|------|------|
| Backend | Spring Boot 4.0, Java 17 |
| ORM | Spring Data JPA |
| Database | H2 (파일 기반) |
| Template | Thymeleaf |
| Chart | Chart.js |

## 실행 방법

**JDK 17 이상 필요**

```bash
# 실행
./gradlew bootRun
```

실행 후 브라우저에서 `http://localhost:8080` 접속

## H2 콘솔

`http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/dividend`
- User Name: `sa`
- Password: (없음)

## 프로젝트 구조

```
src/main/java/com/example/dividend/
├── controller/   # 웹 요청 처리
├── entity/       # DB 엔티티 (Stock, Transaction, Dividend, Goal)
├── repository/   # JPA Repository
├── service/      # 비즈니스 로직 (보유 수량, 배당금 계산)
└── dto/          # 데이터 전달 객체
```
