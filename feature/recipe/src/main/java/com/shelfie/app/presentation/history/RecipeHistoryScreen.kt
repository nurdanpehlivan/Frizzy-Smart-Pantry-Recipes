package com.shelfie.app.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.presentation.common.FALLBACK_INGREDIENT_EMOJI
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.shelfieFilterChipColors
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.theme.extendedColors
import com.shelfie.app.presentation.theme.freshnessPalette
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RecipeHistoryRoute(
    onOpenRecipe: (Long) -> Unit,
    onNewRecipe: () -> Unit,
    onCookWithWhatIHave: () -> Unit = {},
    onOpenMealPlan: () -> Unit = {},
    viewModel: RecipeHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Something may have been eaten, scanned or spoiled since the tab was last open.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshExpiring()
        onPauseOrDispose {}
    }
    RecipeHistoryScreen(
        state = state,
        onFilterSelected = viewModel::selectFilter,
        onOpenRecipe = { onOpenRecipe(it.id) },
        onToggleFavorite = viewModel::toggleFavorite,
        onDelete = viewModel::delete,
        onNewRecipe = onNewRecipe,
        onCookWithWhatIHave = onCookWithWhatIHave,
        onOpenMealPlan = onOpenMealPlan,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeHistoryScreen(
    state: RecipeHistoryUiState,
    onFilterSelected: (HistoryFilter) -> Unit,
    onOpenRecipe: (SavedRecipe) -> Unit,
    onToggleFavorite: (SavedRecipe) -> Unit,
    onDelete: (SavedRecipe) -> Unit,
    onNewRecipe: () -> Unit,
    onCookWithWhatIHave: () -> Unit = {},
    onOpenMealPlan: () -> Unit = {},
) {
    var pendingDelete by remember { mutableStateOf<SavedRecipe?>(null) }

    Scaffold { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            !state.hasAnyRecipe -> EmptyHistory(onNewRecipe, onCookWithWhatIHave, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                // Bottom padding keeps the last card clear of the dock's raised scan button.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") {
                    ScreenHeader(
                        title = stringResource(R.string.history_title),
                        subtitle = stringResource(
                            R.string.history_counts,
                            state.counts[HistoryFilter.ALL] ?: 0,
                            state.counts[HistoryFilter.COOKED] ?: 0,
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item(key = "hero") {
                    RescueHero(
                        expiring = state.expiring,
                        onNewRecipe = onNewRecipe,
                        onCookWithWhatIHave = onCookWithWhatIHave,
                    )
                }
                item(key = "actions") {
                    // Equal heights whatever the text length: the taller card sets the row.
                    Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction(
                            painter = painterResource(R.drawable.ic_restaurant),
                            title = stringResource(R.string.picker_entry_title),
                            subtitle = stringResource(R.string.history_pick_sub),
                            tile = MaterialTheme.colorScheme.tertiaryContainer,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onCookWithWhatIHave,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        QuickAction(
                            painter = painterResource(R.drawable.ic_calendar),
                            title = stringResource(R.string.plan_title),
                            subtitle = if (state.plannedThisWeek > 0) {
                                pluralStringResource(R.plurals.history_plan_sub, state.plannedThisWeek, state.plannedThisWeek)
                            } else {
                                stringResource(R.string.history_plan_empty)
                            },
                            tile = MaterialTheme.colorScheme.primaryContainer,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenMealPlan,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                item(key = "filters") {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.filter == filter,
                                onClick = { onFilterSelected(filter) },
                                label = { Text(stringResource(filter.labelRes) + "  " + (state.counts[filter] ?: 0)) },
                                shape = CircleShape,
                                colors = shelfieFilterChipColors(),
                            )
                        }
                    }
                }
                if (state.recipes.isEmpty()) {
                    item(key = "emptyFilter") {
                        Text(
                            text = stringResource(R.string.history_filter_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(state.recipes, key = { it.id }) { saved ->
                    HistoryCard(
                        saved = saved,
                        onClick = { onOpenRecipe(saved) },
                        onToggleFavorite = { onToggleFavorite(saved) },
                        onDelete = { pendingDelete = saved },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    pendingDelete?.let { saved ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message, saved.recipe.title)) },
            confirmButton = {
                TextButton(onClick = { onDelete(saved); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private val BubbleShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)

/**
 * The tab's point, on top: chef Frizzy shows what is about to spoil and cooks it in one tap.
 * With nothing at risk, the same card invites cooking with any products instead.
 */
@Composable
private fun RescueHero(
    expiring: List<PantryItem>,
    onNewRecipe: () -> Unit,
    onCookWithWhatIHave: () -> Unit,
) {
    val extended = MaterialTheme.extendedColors
    val hasExpiring = expiring.isNotEmpty()
    HighlightCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Frizzy(mood = FrizzyMood.CHEF, size = 64.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = BubbleShape,
                    color = extended.heroTile,
                    contentColor = extended.onHero,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = if (hasExpiring) {
                                pluralStringResource(R.plurals.history_hero_expiring_title, expiring.size, expiring.size)
                            } else {
                                stringResource(R.string.history_hero_idle_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(if (hasExpiring) R.string.history_hero_expiring_body else R.string.history_hero_idle_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.onHeroVariant,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val critical = MaterialTheme.freshnessPalette.critical
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    expiring.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(critical.container, CircleShape)
                                .clearAndSetSemantics { },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = item.displayEmoji, fontSize = 18.sp)
                        }
                    }
                }
                Button(onClick = if (hasExpiring) onNewRecipe else onCookWithWhatIHave) {
                    Text(stringResource(if (hasExpiring) R.string.history_hero_cta else R.string.history_hero_pick))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** A compact entry point: an icon on a tinted tile, a title and one line of context. */
@Composable
private fun QuickAction(
    painter: Painter,
    title: String,
    subtitle: String,
    tile: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShelfieCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tile, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painter, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    saved: SavedRecipe,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = saved.recipe
    val scheme = MaterialTheme.colorScheme
    val fresh = MaterialTheme.freshnessPalette.fresh
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val date = remember(saved.createdAt) { saved.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter) }
    val emojis = recipe.rescuedIngredients.mapNotNull { it.emoji }.ifEmpty { listOf(FALLBACK_INGREDIENT_EMOJI) }
    val rescued = recipe.rescuedIngredients.size

    ShelfieCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlateThumb(emojis = emojis)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InfoChip(
                        text = "⏱ " + pluralStringResource(R.plurals.recipe_time_minutes, recipe.cookingTimeMinutes, recipe.cookingTimeMinutes),
                        container = scheme.surfaceContainerHigh,
                        content = scheme.onSurface,
                    )
                    if (rescued > 0) {
                        InfoChip(
                            text = "✓ " + pluralStringResource(R.plurals.history_rescued, rescued, rescued),
                            container = fresh.container,
                            content = fresh.onContainer,
                        )
                    }
                    if (saved.isCooked) {
                        InfoChip(
                            text = stringResource(R.string.recipe_badge_cooked),
                            container = scheme.primaryContainer,
                            content = scheme.onPrimaryContainer,
                        )
                    }
                }
                Text(text = date, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        painter = painterResource(if (saved.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                        contentDescription = stringResource(
                            if (saved.isFavorite) R.string.recipe_action_unfavorite else R.string.recipe_action_favorite,
                        ),
                        tint = if (saved.isFavorite) scheme.tertiary else scheme.outline,
                    )
                }
                // Deleting is rare and asks first; it stays in the background.
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete),
                        tint = scheme.outlineVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/** The recipe's "cover": its first two rescued ingredients on a little plate, as on the recipe screen. */
@Composable
private fun PlateThumb(emojis: List<String>) {
    val scheme = MaterialTheme.colorScheme
    val plate = scheme.surfaceContainerLowest
    val bed = scheme.tertiary.copy(alpha = 0.16f)
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.tertiaryContainer)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(62.dp)) {
            val r = size.minDimension / 2
            drawCircle(color = Color.Black.copy(alpha = 0.08f), radius = r * 0.98f, center = center.copy(y = center.y + r * 0.08f))
            drawCircle(color = plate, radius = r)
            drawCircle(color = bed, radius = r * 0.72f)
        }
        Text(text = emojis[0], fontSize = 22.sp, modifier = Modifier.offset(x = (-8).dp, y = (-7).dp))
        emojis.getOrNull(1)?.let {
            Text(text = it, fontSize = 18.sp, modifier = Modifier.offset(x = 11.dp, y = 10.dp))
        }
    }
}

@Composable
private fun EmptyHistory(onNewRecipe: () -> Unit, onCookWithWhatIHave: () -> Unit, modifier: Modifier = Modifier) {
    MascotMessage(
        mood = FrizzyMood.CHEF,
        title = stringResource(R.string.history_empty_title),
        message = stringResource(R.string.history_empty_subtitle),
        primaryAction = stringResource(R.string.dashboard_rescue_action) to onNewRecipe,
        secondaryAction = stringResource(R.string.picker_entry_title) to onCookWithWhatIHave,
        modifier = modifier,
    )
}

private val HistoryFilter.labelRes: Int
    get() = when (this) {
        HistoryFilter.ALL -> R.string.history_filter_all
        HistoryFilter.FAVORITES -> R.string.history_filter_favorites
        HistoryFilter.COOKED -> R.string.history_filter_cooked
    }
