package com.shelfie.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * The "days without throwing food away" streak shown as a flame badge on the pantry.
 *
 * It counts whole days since the last thrown-away item, or since the pantry was first used if
 * nothing was ever thrown away. Throwing something away today makes it 0; tomorrow it is 1.
 * A shared pantry holds the whole household's items, so the streak is the household's.
 */
object WasteFreeStreak {

    /** How one day of the current week went, for the statistics' row of seven days. */
    enum class Day { CLEAN, WASTED, FUTURE, BEFORE_START }

    /**
     * [current] as on the badge, the longest streak so far ([best], at least [current]) and the
     * days of this week, Monday first.
     */
    data class Summary(val current: Int, val best: Int, val week: List<Pair<LocalDate, Day>>)

    /**
     * @param lastWasteDay day the most recent item was thrown away, `null` if none ever was.
     * @param firstUseDay day the first pantry item was added, `null` if the pantry was never used.
     * @return days in the streak, or `null` before the pantry was used at all.
     */
    fun days(lastWasteDay: LocalDate?, firstUseDay: LocalDate?, today: LocalDate): Int? {
        val start = lastWasteDay ?: firstUseDay ?: return null
        return ChronoUnit.DAYS.between(start, today).coerceAtLeast(0).toInt()
    }

    /**
     * The streak in full from every day something was thrown away ([wasteDays]); `null` before
     * the pantry was used. A streak ending at a waste day peaked the day before it.
     */
    fun summary(wasteDays: Set<LocalDate>, firstUseDay: LocalDate?, today: LocalDate): Summary? {
        val firstUse = listOfNotNull(firstUseDay, wasteDays.minOrNull()).minOrNull() ?: return null
        val current = days(wasteDays.maxOrNull(), firstUse, today) ?: return null
        val starts = listOf(firstUse) + wasteDays.sorted()
        val ends = wasteDays.sorted()
        val past = starts.zip(ends).map { (from, to) -> ChronoUnit.DAYS.between(from, to).toInt() - 1 }
        val best = (past + current).max().coerceAtLeast(0)

        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = (0L until 7L).map { offset ->
            val day = monday.plusDays(offset)
            day to when {
                day.isAfter(today) -> Day.FUTURE
                day.isBefore(firstUse) -> Day.BEFORE_START
                day in wasteDays -> Day.WASTED
                else -> Day.CLEAN
            }
        }
        return Summary(current = current, best = best, week = week)
    }
}
