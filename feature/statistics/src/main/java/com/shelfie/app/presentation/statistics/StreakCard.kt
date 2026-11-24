package com.shelfie.app.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.WasteFreeStreak
import com.shelfie.app.presentation.theme.chartPalette
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The streak on top of the statistics: the days in a row without throwing food away, the best
 * run so far, and this week as seven marks (eaten-only days ticked, thrown-away days crossed).
 */
@Composable
internal fun StreakCard(streak: WasteFreeStreak.Summary, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = scheme.tertiaryContainer,
        contentColor = scheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(scheme.surfaceContainerLowest, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_streak),
                        contentDescription = null,
                        tint = scheme.tertiary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = streak.current.toString(), style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pluralStringResource(R.plurals.stats_streak_unit, streak.current),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Text(text = stringResource(R.string.stats_streak_body), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (streak.current > 0 && streak.current >= streak.best) {
                            stringResource(R.string.stats_streak_best_now)
                        } else {
                            pluralStringResource(R.plurals.stats_streak_best, streak.best, streak.best)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.tertiary,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                val today = LocalDate.now()
                streak.week.forEach { (day, mark) ->
                    DayMark(day = day, mark = mark, isToday = day == today, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayMark(day: LocalDate, mark: WasteFreeStreak.Day, isToday: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val chart = MaterialTheme.chartPalette
    val locale = LocalConfiguration.current.locales[0]
    val dayName = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val description = when (mark) {
        WasteFreeStreak.Day.CLEAN -> stringResource(R.string.stats_day_clean, dayName)
        WasteFreeStreak.Day.WASTED -> stringResource(R.string.stats_day_wasted, dayName)
        WasteFreeStreak.Day.FUTURE, WasteFreeStreak.Day.BEFORE_START -> dayName
    }
    val outline = scheme.onTertiaryContainer.copy(alpha = 0.35f)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    when (mark) {
                        WasteFreeStreak.Day.CLEAN -> Modifier.background(chart.rescued, CircleShape)
                        WasteFreeStreak.Day.WASTED -> Modifier.background(chart.wasted, CircleShape)
                        WasteFreeStreak.Day.FUTURE -> Modifier.drawBehind {
                            val stroke = 1.5.dp.toPx()
                            drawCircle(
                                color = outline,
                                radius = (size.minDimension - stroke) / 2,
                                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
                            )
                        }
                        WasteFreeStreak.Day.BEFORE_START -> Modifier.border(1.5.dp, outline.copy(alpha = 0.2f), CircleShape)
                    },
                )
                .then(if (isToday) Modifier.border(2.dp, scheme.onTertiaryContainer, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when (mark) {
                WasteFreeStreak.Day.CLEAN -> R.drawable.ic_check
                WasteFreeStreak.Day.WASTED -> R.drawable.ic_close
                else -> null
            }
            if (icon != null) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else null,
        )
    }
}
