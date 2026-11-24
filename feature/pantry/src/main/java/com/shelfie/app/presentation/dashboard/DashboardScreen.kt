package com.shelfie.app.presentation.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.domain.model.FreshnessThresholds
import com.shelfie.app.domain.model.MeasurementUnit
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.GlassIconButton
import com.shelfie.app.presentation.common.Pill
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.rangeText
import com.shelfie.app.presentation.common.titleRes
import com.shelfie.app.presentation.dashboard.components.AddItemSheet
import com.shelfie.app.presentation.dashboard.components.FrizzyHeroCard
import com.shelfie.app.presentation.dashboard.components.ItemEditSheet
import com.shelfie.app.presentation.dashboard.components.LocationChips
import com.shelfie.app.presentation.dashboard.components.PantrySearchField
import com.shelfie.app.presentation.dashboard.components.QuickAddButton
import com.shelfie.app.presentation.dashboard.components.SwipeablePantryItemCard
import com.shelfie.app.presentation.dashboard.components.UseTodaySection
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.theme.ShelfieTheme
import com.shelfie.app.presentation.theme.freshnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

/**
 * @param savedItemsCount number of items the scanner just saved, shown once as a snackbar;
 *   [onSavedItemsMessageShown] clears it so it isn't shown again.
 */
@Composable
fun DashboardRoute(
    onNavigateToScanner: () -> Unit,
    onNavigateToRecipe: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    savedItemsCount: Int? = null,
    onSavedItemsMessageShown: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val resources = LocalResources.current
    val currentOnNavigateToScanner by rememberUpdatedState(onNavigateToScanner)
    val currentOnNavigateToRecipe by rememberUpdatedState(onNavigateToRecipe)
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)
    val currentOnNavigateToNotifications by rememberUpdatedState(onNavigateToNotifications)
    val currentOnNavigateToStatistics by rememberUpdatedState(onNavigateToStatistics)
    val unseenActivity by viewModel.unseenActivity.collectAsStateWithLifecycle()
    val wasteFreeStreak by viewModel.wasteFreeStreak.collectAsStateWithLifecycle()

    // The household may have changed things while the app was away.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshActivity()
        onPauseOrDispose {}
    }
    val addItem by viewModel.addItem.collectAsStateWithLifecycle()
    val editItem by viewModel.editItem.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        DashboardEffect.NavigateToScanner -> currentOnNavigateToScanner()
                        DashboardEffect.NavigateToRecipe -> currentOnNavigateToRecipe()
                        DashboardEffect.NavigateToSettings -> currentOnNavigateToSettings()
                        DashboardEffect.NavigateToNotifications -> currentOnNavigateToNotifications()
                        DashboardEffect.NavigateToStatistics -> currentOnNavigateToStatistics()

                        is DashboardEffect.ItemAdded -> snackbarScope.launch {
                            snackbarHostState.showSnackbar(
                                resources.getString(R.string.add_item_saved, effect.itemName),
                            )
                        }

                        is DashboardEffect.ShowRemovalUndo -> snackbarScope.launch {
                            // Replace, don't queue: after several swipes only the last undo matters.
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val message = resources.getString(
                                when (effect.reason) {
                                    RemovalReason.CONSUMED -> R.string.dashboard_removed_consumed
                                    RemovalReason.WASTED -> R.string.dashboard_removed_wasted
                                },
                                effect.itemName,
                            )
                            val result = snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = resources.getString(R.string.action_undo),
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.onIntent(DashboardIntent.UndoRemoval(effect.itemId))
                            }
                        }

                        is DashboardEffect.ShowItemMessage -> snackbarScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = resources.getString(effect.messageRes, effect.itemName, effect.extra),
                                actionLabel = effect.undo?.let { resources.getString(R.string.action_undo) },
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed && effect.undo != null) {
                                viewModel.onIntent(DashboardIntent.UndoChange(effect.undo))
                            }
                        }

                        is DashboardEffect.ShowExpiredDiscarded -> snackbarScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = resources.getString(
                                    R.string.dashboard_expired_discarded,
                                    effect.itemIds.size,
                                ),
                                actionLabel = resources.getString(R.string.action_undo),
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.onIntent(DashboardIntent.UndoDiscardExpired(effect.itemIds))
                            }
                        }

                        is DashboardEffect.ShowMessage -> snackbarScope.launch {
                            snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(savedItemsCount) {
        val count = savedItemsCount ?: return@LaunchedEffect
        // Clearing the result changes this effect's key, so show the snackbar in a scope that
        // outlives it.
        onSavedItemsMessageShown()
        snackbarScope.launch {
            snackbarHostState.showSnackbar(
                resources.getQuantityString(R.plurals.scanner_items_saved, count, count),
            )
        }
    }

    DashboardScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        unseenActivity = unseenActivity,
        wasteFreeStreak = wasteFreeStreak,
    )

    editItem?.let { item ->
        ItemEditSheet(item = item, onIntent = viewModel::onIntent)
    }

    addItem?.let { addState ->
        AddItemSheet(state = addState, onIntent = viewModel::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onIntent: (DashboardIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    /** News from the household since the user last looked: a badge on the family button. */
    unseenActivity: Int = 0,
    /** Days without throwing food away; `null` hides the flame badge, see [StreakBadge]. */
    wasteFreeStreak: Int? = null,
) {
    val listState = rememberLazyListState()
    // Open while there is a query, so coming back to the tab keeps the search visible.
    var isSearching by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The empty state has its own big buttons for the same actions.
            // Scanning is the raised button in the middle of the dock; manual entry stays here.
            if (!state.isEmpty) {
                QuickAddButton(onClick = { onIntent(DashboardIntent.OpenAddItem) })
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.isEmpty -> Box(modifier = Modifier.padding(innerPadding)) {
                MascotMessage(
                    mood = FrizzyMood.SLEEPY,
                    title = stringResource(R.string.dashboard_empty_title),
                    message = stringResource(R.string.dashboard_empty_subtitle),
                    primaryAction = stringResource(R.string.dashboard_action_scan) to { onIntent(DashboardIntent.ScanNewPhoto) },
                    secondaryAction = stringResource(R.string.add_item_title) to { onIntent(DashboardIntent.OpenAddItem) },
                )
                // A new member's pantry starts empty; family and settings must still be reachable.
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 20.dp)) {
                    HeaderActions(unseenActivity = unseenActivity, wasteFreeStreak = wasteFreeStreak, onIntent = onIntent)
                }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                // Bottom padding keeps the last card clear of the scan actions.
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 168.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") {
                    ScreenHeader(
                        title = stringResource(R.string.dashboard_title),
                        subtitle = stringResource(R.string.dashboard_subtitle),
                        actions = {
                            GlassIconButton(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.dashboard_search),
                                onClick = { isSearching = true },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HeaderActions(unseenActivity = unseenActivity, wasteFreeStreak = wasteFreeStreak, onIntent = onIntent)
                        },
                    )
                }
                if (isSearching) {
                    item(key = "search") {
                        PantrySearchField(
                            query = state.query,
                            onQueryChange = { onIntent(DashboardIntent.Search(it)) },
                            onClose = {
                                onIntent(DashboardIntent.Search(""))
                                isSearching = false
                            },
                        )
                    }
                }
                item(key = "hero") {
                    FrizzyHeroCard(
                        criticalCount = state.critical.size,
                        warningCount = state.warning.size,
                        freshCount = state.fresh.size,
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = { onIntent(DashboardIntent.SelectFilter(it)) },
                        showRescueRecipe = state.hasRescuableItems,
                        onRescueRecipe = { onIntent(DashboardIntent.OpenRescueRecipe) },
                    )
                }
                item(key = "locations") {
                    LocationChips(
                        selected = state.locationFilter,
                        counts = state.locationCounts,
                        onSelect = { onIntent(DashboardIntent.SelectLocation(it)) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (state.isFiltered && state.totalCount == 0) {
                    item(key = "noResults") {
                        Text(
                            text = if (state.query.isNotBlank()) {
                                stringResource(R.string.dashboard_search_empty, state.query.trim())
                            } else {
                                stringResource(R.string.dashboard_group_empty)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        )
                    }
                    return@LazyColumn
                }
                // Critical items get the big "use today" cards instead of a list group.
                if (FreshnessStatus.CRITICAL in state.visibleGroups && state.critical.isNotEmpty()) {
                    item(key = "useToday") {
                        UseTodaySection(
                            items = state.critical,
                            onEaten = { onIntent(DashboardIntent.MarkConsumed(it.item.id)) },
                            onCook = { onIntent(DashboardIntent.OpenRescueRecipe) },
                            onOpen = { onIntent(DashboardIntent.OpenItem(it.item.id)) },
                        )
                    }
                    val expiredCount = state.expiredItems.size
                    if (expiredCount > 0) {
                        item(key = "discardExpired") {
                            DiscardExpiredButton(
                                count = expiredCount,
                                onConfirm = { onIntent(DashboardIntent.DiscardExpired) },
                            )
                        }
                    }
                }

                item(key = "swipeHint") {
                    Text(
                        text = stringResource(R.string.dashboard_swipe_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }

                state.visibleGroups.filter { it != FreshnessStatus.CRITICAL }.forEach { status ->
                    val items = state.itemsFor(status)
                    stickyHeader(key = "header_$status") {
                        GroupHeader(status = status, count = items.size, thresholds = state.thresholds)
                    }
                    if (items.isEmpty()) {
                        item(key = "empty_$status") {
                            Text(
                                text = stringResource(R.string.dashboard_group_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            )
                        }
                    }
                    items(items = items, key = { it.item.id }) { entry ->
                        SwipeablePantryItemCard(
                            entry = entry,
                            onConsumed = { onIntent(DashboardIntent.MarkConsumed(entry.item.id)) },
                            onWasted = { onIntent(DashboardIntent.MarkWasted(entry.item.id)) },
                            onClick = { onIntent(DashboardIntent.OpenItem(entry.item.id)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

/** One tap (plus a confirmation) to throw away every expired item instead of swiping each. */
@Composable
private fun DiscardExpiredButton(count: Int, onConfirm: () -> Unit) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { showConfirm = true }) {
            Text(
                text = "🗑  " + stringResource(R.string.dashboard_discard_expired, count),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.dashboard_discard_expired_title)) },
            text = { Text(stringResource(R.string.dashboard_discard_expired_message, count)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onConfirm()
                }) {
                    Text(
                        stringResource(R.string.dashboard_discard_expired_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GroupHeader(status: FreshnessStatus, count: Int, thresholds: FreshnessThresholds) {
    val colors = MaterialTheme.freshnessPalette.forStatus(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Opaque so cards scrolling under the sticky header don't show through.
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(colors.accent, CircleShape),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(status.titleRes),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status.rangeText(thresholds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Pill(text = count.toString(), containerColor = colors.container, contentColor = colors.onContainer)
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    val today = LocalDate.of(2026, 10, 8)
    fun entry(id: Long, name: String, category: Category, days: Long) = DashboardItem(
        item = PantryItem(
            id = id,
            name = name,
            category = category,
            unit = MeasurementUnit.PIECE,
            purchaseDate = today,
            expiryDate = today.plusDays(days),
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        ),
        daysLeft = days,
        status = FreshnessStatus.from(today.plusDays(days), today),
    )
    ShelfieTheme {
        DashboardScreen(
            state = DashboardUiState(
                isLoading = false,
                critical = listOf(entry(1, "Süt", Category.DAIRY, 1), entry(2, "Tavuk", Category.MEAT, 0)),
                warning = listOf(entry(3, "Domates", Category.VEGETABLES, 4)),
                fresh = listOf(entry(4, "Pirinç", Category.GRAINS, 120)),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardEmptyPreview() {
    ShelfieTheme {
        DashboardScreen(state = DashboardUiState(isLoading = false), onIntent = {})
    }
}

/**
 * The waste-free streak, notifications (with a badge for news from the household) and settings,
 * top right of the pantry.
 */
@Composable
private fun HeaderActions(unseenActivity: Int, wasteFreeStreak: Int?, onIntent: (DashboardIntent) -> Unit) {
    // Shown from the pantry's first day, also at 0: it is the way into the statistics.
    if (wasteFreeStreak != null) {
        StreakBadge(days = wasteFreeStreak, onClick = { onIntent(DashboardIntent.OpenStatistics) })
        Spacer(modifier = Modifier.width(8.dp))
    }
    BadgedBox(
        badge = {
            if (unseenActivity > 0) Badge { Text(if (unseenActivity > MAX_BADGE) "$MAX_BADGE+" else "$unseenActivity") }
        },
    ) {
        GlassIconButton(
            painter = painterResource(R.drawable.ic_notifications),
            contentDescription = stringResource(R.string.notifications_title),
            onClick = { onIntent(DashboardIntent.OpenNotifications) },
        )
    }
    Spacer(modifier = Modifier.width(8.dp))
    GlassIconButton(
        painter = painterResource(R.drawable.ic_settings),
        contentDescription = stringResource(R.string.settings_title),
        onClick = { onIntent(DashboardIntent.OpenSettings) },
    )
}

private const val MAX_BADGE = 9

/**
 * Flame and the number of days in a row without throwing food away; opens the statistics.
 * On day 0 (right after something was thrown away) it turns a quiet grey instead of carrot,
 * so the header never scolds but the statistics stay one tap away.
 */
@Composable
private fun StreakBadge(days: Int, onClick: () -> Unit) {
    val description = pluralStringResource(R.plurals.streak_badge_description, days, days)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (days > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (days > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .height(44.dp)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 10.dp, end = 14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_streak),
                contentDescription = null,
                tint = if (days > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(text = days.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
