package com.example.dividend.dto.response;

/**
 * 배당 지급월 + 지급연도 묶음.
 * 연도가 다른 지급일(예: 분기배당의 다음해 1분기)을 프론트에서
 * "2027.4월"처럼 연.월로 구분 표시할 수 있도록 연도 정보를 함께 전달한다.
 */
public record PaymentMonthDto(int month, int year) {}
