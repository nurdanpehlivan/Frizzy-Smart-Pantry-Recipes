package com.shelfie.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.SavedRecipe
import com.shelfie.app.domain.repository.MealPlanRepository
import com.shelfie.app.domain.repository.RecipeRepository
import com.shelfie.app.domain.usecase.GetExpiringIngredientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class HistoryFilter { ALL, FAVORITES, COOKED }

data class RecipeHistoryUiState(
    val isLoading: Boolean = true,
    val recipes: List<SavedRecipe> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    /** Whether anything is saved at all, so an empty filter isn't shown as an empty history. */
    val hasAnyRecipe: Boolean = false,
    /** How many recipes each filter would show, for the chips and the header. */
    val counts: Map<HistoryFilter, Int> = emptyMap(),
    /** Pantry items a rescue recipe would use now (critical, not expired); empty when none. */
    val expiring: List<PantryItem> = emptyList(),
    /** Meals planned from today to the end of this week. */
    val plannedThisWeek: Int = 0,
)

@HiltViewModel
class RecipeHistoryViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val getExpiringIngredients: GetExpiringIngredientsUseCase,
    mealPlanRepository: MealPlanRepository,
    clock: Clock,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val expiring = MutableStateFlow<List<PantryItem>>(emptyList())

    private val plannedThisWeek = LocalDate.now(clock).let { today ->
        mealPlanRepository.observeWeek(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
            .map { week -> week.count { !it.date.isBefore(today) && (it.recipe != null || it.note.isNotBlank()) } }
    }

    val uiState: StateFlow<RecipeHistoryUiState> = combine(
        recipeRepository.observeSavedRecipes(),
        filter,
        expiring,
        plannedThisWeek,
    ) { recipes, filter, expiring, planned ->
        RecipeHistoryUiState(
            isLoading = false,
            recipes = when (filter) {
                HistoryFilter.ALL -> recipes
                HistoryFilter.FAVORITES -> recipes.filter { it.isFavorite }
                HistoryFilter.COOKED -> recipes.filter { it.isCooked }
            },
            filter = filter,
            hasAnyRecipe = recipes.isNotEmpty(),
            counts = mapOf(
                HistoryFilter.ALL to recipes.size,
                HistoryFilter.FAVORITES to recipes.count { it.isFavorite },
                HistoryFilter.COOKED to recipes.count { it.isCooked },
            ),
            expiring = expiring,
            plannedThisWeek = planned,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeHistoryUiState())

    init {
        refreshExpiring()
    }

    /** What's about to spoil changes while the tab is away (eaten, scanned, a day passed). */
    fun refreshExpiring() {
        viewModelScope.launch { expiring.value = runCatching { getExpiringIngredients() }.getOrDefault(emptyList()) }
    }

    fun selectFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun toggleFavorite(recipe: SavedRecipe) {
        viewModelScope.launch { recipeRepository.setFavorite(recipe.id, !recipe.isFavorite) }
    }

    fun delete(recipe: SavedRecipe) {
        viewModelScope.launch { recipeRepository.deleteSavedRecipe(recipe.id) }
    }
}
