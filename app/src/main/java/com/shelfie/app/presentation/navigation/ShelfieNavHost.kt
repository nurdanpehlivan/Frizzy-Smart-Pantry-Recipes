package com.shelfie.app.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.common.glow
import com.shelfie.app.presentation.dashboard.DashboardRoute
import com.shelfie.app.presentation.history.RecipeHistoryRoute
import com.shelfie.app.presentation.mealplan.MealPlanRoute
import com.shelfie.app.presentation.premium.PremiumRoute
import com.shelfie.app.presentation.recipe.RecipeDestination
import com.shelfie.app.presentation.recipe.RecipeRoute
import com.shelfie.app.presentation.recipe.picker.IngredientPickerRoute
import com.shelfie.app.presentation.scanner.ScannerRoute
import com.shelfie.app.presentation.settings.AccountRoute
import com.shelfie.app.presentation.settings.NotificationsRoute
import com.shelfie.app.presentation.settings.SettingsRoute
import com.shelfie.app.presentation.shopping.ShoppingListRoute
import com.shelfie.app.presentation.statistics.StatisticsRoute
import com.shelfie.app.presentation.theme.OnScanGradient
import com.shelfie.app.presentation.theme.ScanGradient
import com.shelfie.app.presentation.theme.extendedColors
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object DashboardDestination

@Serializable
data object ScannerDestination

@Serializable
data object RecipeHistoryDestination

@Serializable
data object IngredientPickerDestination

@Serializable
data object MealPlanDestination

@Serializable
data object PremiumDestination

@Serializable
data object ShoppingListDestination

@Serializable
data object StatisticsDestination

@Serializable
data object SettingsDestination

/** The household's activity feed: who added, used or threw away what. */
@Serializable
data object NotificationsDestination

/**
 * Account, family sharing and its activity. [joinCode]: a family invite code from an invite
 * link, filled into "join" on arrival.
 */
@Serializable
data class AccountDestination(val joinCode: String = "")

/** Opens the account and family screen, e.g. from a household activity notification. */
const val ACCOUNT_DEEP_LINK = "shelfie://account"

/** Opens the household's activity feed, e.g. from a household activity notification. */
const val NOTIFICATIONS_DEEP_LINK = "shelfie://notifications"

/** Opens the rescue recipe, e.g. from the daily freshness notification. */
const val RECIPE_DEEP_LINK = "shelfie://recipe"

/** Opens family sharing with `?joinCode=`; the backend's /join/<code> page links here. */
const val JOIN_DEEP_LINK = "shelfie://join"

/** Deep link to one recipe of the history. */
fun savedRecipeDeepLink(savedRecipeId: Long): String = "$RECIPE_DEEP_LINK?savedRecipeId=$savedRecipeId"

/** Result key the scanner uses to tell the dashboard how many items it saved. */
private const val KEY_SAVED_ITEMS_COUNT = "saved_items_count"

/** [iconRes] is the outlined icon of an idle tab, [selectedIconRes] the filled one. */
private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    // The scan button sits between RECIPES and SHOPPING, see [ShelfieNavigationBar]. Statistics
    // is no tab: the streak badge on the pantry opens it.
    PANTRY(DashboardDestination, DashboardDestination::class, R.drawable.ic_home_outlined, R.drawable.ic_home, R.string.tab_pantry),
    RECIPES(RecipeHistoryDestination, RecipeHistoryDestination::class, R.drawable.ic_restaurant, R.drawable.ic_restaurant, R.string.tab_recipes),
    SHOPPING(ShoppingListDestination, ShoppingListDestination::class, R.drawable.ic_shopping_cart_outlined, R.drawable.ic_shopping_cart, R.string.tab_shopping),
    FAMILY(AccountDestination(), AccountDestination::class, R.drawable.ic_group, R.drawable.ic_group, R.string.tab_family),
}

@Composable
fun ShelfieNavHost(
    navController: NavHostController = rememberNavController(),
    inviteCode: String? = null,
    onInviteCodeHandled: () -> Unit = {},
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val unseenActivity by shellViewModel.unseenActivity.collectAsStateWithLifecycle()
    // Installed from a family invite: open "join" with the code filled in, once.
    LaunchedEffect(inviteCode) {
        val code = inviteCode ?: return@LaunchedEffect
        navController.navigate(AccountDestination(joinCode = code)) { launchSingleTop = true }
        onInviteCodeHandled()
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelTab.entries.any { currentDestination.isOn(it) }

    fun navigateToTab(tab: TopLevelTab) {
        navController.navigate(tab.route) {
            // Standard tab behaviour: one copy of each tab, state kept.
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Scanning saves into the pantry and reports back to it, so it always starts from there.
    fun navigateToScanner() {
        navigateToTab(TopLevelTab.PANTRY)
        navController.navigate(ScannerDestination) { launchSingleTop = true }
    }

    fun navigateToRecipe(savedRecipeId: Long = RecipeDestination.NO_SAVED_RECIPE) {
        navController.navigate(RecipeDestination(savedRecipeId)) { launchSingleTop = true }
    }

    Scaffold(
        // Each screen has its own Scaffold that handles the status bar; this one only adds
        // the bottom bar, and the padding below tells the inner ones that it took the bottom.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                ShelfieNavigationBar(
                    isSelected = { tab -> currentDestination.isOn(tab) },
                    onTabClick = { tab -> navigateToTab(tab) },
                    onScan = { navigateToScanner() },
                    familyBadge = if (currentDestination.isOn(TopLevelTab.FAMILY)) 0 else unseenActivity,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<DashboardDestination> { entry ->
                val savedItemsCount by entry.savedStateHandle
                    .getStateFlow<Int?>(KEY_SAVED_ITEMS_COUNT, null)
                    .collectAsStateWithLifecycle()

                DashboardRoute(
                    onNavigateToScanner = { navigateToScanner() },
                    onNavigateToRecipe = { navigateToRecipe() },
                    onNavigateToSettings = {
                        navController.navigate(SettingsDestination) { launchSingleTop = true }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(NotificationsDestination) { launchSingleTop = true }
                    },
                    onNavigateToStatistics = {
                        navController.navigate(StatisticsDestination) { launchSingleTop = true }
                    },
                    savedItemsCount = savedItemsCount,
                    onSavedItemsMessageShown = {
                        entry.savedStateHandle[KEY_SAVED_ITEMS_COUNT] = null
                    },
                )
            }
            composable<RecipeDestination>(
                deepLinks = listOf(navDeepLink<RecipeDestination>(basePath = RECIPE_DEEP_LINK)),
            ) {
                RecipeRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                )
            }
            composable<RecipeHistoryDestination> {
                RecipeHistoryRoute(
                    onOpenRecipe = { id -> navigateToRecipe(id) },
                    onNewRecipe = { navigateToRecipe() },
                    onCookWithWhatIHave = {
                        navController.navigate(IngredientPickerDestination) { launchSingleTop = true }
                    },
                    onOpenMealPlan = {
                        navController.navigate(MealPlanDestination) { launchSingleTop = true }
                    },
                )
            }
            composable<MealPlanDestination> {
                MealPlanRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenRecipe = { id -> navigateToRecipe(id) },
                )
            }
            composable<IngredientPickerDestination> {
                IngredientPickerRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onCreateRecipe = { ids ->
                        // The picker has done its job; back from the recipe returns to the history.
                        navController.navigate(RecipeDestination(ingredientIds = ids.joinToString(","))) {
                            popUpTo(IngredientPickerDestination) { inclusive = true }
                        }
                    },
                )
            }
            composable<ShoppingListDestination> {
                ShoppingListRoute()
            }
            composable<StatisticsDestination> {
                StatisticsRoute(onNavigateUp = { navController.navigateUp() })
            }
            composable<SettingsDestination> {
                SettingsRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                    onOpenAccount = { navController.navigate(AccountDestination()) { launchSingleTop = true } },
                )
            }
            composable<AccountDestination>(
                deepLinks = listOf(
                    navDeepLink<AccountDestination>(basePath = JOIN_DEEP_LINK),
                    navDeepLink<AccountDestination>(basePath = ACCOUNT_DEEP_LINK),
                ),
            ) {
                // A tab of its own; the back arrow only when it was opened from settings or Premium.
                val openedFromScreen = navController.previousBackStackEntry?.destination?.let {
                    it.hasRoute(SettingsDestination::class) || it.hasRoute(PremiumDestination::class)
                } == true
                AccountRoute(
                    onNavigateUp = if (openedFromScreen) ({ navController.navigateUp() }) else null,
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                    onOpenNotifications = { navController.navigate(NotificationsDestination) { launchSingleTop = true } },
                    onOpenStatistics = { navController.navigate(StatisticsDestination) { launchSingleTop = true } },
                )
            }
            composable<NotificationsDestination>(
                deepLinks = listOf(navDeepLink<NotificationsDestination>(basePath = NOTIFICATIONS_DEEP_LINK)),
            ) {
                NotificationsRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenFamily = {
                        navController.popBackStack()
                        navigateToTab(TopLevelTab.FAMILY)
                    },
                )
            }
            composable<PremiumDestination> {
                PremiumRoute(
                    onNavigateUp = { navController.navigateUp() },
                    // Premium needs an account; this is where one is made.
                    onOpenSettings = {
                        navController.navigate(AccountDestination()) {
                            popUpTo(PremiumDestination) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<ScannerDestination> {
                ScannerRoute(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenPremium = { navController.navigate(PremiumDestination) { launchSingleTop = true } },
                    onItemsSaved = { count ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(KEY_SAVED_ITEMS_COUNT, count)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

/**
 * Floating glass dock: inset from the screen edges, rounded, with a soft shadow. Two tabs on
 * each side of a raised gradient scan button, the app's most frequent action, right under the
 * thumb. The active tab gets a filled icon on a faint leaf pill; idle tabs are outlined and muted.
 */
@Composable
private fun ShelfieNavigationBar(
    isSelected: (TopLevelTab) -> Boolean,
    onTabClick: (TopLevelTab) -> Unit,
    onScan: () -> Unit,
    familyBadge: Int,
) {
    val extended = MaterialTheme.extendedColors
    val shape = RoundedCornerShape(32.dp)
    val (left, right) = TopLevelTab.entries.let { it.take(2) to it.drop(2) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 20.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(extended.navBar)
                .border(1.dp, extended.glassBorder, shape)
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .selectableGroup(),
        ) {
            left.forEach { tab -> NavItem(tab = tab, selected = isSelected(tab), badge = 0, onClick = { onTabClick(tab) }) }
            // Room for the raised scan button drawn on top.
            Spacer(modifier = Modifier.weight(1f))
            right.forEach { tab ->
                NavItem(
                    tab = tab,
                    selected = isSelected(tab),
                    badge = if (tab == TopLevelTab.FAMILY) familyBadge else 0,
                    onClick = { onTabClick(tab) },
                )
            }
        }
        Surface(
            onClick = onScan,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = OnScanGradient,
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.background),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .size(64.dp)
                .glow(extended.glow, CircleShape, radius = 12.dp)
                .background(ScanGradient, CircleShape),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.dashboard_action_scan),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: TopLevelTab, selected: Boolean, badge: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val iconColor by animateColorAsState(if (selected) scheme.primary else scheme.onSurfaceVariant, label = "navIcon")
    val pillColor by animateColorAsState(
        if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        label = "navPill",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(pillColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(
                badge = {
                    if (badge > 0) Badge { Text(if (badge > MAX_NAV_BADGE) "$MAX_NAV_BADGE+" else "$badge") }
                },
            ) {
                Icon(
                    painter = painterResource(if (selected) tab.selectedIconRes else tab.iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun NavDestination?.isOn(tab: TopLevelTab): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

private const val MAX_NAV_BADGE = 9
