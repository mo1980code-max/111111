package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.NamedNavArgument

@Composable
fun NavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit
) = Unit

@Composable
fun rememberNavController(): NavHostController = NavHostController()

fun NavGraphBuilder.composableCompat(
    route: String,
    content: @Composable () -> Unit
) = Unit

fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) = Unit

fun NavHostController.navigateCompat(route: String) {}
