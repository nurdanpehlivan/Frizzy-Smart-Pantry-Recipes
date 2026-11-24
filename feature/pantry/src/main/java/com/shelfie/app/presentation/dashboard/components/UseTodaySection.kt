package com.shelfie.app.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.common.FreshnessRing
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.daysLeftText
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.dashboard.DashboardItem
import com.shelfie.app.presentation.theme.freshnessPalette

/**
 * "Use today": the critical items as big cards in a row, each with one-tap "eaten" and "cook"
 * buttons, so the most important action never needs a swipe or a trip into the list.
 */
@Composable
fun UseTodaySection(
    items: List<DashboardItem>,
    onEaten: (DashboardItem) -> Unit,
    onCook: () -> Unit,
    onOpen: (DashboardItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val critical = MaterialTheme.freshnessPalette.critical
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(10.dp).background(critical.accent, CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.dashboard_use_today),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Pill(text = items.size.toString(), containerColor = critical.container, contentColor = critical.onContainer)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items = items, key = { it.item.id }) { entry ->
                UseTodayCard(
                    entry = entry,
                    onEaten = { onEaten(entry) },
                    onCook = onCook,
                    onOpen = { onOpen(entry) },
                )
            }
        }
    }
}

@Composable
private fun UseTodayCard(
    entry: DashboardItem,
    onEaten: () -> Unit,
    onCook: () -> Unit,
    onOpen: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = MaterialTheme.freshnessPalette.forStatus(entry.status)
    val fresh = MaterialTheme.freshnessPalette.fresh
    val name = entry.item.name

    ShelfieCard(onClick = onOpen, shape = RoundedCornerShape(26.dp), modifier = Modifier.width(152.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FreshnessRing(
                emoji = entry.item.displayEmoji,
                shelfLifeLeft = entry.shelfLifeLeft,
                status = entry.status,
                size = 76.dp,
                strokeWidth = 6.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = (if (entry.item.isExpiryEstimated) "≈ " else "") + daysLeftText(entry.daysLeft),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onContainer,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.dashboard_action_eaten_item, name),
                    container = fresh.container,
                    content = fresh.onContainer,
                    onClick = onEaten,
                )
                QuickAction(
                    painter = painterResource(R.drawable.ic_restaurant),
                    contentDescription = stringResource(R.string.dashboard_action_cook_item, name),
                    container = scheme.tertiaryContainer,
                    content = scheme.onTertiaryContainer,
                    onClick = onCook,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    painter: Painter,
    contentDescription: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
    ) {
        Box(modifier = Modifier.size(width = 52.dp, height = 44.dp), contentAlignment = Alignment.Center) {
            Icon(painter = painter, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}
