package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.WasteFreeStreak
import com.shelfie.app.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Days in a row without throwing food away, see [WasteFreeStreak]; `null` before the pantry was
 * used. Built from data the app already keeps: thrown-away items are removed items with
 * [PantryItem.isWasted], and the time they left is their last update, as in the statistics.
 */
class ObserveWasteFreeStreakUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) {

    /** [today] is passed in so the caller's day-change ticker keeps the count current. */
    operator fun invoke(today: LocalDate): Flow<Int?> = summary(today).map { it?.current }.distinctUntilChanged()

    /** The streak with its best run and this week's days, for the statistics. */
    fun summary(today: LocalDate): Flow<WasteFreeStreak.Summary?> = combine(
        pantryRepository.observePantryItems(),
        pantryRepository.observeRemovedItemsSince(Instant.EPOCH),
    ) { active, removed ->
        val wasteDays = removed.filter { it.isWasted }.mapTo(HashSet()) { it.updatedAt.toLocalDate() }
        val firstUse = (active.asSequence() + removed.asSequence()).minOfOrNull { it.createdAt }
        WasteFreeStreak.summary(wasteDays = wasteDays, firstUseDay = firstUse?.toLocalDate(), today = today)
    }.distinctUntilChanged()

    private fun Instant.toLocalDate(): LocalDate = atZone(clock.zone).toLocalDate()
}
