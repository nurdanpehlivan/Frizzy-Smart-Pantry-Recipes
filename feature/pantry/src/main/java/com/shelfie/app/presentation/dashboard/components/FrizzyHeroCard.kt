package com.shelfie.app.presentation.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.glow
import com.shelfie.app.presentation.dashboard.PantryFilter
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors
import com.shelfie.app.presentation.theme.freshnessPalette

private val BubbleShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)

/**
 * The top of the pantry in one card. Frizzy says what needs attention, offers the rescue recipe
 * while something can still be saved, and the green / amber / red bar shows the whole pantry;
 * its two counts filter the list below (tap again to show everything). It replaces the overview
 * cards, the "at a glance" bar and the rescue banner, so the list starts higher on the screen.
 */
@Composable
fun FrizzyHeroCard(
    criticalCount: Int,
    warningCount: Int,
    freshCount: Int,
    selectedFilter: PantryFilter?,
    onFilterSelected: (PantryFilter?) -> Unit,
    showRescueRecipe: Boolean,
    onRescueRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.extendedColors
    val mood: FrizzyMood
    val title: String
    val message: String
    when {
        criticalCount > 0 -> {
            mood = FrizzyMood.WORRIED
            title = pluralStringResource(R.plurals.dashboard_hero_critical_title, criticalCount, criticalCount)
            message = stringResource(R.string.dashboard_hero_critical_message)
        }
        warningCount > 0 -> {
            mood = FrizzyMood.HAPPY
            title = pluralStringResource(R.plurals.dashboard_hero_warning_title, warningCount, warningCount)
            message = stringResource(R.string.dashboard_hero_warning_message)
        }
        else -> {
            mood = FrizzyMood.HAPPY
            title = stringResource(R.string.dashboard_hero_fresh_title)
            message = stringResource(R.string.dashboard_hero_fresh_message)
        }
    }

    HighlightCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Frizzy(mood = mood, size = 68.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = BubbleShape,
                    color = extended.heroTile,
                    contentColor = extended.onHero,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.onHeroVariant,
                        )
                    }
                }
            }
            if (showRescueRecipe) {
                RescueRecipeRow(onClick = onRescueRecipe)
            }
            FreshnessSummary(
                criticalCount = criticalCount,
                warningCount = warningCount,
                freshCount = freshCount,
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
            )
        }
    }
}

/** The rescue recipe as a concrete, tappable row inside the hero card. */
@Composable
private fun RescueRecipeRow(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = scheme.surfaceContainerLowest,
        contentColor = scheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(scheme.tertiaryContainer, RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_restaurant),
                    contentDescription = null,
                    tint = scheme.tertiary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_rescue_title),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.dashboard_rescue_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .glow(MaterialTheme.extendedColors.glow, CircleShape, radius = 10.dp)
                    .background(scheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Whole pantry as one bar, with the two filter counts and the total underneath. */
@Composable
private fun FreshnessSummary(
    criticalCount: Int,
    warningCount: Int,
    freshCount: Int,
    selectedFilter: PantryFilter?,
    onFilterSelected: (PantryFilter?) -> Unit,
) {
    val total = criticalCount + warningCount + freshCount
    if (total == 0) return
    val palette = MaterialTheme.freshnessPalette
    val extended = MaterialTheme.extendedColors
    val barDescription = stringResource(R.string.dashboard_freshness_bar, freshCount, warningCount, criticalCount)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .semantics { contentDescription = barDescription },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            listOf(
                freshCount to palette.fresh.accent,
                warningCount to palette.warning.accent,
                criticalCount to palette.critical.accent,
            ).filter { it.first > 0 }.forEach { (count, color) ->
                val weight by animateFloatAsState(count.toFloat(), label = "heroSegment")
                Box(
                    modifier = Modifier
                        .weight(weight.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.dashboard_product_count_total, total, total),
                style = MaterialTheme.typography.labelMedium,
                color = extended.onHeroVariant,
                modifier = Modifier.weight(1f),
            )
            FilterCount(
                label = "$freshCount ${stringResource(R.string.freshness_fresh)}",
                dot = palette.fresh.accent,
                selected = selectedFilter == PantryFilter.SAFE,
                onClick = { onFilterSelected(if (selectedFilter == PantryFilter.SAFE) null else PantryFilter.SAFE) },
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterCount(
                label = "${criticalCount + warningCount} ${stringResource(R.string.dashboard_use_soon)}",
                dot = if (criticalCount > 0) palette.critical.accent else palette.warning.accent,
                selected = selectedFilter == PantryFilter.USE_SOON,
                onClick = {
                    onFilterSelected(if (selectedFilter == PantryFilter.USE_SOON) null else PantryFilter.USE_SOON)
                },
            )
        }
    }
}

@Composable
private fun FilterCount(label: String, dot: Color, selected: Boolean, onClick: () -> Unit) {
    val extended = MaterialTheme.extendedColors
    val border by animateColorAsState(if (selected) dot else Color.Transparent, label = "filterCountBorder")
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else extended.heroTile,
        contentColor = extended.onHero,
        border = BorderStroke(1.5.dp, border),
        modifier = Modifier.semantics {
            role = Role.Tab
            this.selected = selected
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(8.dp).background(dot, CircleShape))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
