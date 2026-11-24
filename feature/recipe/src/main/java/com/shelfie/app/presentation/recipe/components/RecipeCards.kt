package com.shelfie.app.presentation.recipe.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.Recipe
import com.shelfie.app.domain.model.RecipeIngredient
import com.shelfie.app.domain.model.RecipeStep
import com.shelfie.app.presentation.common.FALLBACK_INGREDIENT_EMOJI
import com.shelfie.app.presentation.common.FreshnessRing
import com.shelfie.app.presentation.common.SectionHeader
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.foodEmoji
import com.shelfie.app.presentation.theme.extendedColors
import com.shelfie.app.presentation.theme.freshnessPalette
import java.time.LocalDate

/**
 * Emoji for [ingredient]: the model's choice, else the category emoji of the matching pantry
 * item, else [FALLBACK_INGREDIENT_EMOJI].
 */
fun ingredientEmoji(ingredient: RecipeIngredient, pantryItems: List<PantryItem>): String =
    ingredient.emoji
        ?: pantryItems.firstOrNull { it.name.equals(ingredient.name, ignoreCase = true) }
            ?.displayEmoji
        ?: foodEmoji(ingredient.name, Category.OTHER).takeIf { it != Category.OTHER.emoji }
        ?: FALLBACK_INGREDIENT_EMOJI

/** At most this many ingredients sit on the plate; more would crowd it. */
private const val MAX_ON_PLATE = 5

/** Where each emoji sits on the plate, from its centre, biggest first. */
private val PlateSpots = listOf(
    DpOffset((-26).dp, (-22).dp),
    DpOffset(26.dp, (-6).dp),
    DpOffset((-18).dp, 26.dp),
    DpOffset(28.dp, 32.dp),
    DpOffset(2.dp, 2.dp),
)

/**
 * Header of the recipe. AI recipes have no photo, so the rescued ingredients become one: their
 * emojis on a plate on a warm carrot ground. Then the "rescue recipe" tag, the title and the
 * key facts.
 */
@Composable
fun RecipeHeroCard(
    recipe: Recipe,
    rescuedEmojis: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RecipePlate(emojis = rescuedEmojis.take(MAX_ON_PLATE))
        Text(
            text = stringResource(R.string.recipe_title).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRowFacts(
            facts = listOf(
                "⏱" to pluralStringResource(
                    R.plurals.recipe_time_minutes,
                    recipe.cookingTimeMinutes,
                    recipe.cookingTimeMinutes,
                ),
                "🍽" to pluralStringResource(R.plurals.recipe_servings, recipe.servings, recipe.servings),
                "✓" to pluralStringResource(
                    R.plurals.recipe_rescued_count,
                    recipe.rescuedIngredients.size,
                    recipe.rescuedIngredients.size,
                ),
            ),
        )
    }
}

/** A plate seen from above, with the ingredients on it; decorative, the list below names them. */
@Composable
private fun RecipePlate(emojis: List<String>) {
    val scheme = MaterialTheme.colorScheme
    val rim = scheme.surfaceContainerLowest.copy(alpha = 0.6f)
    val plate = scheme.surfaceContainerLowest
    val bed = scheme.tertiary.copy(alpha = 0.16f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(scheme.tertiaryContainer)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val r = size.minDimension / 2
            drawCircle(color = Color.Black.copy(alpha = 0.08f), radius = r * 0.98f, center = center.copy(y = center.y + r * 0.08f))
            drawCircle(color = rim, radius = r)
            drawCircle(color = plate, radius = r * 0.82f)
            drawCircle(color = bed, radius = r * 0.62f)
        }
        emojis.forEachIndexed { index, emoji ->
            val spot = PlateSpots[index]
            Text(
                text = emoji,
                fontSize = if (index == 0) 38.sp else 30.sp,
                modifier = Modifier.offset(x = spot.x, y = spot.y),
            )
        }
    }
}

/** Key facts as pills; the last one (items rescued) is the good news, so it's green. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowFacts(facts: List<Pair<String, String>>) {
    val fresh = MaterialTheme.freshnessPalette.fresh
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.forEachIndexed { index, (icon, label) ->
            val highlighted = index == facts.lastIndex
            Surface(
                shape = CircleShape,
                color = if (highlighted) fresh.container else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (highlighted) fresh.onContainer else MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = icon)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * The rescued ingredients as cards, each with the pantry's freshness ring, so the recipe shows
 * what it is saving and how close it was. An ingredient with no matching pantry item (renamed
 * or translated by the model) shows its emoji on a plain tile.
 */
@Composable
fun RescuedIngredients(
    ingredients: List<RecipeIngredient>,
    pantryItems: List<PantryItem>,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ingredients.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { ingredient ->
                    val item = pantryItems.firstOrNull { it.name.equals(ingredient.name, ignoreCase = true) }
                    ShelfieCard(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .clearAndSetSemantics { contentDescription = "${ingredient.name} ${ingredient.amount}" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (item != null) {
                                FreshnessRing(
                                    emoji = ingredientEmoji(ingredient, pantryItems),
                                    shelfLifeLeft = item.shelfLifeLeft(today),
                                    status = item.freshnessStatus(today),
                                    size = 54.dp,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(text = ingredientEmoji(ingredient, pantryItems), fontSize = 22.sp)
                                }
                            }
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (ingredient.amount.isNotEmpty()) {
                                Text(
                                    text = ingredient.amount,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                // Keep the last row's cards the same width as the others.
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** The basics as chips with their amount; they may need buying, hence the carrot dot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtraIngredientChips(ingredients: List<RecipeIngredient>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ingredients.forEach { ingredient ->
            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainerLowest,
                contentColor = scheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.extendedColors.glassBorder),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).background(scheme.tertiary, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = ingredient.name, style = MaterialTheme.typography.labelLarge)
                    if (ingredient.amount.isNotEmpty()) {
                        Text(
                            text = "  " + ingredient.amount,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientTile(
    emoji: String,
    name: String,
    amount: String,
    isRescued: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = if (isRescued) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$name $amount" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(container, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 34.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (amount.isNotEmpty()) {
            Text(
                text = amount,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One step of the overview list, with its number in a circle. */
@Composable
fun StepOverviewCard(number: Int, step: RecipeStep, modifier: Modifier = Modifier) {
    ShelfieCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.padding(16.dp)) {
            StepNumber(number = number)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = step.instruction, style = MaterialTheme.typography.bodyLarge)
                if (step.durationMinutes > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Waiting time in the cooking accent: cooking mode turns it into a timer.
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            text = "⏱ " + pluralStringResource(
                                R.plurals.recipe_step_wait,
                                step.durationMinutes,
                                step.durationMinutes,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepNumber(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    SectionHeader(title = text, modifier = modifier)
}

/**
 * Videos and recipe sites for the dish, as a YouTube and a web search on its title. Searches,
 * not links the model writes: a model asked for a URL often makes one up, a search always
 * leads to pages that exist.
 */
@Composable
fun RecipeLinksCard(title: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val query = Uri.encode(stringResource(R.string.recipe_search_query, title))
    ShelfieCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = stringResource(R.string.recipe_links_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.recipe_links_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { uriHandler.openUri("https://www.youtube.com/results?search_query=$query") },
                    modifier = Modifier.weight(1f),
                ) { Text("▶ " + stringResource(R.string.recipe_links_video), maxLines = 1) }
                FilledTonalButton(
                    onClick = { uriHandler.openUri("https://www.google.com/search?q=$query") },
                    modifier = Modifier.weight(1f),
                ) { Text("🔎 " + stringResource(R.string.recipe_links_web), maxLines = 1) }
            }
        }
    }
}
