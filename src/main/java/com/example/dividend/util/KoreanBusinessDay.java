package com.example.dividend.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 한국 영업일(주말 + 공휴일 제외) 계산 유틸.
 *
 * 배당락일 = 배당기준일 - 1영업일 계산에 사용한다.
 * 공휴일은 직접 관리하는 정적 목록(2025~2028)을 사용한다.
 * (음력 공휴일/대체공휴일 포함, 최선 노력 기준 — 컷오프가 아닌 표시용)
 * 목록에 없는 연도는 주말만 제외된다.
 */
public final class KoreanBusinessDay {

    private KoreanBusinessDay() {}

    /** 한국 법정 공휴일 (2025~2028, 음력·대체공휴일 포함, 유지보수 대상 정적 목록) */
    private static final Set<LocalDate> HOLIDAYS = Set.of(
            // ── 2025 ──
            LocalDate.of(2025, 1, 1),                                   // 신정
            LocalDate.of(2025, 1, 28), LocalDate.of(2025, 1, 29), LocalDate.of(2025, 1, 30), // 설날
            LocalDate.of(2025, 3, 1),  LocalDate.of(2025, 3, 3),       // 삼일절 + 대체
            LocalDate.of(2025, 5, 5),  LocalDate.of(2025, 5, 6),       // 어린이날/부처님오신날 + 대체
            LocalDate.of(2025, 6, 6),                                   // 현충일
            LocalDate.of(2025, 8, 15),                                  // 광복절
            LocalDate.of(2025, 10, 3),                                  // 개천절
            LocalDate.of(2025, 10, 5), LocalDate.of(2025, 10, 6), LocalDate.of(2025, 10, 7), LocalDate.of(2025, 10, 8), // 추석 + 대체
            LocalDate.of(2025, 10, 9),                                  // 한글날
            LocalDate.of(2025, 12, 25),                                 // 성탄절

            // ── 2026 ──
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18), // 설날
            LocalDate.of(2026, 3, 1),  LocalDate.of(2026, 3, 2),       // 삼일절(일) + 대체
            LocalDate.of(2026, 5, 5),
            LocalDate.of(2026, 5, 24), LocalDate.of(2026, 5, 25),      // 부처님오신날(일) + 대체
            LocalDate.of(2026, 6, 6),
            LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 17),      // 광복절(토) + 대체
            LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 28), // 추석 + 대체
            LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5),      // 개천절(토) + 대체
            LocalDate.of(2026, 10, 9),
            LocalDate.of(2026, 12, 25),

            // ── 2027 ──
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 2, 6),  LocalDate.of(2027, 2, 7),  LocalDate.of(2027, 2, 8), LocalDate.of(2027, 2, 9), // 설날(일) + 대체
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 5, 5),
            LocalDate.of(2027, 5, 13),                                  // 부처님오신날
            LocalDate.of(2027, 6, 6),
            LocalDate.of(2027, 8, 15), LocalDate.of(2027, 8, 16),      // 광복절(일) + 대체
            LocalDate.of(2027, 9, 14), LocalDate.of(2027, 9, 15), LocalDate.of(2027, 9, 16), // 추석
            LocalDate.of(2027, 10, 3), LocalDate.of(2027, 10, 4),      // 개천절(일) + 대체
            LocalDate.of(2027, 10, 9), LocalDate.of(2027, 10, 11),     // 한글날(토) + 대체
            LocalDate.of(2027, 12, 25), LocalDate.of(2027, 12, 27),    // 성탄절(토) + 대체

            // ── 2028 ──
            LocalDate.of(2028, 1, 1),
            LocalDate.of(2028, 1, 26), LocalDate.of(2028, 1, 27), LocalDate.of(2028, 1, 28), // 설날
            LocalDate.of(2028, 3, 1),
            LocalDate.of(2028, 5, 2),                                   // 부처님오신날
            LocalDate.of(2028, 5, 5),
            LocalDate.of(2028, 6, 6),
            LocalDate.of(2028, 8, 15),
            LocalDate.of(2028, 10, 2), LocalDate.of(2028, 10, 3), LocalDate.of(2028, 10, 4), LocalDate.of(2028, 10, 5), // 추석(개천절 겹침) + 대체
            LocalDate.of(2028, 10, 9),
            LocalDate.of(2028, 12, 25)
    );

    /** 주말 또는 공휴일이면 true */
    public static boolean isWeekendOrHoliday(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return true;
        return HOLIDAYS.contains(date);
    }

    /** 직전 영업일 (주말·공휴일을 건너뛴 가장 가까운 이전 날짜) */
    public static LocalDate prevBusinessDay(LocalDate date) {
        if (date == null) return null;
        LocalDate prev = date.minusDays(1);
        while (isWeekendOrHoliday(prev)) {
            prev = prev.minusDays(1);
        }
        return prev;
    }
}
