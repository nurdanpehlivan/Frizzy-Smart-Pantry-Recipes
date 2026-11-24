package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WasteFreeStreakTest {

    private val today = LocalDate.of(2026, 11, 24)

    @Test
    fun `counts whole days since the last thrown away item`() {
        assertEquals(12, WasteFreeStreak.days(today.minusDays(12), today.minusDays(40), today))
    }

    @Test
    fun `throwing something away today resets the streak`() {
        assertEquals(0, WasteFreeStreak.days(today, today.minusDays(40), today))
    }

    @Test
    fun `without any waste the streak runs from the first use`() {
        assertEquals(5, WasteFreeStreak.days(null, today.minusDays(5), today))
    }

    @Test
    fun `no streak before the pantry was used`() {
        assertNull(WasteFreeStreak.days(null, null, today))
    }

    @Test
    fun `a waste date after today (clock change) never goes negative`() {
        assertEquals(0, WasteFreeStreak.days(today.plusDays(1), today.minusDays(3), today))
    }

    // Tuesday 24 Nov 2026; the week starts on Monday 23 Nov.

    @Test
    fun `best is the longest run between two thrown away days`() {
        val summary = WasteFreeStreak.summary(
            wasteDays = setOf(today.minusDays(30), today.minusDays(9)),
            firstUseDay = today.minusDays(40),
            today = today,
        )!!

        // 30 days ago to 9 days ago: the streak reached 20 the day before the second waste.
        assertEquals(9, summary.current)
        assertEquals(20, summary.best)
    }

    @Test
    fun `an unbroken streak is its own best`() {
        val summary = WasteFreeStreak.summary(emptySet(), firstUseDay = today.minusDays(12), today = today)!!

        assertEquals(12, summary.current)
        assertEquals(12, summary.best)
    }

    @Test
    fun `the week marks each day from Monday`() {
        val summary = WasteFreeStreak.summary(
            wasteDays = setOf(today.minusDays(1)),
            firstUseDay = today.minusDays(40),
            today = today,
        )!!

        assertEquals(7, summary.week.size)
        assertEquals(LocalDate.of(2026, 11, 23), summary.week.first().first)
        assertEquals(WasteFreeStreak.Day.WASTED, summary.week[0].second)
        assertEquals(WasteFreeStreak.Day.CLEAN, summary.week[1].second)
        assertEquals(WasteFreeStreak.Day.FUTURE, summary.week[2].second)
    }

    @Test
    fun `days before the first use are not counted as clean`() {
        val summary = WasteFreeStreak.summary(emptySet(), firstUseDay = today, today = today)!!

        assertEquals(WasteFreeStreak.Day.BEFORE_START, summary.week[0].second)
        assertEquals(WasteFreeStreak.Day.CLEAN, summary.week[1].second)
    }

    @Test
    fun `no summary before the pantry was used`() {
        assertNull(WasteFreeStreak.summary(emptySet(), firstUseDay = null, today = today))
    }
}
