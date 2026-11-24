package com.shelfie.app.presentation.recipe.picker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.BottomActionBar
import com.shelfie.app.presentation.common.FreshnessRing
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.common.daysLeftText
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.mascot.MascotMessage
import com.shelfie.app.presentation.theme.extendedColors
import com.shelfie.app.presentation.theme.freshnessPalette
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun IngredientPickerRoute(
    onNavigateUp: () -> Unit,
    onCreateRecipe: (ingredientIds: List<Long>) -> Unit,
    viewModel: IngredientPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IngredientPickerScreen(
        state = state,
        onToggle = viewModel::toggle,
        onNavigateUp = onNavigateUp,
        // Soonest expiry first, so the recipe leads with what needs using most.
        onCreateRecipe = { onCreateRecipe(state.items.map { it.id }.filter { it in state.selectedIds }) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerScreen(
    state: IngredientPickerUiState,
    onToggle: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    onCreateRecipe: () -> Unit,
) {
    Scaffold(
        topBar = { ShelfieTopBar(title = stringResource(R.string.picker_title), onNavigateUp = onNavigateUp) },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                BottomActionBar {
                    Button(
                        onClick = onCreateRecipe,
                        enabled = state.selectedIds.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text(
                            if (state.selectedIds.isEmpty()) {
                                stringResource(R.string.picker_select_prompt)
                            } else {
                                pluralStringResource(R.plurals.picker_create, state.selectedIds.size, state.selectedIds.size)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.items.isEmpty() -> MascotMessage(
                mood = FrizzyMood.SLEEPY,
                title = stringResource(R.string.dashboard_empty_title),
                message = stringResource(R.string.picker_empty),
                primaryAction = stringResource(R.string.action_navigate_up) to onNavigateUp,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "tray") {
                    SelectionTray(
                        selected = state.items.filter { it.id in state.selectedIds },
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                items(state.items, key = { it.id }) { item ->
                    val selected = item.id in state.selectedIds
                    PickerRow(
                        item = item,
                        selected = selected,
                        enabled = selected || state.canPickMore,
                        onToggle = { onToggle(item.id) },
                    )
                }
            }
        }
    }
}

/**
 * Frizzy's pot: one seat per ingredient the recipe can take, filled with what's picked, so the
 * limit is visible before it's hit.
 */
@Composable
private fun SelectionTray(selected: List<PantryItem>, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.extendedColors
    HighlightCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Frizzy(mood = FrizzyMood.CHEF, size = 48.dp, animated = false)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.picker_hint, MAX_PICKED_INGREDIENTS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.onHeroVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${selected.size}/$MAX_PICKED_INGREDIENTS",
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.onHero,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clearAndSetSemantics { }) {
                repeat(MAX_PICKED_INGREDIENTS) { index ->
                    val item = selected.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(
                                if (item != null) {
                                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                                } else {
                                    Modifier.border(1.5.dp, extended.heroBorder, CircleShape)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item != null) Text(text = item.displayEmoji, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(item: PantryItem, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }
    val status = item.freshnessStatus(today)
    ShelfieCard(
        onClick = onToggle.takeIf { enabled },
        shape = RoundedCornerShape(22.dp),
        containerColor = if (selected) scheme.primaryContainer.copy(alpha = 0.45f) else Color.Unspecified,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) scheme.primary else MaterialTheme.extendedColors.glassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(selected)
            },
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FreshnessRing(
                emoji = item.displayEmoji,
                shelfLifeLeft = item.shelfLifeLeft(today),
                status = status,
                size = 52.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${daysLeftText(ChronoUnit.DAYS.between(today, item.expiryDate))} · ${stringResource(item.location.labelRes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.freshnessPalette.forStatus(status).onContainer,
                    maxLines = 1,
                )
            }
            // The row is the toggle; this is its picture.
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .then(
                        if (selected) Modifier.background(scheme.primary, CircleShape) else Modifier.border(2.dp, scheme.outline, CircleShape),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Unpickable rows (the pot is full) step back. */
private const val DISABLED_ALPHA = 0.5f
