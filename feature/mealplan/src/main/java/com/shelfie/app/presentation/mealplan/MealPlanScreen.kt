package com.shelfie.app.presentation.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.MealPlanEntry
import com.shelfie.app.domain.model.MealType
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.presentation.common.FALLBACK_INGREDIENT_EMOJI
import com.shelfie.app.presentation.common.HighlightCard
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.ShelfieTopBar
import com.shelfie.app.presentation.theme.extendedColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MealPlanRoute(
    onNavigateUp: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    viewModel: MealPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealPlanEffect.AddedToShoppingList -> snackbarHostState.showSnackbar(
                    if (effect.addedCount == 0) {
                        resources.getString(R.string.plan_shopping_nothing)
                    } else {
                        resources.getQuantityString(R.plurals.recipe_shopping_added, effect.addedCount, effect.addedCount)
                    },
                )
            }
        }
    }
    MealPlanScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onShiftWeek = viewModel::shiftWeek,
        onSetRecipe = viewModel::setRecipe,
        onSetNote = viewModel::setNote,
        onClear = viewModel::clear,
        onOpenRecipe = onOpenRecipe,
        onAddMissing = viewModel::addMissingToShoppingList,
    )
}

/** The slot being edited; kept in saved state as its epoch day and meal. */
private data class SlotKey(val date: LocalDate, val mealType: MealType)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    state: MealPlanUiState,
    onNavigateUp: () -> Unit,
    onShiftWeek: (Long) -> Unit,
    onSetRecipe: (LocalDate, MealType, Long) -> Unit,
    onSetNote: (LocalDate, MealType, String) -> Unit,
    onClear: (LocalDate, MealType) -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onAddMissing: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var editingDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeal by rememberSaveable { mutableStateOf<MealType?>(null) }
    val editing = editingDay?.let { day -> editingMeal?.let { SlotKey(LocalDate.ofEpochDay(day), it) } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { ShelfieTopBar(title = stringResource(R.string.plan_title), onNavigateUp = onNavigateUp) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "week") {
                WeekSwitcher(weekStart = state.weekStart, onShiftWeek = onShiftWeek)
            }
            item(key = "strip") {
                // Tapping a day scrolls to its card: the strip's two items come before the days.
                DayStrip(
                    days = state.days,
                    today = state.today,
                    onSelect = { index -> scope.launch { listState.animateScrollToItem(index + DAY_ITEMS_OFFSET) } },
                )
            }
            item(key = "shopping") {
                FilledTonalButton(
                    onClick = onAddMissing,
                    enabled = state.plannedCount > 0,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_add_shopping), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.plan_add_missing))
                }
            }
            items(state.days, key = { it.date.toEpochDay() }) { day ->
                DayCard(
                    day = day,
                    isToday = day.date == state.today,
                    isPast = day.date.isBefore(state.today),
                    onSlotClick = { meal ->
                        editingDay = day.date.toEpochDay()
                        editingMeal = meal
                    },
                )
            }
        }
    }

    if (editing != null) {
        SlotSheet(
            slot = editing,
            entry = state.days.firstOrNull { it.date == editing.date }?.meals?.get(editing.mealType),
            recipes = state.recipes,
            onDismiss = { editingDay = null; editingMeal = null },
            onPickRecipe = { onSetRecipe(editing.date, editing.mealType, it); editingDay = null },
            onSaveNote = { onSetNote(editing.date, editing.mealType, it); editingDay = null },
            onClear = { onClear(editing.date, editing.mealType); editingDay = null },
            onOpenRecipe = { onOpenRecipe(it); editingDay = null },
        )
    }
}

@Composable
private fun WeekSwitcher(weekStart: LocalDate, onShiftWeek: (Long) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShiftWeek(-1) }) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.plan_previous_week))
        }
        Text(
            text = "${weekStart.format(formatter)} – ${weekStart.plusDays(6).format(formatter)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        IconButton(onClick = { onShiftWeek(1) }) {
            Icon(
                painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.plan_next_week),
                modifier = Modifier.rotate(180f),
            )
        }
    }
}

/** The week at a glance: seven days, today filled, a dot under every day with something planned. */
@Composable
private fun DayStrip(days: List<DayPlan>, today: LocalDate, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { index, day ->
            val isToday = day.date == today
            val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(16.dp),
                color = if (isToday) scheme.primary else scheme.surfaceContainerLowest,
                contentColor = if (isToday) scheme.onPrimary else scheme.onSurface,
                border = if (isToday) null else BorderStroke(1.dp, MaterialTheme.extendedColors.glassBorder),
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (day.date.isBefore(today)) PAST_ALPHA else 1f),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = dayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(text = day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(5.dp)
                            .background(
                                when {
                                    day.meals.isEmpty() -> Color.Transparent
                                    isToday -> scheme.onPrimary
                                    else -> scheme.primary
                                },
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: DayPlan, isToday: Boolean, isPast: Boolean, onSlotClick: (MealType) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dayName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val dateText = remember(day.date) { day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = dayName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isToday) stringResource(R.string.plan_today) else dateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.extendedColors.onHeroVariant else scheme.onSurfaceVariant,
                )
            }
            MealType.entries.forEach { meal ->
                SlotRow(meal = meal, entry = day.meals[meal], onClick = { onSlotClick(meal) })
            }
        }
    }
    // Today is the highlight; past days fade, they're only there for reference.
    if (isToday) {
        HighlightCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), content = content)
    } else {
        ShelfieCard(
            modifier = Modifier.fillMaxWidth().alpha(if (isPast) PAST_ALPHA else 1f),
            shape = RoundedCornerShape(26.dp),
        ) { content() }
    }
}

@Composable
private fun SlotRow(meal: MealType, entry: MealPlanEntry?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(scheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = entry?.recipe?.coverEmoji() ?: meal.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(meal.planLabelRes),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            when {
                entry == null -> Text(
                    text = "+ " + stringResource(R.string.plan_add),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.primary,
                )
                else -> {
                    entry.recipe?.let { saved ->
                        Text(
                            text = saved.recipe.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (entry.note.isNotBlank()) {
                        Text(
                            text = entry.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotSheet(
    slot: SlotKey,
    entry: MealPlanEntry?,
    recipes: List<SavedRecipe>,
    onDismiss: () -> Unit,
    onPickRecipe: (Long) -> Unit,
    onSaveNote: (String) -> Unit,
    onClear: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
) {
    var note by rememberSaveable(slot) { mutableStateOf(entry?.note.orEmpty()) }
    val dayName = slot.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "$dayName · ${stringResource(slot.mealType.planLabelRes)}", style = MaterialTheme.typography.headlineSmall)

            entry?.recipe?.let { saved ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpenRecipe(saved.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.plan_open_recipe), maxLines = 1)
                    }
                    TextButton(onClick = onClear) { Text(stringResource(R.string.plan_clear)) }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.plan_note)) },
                placeholder = { Text(stringResource(R.string.plan_note_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (entry != null && entry.recipe == null) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.plan_clear)) }
                }
                TextButton(onClick = { onSaveNote(note) }) { Text(stringResource(R.string.action_save)) }
            }

            HorizontalDivider(color = MaterialTheme.extendedColors.glassBorder)
            Text(text = stringResource(R.string.plan_pick_recipe), style = MaterialTheme.typography.titleMedium)
            if (recipes.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_no_recipes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            recipes.take(MAX_PICKER_RECIPES).forEach { saved ->
                val isCurrent = saved.id == entry?.recipe?.id
                ShelfieCard(
                    onClick = { onPickRecipe(saved.id) },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        if (isCurrent) 1.5.dp else 1.dp,
                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.extendedColors.glassBorder,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = saved.coverEmoji(), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(saved.recipe.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = pluralStringResource(
                                    R.plurals.recipe_time_minutes,
                                    saved.recipe.cookingTimeMinutes,
                                    saved.recipe.cookingTimeMinutes,
                                ) + if (saved.isFavorite) "  ♥" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun SavedRecipe.coverEmoji(): String =
    recipe.rescuedIngredients.firstNotNullOfOrNull { it.emoji } ?: FALLBACK_INGREDIENT_EMOJI

/** Icon of an empty slot; a planned one shows its recipe's ingredient instead. */
private val MealType.emoji: String
    get() = when (this) {
        MealType.BREAKFAST -> "🍳"
        MealType.LUNCH -> "🥗"
        MealType.DINNER -> "🍲"
    }

/** The week switcher and the day strip come before the day cards. */
private const val DAY_ITEMS_OFFSET = 3

/** Past days stay visible but step back. */
private const val PAST_ALPHA = 0.6f

private val MealType.planLabelRes: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.plan_breakfast
        MealType.LUNCH -> R.string.plan_lunch
        MealType.DINNER -> R.string.plan_dinner
    }

/** Enough to choose from without turning the sheet into the whole history. */
private const val MAX_PICKER_RECIPES = 20
