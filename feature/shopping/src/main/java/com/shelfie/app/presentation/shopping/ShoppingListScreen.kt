package com.shelfie.app.presentation.shopping

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.FoodCategories
import com.shelfie.app.domain.model.ShoppingItem
import com.shelfie.app.presentation.common.ScreenHeader
import com.shelfie.app.presentation.common.MemberAvatar
import com.shelfie.app.presentation.common.ShelfieCard
import com.shelfie.app.presentation.common.displayName
import com.shelfie.app.presentation.common.emoji
import com.shelfie.app.presentation.common.foodEmoji
import com.shelfie.app.presentation.common.glow
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.mascot.Frizzy
import com.shelfie.app.presentation.mascot.FrizzyMood
import com.shelfie.app.presentation.theme.extendedColors
import kotlinx.coroutines.launch

@Composable
fun ShoppingListRoute(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ShoppingEffect.Bought -> launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.shopping_bought_one, effect.item.name),
                        actionLabel = resources.getString(R.string.shopping_action_to_pantry),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.stockUp(effect.item)
                }
                is ShoppingEffect.StockedUp -> launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        resources.getQuantityString(R.plurals.shopping_stocked_up, effect.count, effect.count),
                    )
                }
            }
        }
    }
    ShoppingListScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onStockUpBought = viewModel::stockUpBought,
        onAdd = { viewModel.add(it) },
        onCheckedChange = viewModel::setChecked,
        onDelete = viewModel::delete,
        onClearBought = viewModel::clearBought,
    )
}

/** One rounded card per aisle: rows join into it, only the first and last round their corners. */
private val GroupRadius = 22.dp

@Composable
fun ShoppingListScreen(
    state: ShoppingListUiState,
    onAdd: (String) -> Unit,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onClearBought: () -> Unit,
    onStockUpBought: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var newItem by rememberSaveable { mutableStateOf("") }
    fun submit() {
        onAdd(newItem)
        newItem = ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Above the dock's raised scan button, which reaches ~30 dp into the screen.
            if (state.bought.isNotEmpty()) {
                StockUpBar(
                    count = state.bought.size,
                    onStockUp = onStockUpBought,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 48.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.shopping_title),
                    actions = {
                        if (state.toBuy.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Text(
                                    text = pluralStringResource(R.plurals.shopping_to_buy, state.toBuy.size, state.toBuy.size),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    },
                )
            }
            item(key = "input") {
                AddField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    onSubmit = ::submit,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestionsTitle") { GroupTitle(stringResource(R.string.shopping_suggestions)) }
                item(key = "suggestions") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.suggestions, key = { it }) { name ->
                            SuggestionPill(name = name, onClick = { onAdd(name) })
                        }
                    }
                }
            }

            if (state.toBuy.isEmpty()) {
                item(key = "empty") {
                    ShelfieCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = null,
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Frizzy(mood = FrizzyMood.SLEEPY, size = 64.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.shopping_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            // One card per aisle, in walking order through the shop.
            state.aisles.forEach { aisle ->
                item(key = "aisle_${aisle.category}") {
                    GroupTitle("${aisle.category.emoji}  ${stringResource(aisle.category.labelRes).uppercase()}")
                }
                itemsInGroup(aisle.items, category = { aisle.category }, state.myEmail, onCheckedChange, onDelete)
            }

            if (state.bought.isNotEmpty()) {
                item(key = "boughtTitle") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GroupTitle(stringResource(R.string.shopping_bought).uppercase(), modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearBought, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(R.string.shopping_clear_bought))
                        }
                    }
                }
                itemsInGroup(
                    state.bought,
                    category = { FoodCategories.guess(it.name) ?: Category.OTHER },
                    state.myEmail,
                    onCheckedChange,
                    onDelete,
                )
            }
        }
    }
}

/** The rows of one group, each shaped as its slice of a single rounded card. */
private fun LazyListScope.itemsInGroup(
    items: List<ShoppingItem>,
    category: (ShoppingItem) -> Category,
    myEmail: String?,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
) {
    items.forEachIndexed { index, entry ->
        item(key = entry.id) {
            ShoppingRow(
                item = entry,
                emoji = foodEmoji(entry.name, category(entry)),
                // Your own items need no face; only what someone else in the household added.
                addedByOther = entry.addedBy?.takeUnless { myEmail != null && it.equals(myEmail, ignoreCase = true) },
                isFirst = index == 0,
                isLast = index == items.lastIndex,
                onCheckedChange = onCheckedChange,
                onDelete = onDelete,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** A pill-shaped composer, like a chat input: type, then tap + or Done. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddField(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.extendedColors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.shopping_add_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, extended.glassBorder, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSubmit,
            enabled = value.isNotBlank(),
            modifier = Modifier
                .size(56.dp)
                .then(if (value.isNotBlank()) Modifier.glow(extended.glow, CircleShape, radius = 10.dp) else Modifier),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.shopping_add))
        }
    }
}

/** "Recently ran out": one tap puts it back on the list. */
@Composable
private fun SuggestionPill(name: String, onClick: () -> Unit) {
    val extended = MaterialTheme.extendedColors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, extended.glassBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = foodEmoji(name, FoodCategories.guess(name) ?: Category.OTHER), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    emoji: String,
    addedByOther: String?,
    isFirst: Boolean,
    isLast: Boolean,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val top = if (isFirst) GroupRadius else 0.dp
    val bottom = if (isLast) GroupRadius else 0.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    val checkedLabel = stringResource(R.string.shopping_bought)

    Surface(
        onClick = { onCheckedChange(item, !item.isChecked) },
        shape = shape,
        color = if (item.isChecked) scheme.surfaceContainerLow else scheme.surfaceContainerLowest,
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(item.isChecked)
                if (item.isChecked) stateDescription = checkedLabel
            },
    ) {
        Column {
            if (!isFirst) HorizontalDivider(color = extended.glassBorder, modifier = Modifier.padding(start = 56.dp))
            Row(
                modifier = Modifier
                    .heightIn(min = 60.dp)
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The row is the toggle; this is only its picture, sized like a 44 dp target.
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    RoundCheck(checked = item.isChecked)
                }
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.alpha(if (item.isChecked) 0.5f else 1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).alpha(if (item.isChecked) 0.6f else 1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.amount.isNotEmpty()) {
                        Text(
                            text = item.amount,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (item.fromRecipe) {
                    RecipeTag(modifier = Modifier.padding(start = 8.dp))
                }
                if (addedByOther != null) {
                    AddedByAvatar(email = addedByOther, modifier = Modifier.padding(start = 8.dp))
                }
                IconButton(onClick = { onDelete(item) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete),
                        tint = scheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** "Recipe": the item is a missing ingredient a recipe or the meal plan asked for. */
@Composable
private fun RecipeTag(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.tertiaryContainer,
        contentColor = scheme.onTertiaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.shopping_tag_recipe),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Who added the item, in their household colour; read out as "Added by …". */
@Composable
private fun AddedByAvatar(email: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.shopping_added_by, displayName(email))
    MemberAvatar(
        email = email,
        size = 26.dp,
        modifier = modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun RoundCheck(checked: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (checked) {
                    Modifier.background(scheme.primary, CircleShape)
                } else {
                    Modifier.border(2.dp, scheme.outline, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Floating bar once something is ticked off: everything bought goes into the pantry in one tap. */
@Composable
private fun StockUpBar(count: Int, onStockUp: () -> Unit, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.extendedColors
    val shape = RoundedCornerShape(30.dp)
    Surface(
        shape = shape,
        color = extended.onHero,
        contentColor = extended.hero,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(extended.hero.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = count.toString(), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = pluralStringResource(R.plurals.shopping_bought_ready, count, count),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onStockUp,
                shape = CircleShape,
                color = extended.hero,
                contentColor = extended.onHero,
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.shopping_action_to_pantry), style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(4.dp))
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
