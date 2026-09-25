package androidx.navigation

import androidx.compose.runtime.Composable

open class NavController {
    open fun navigate(route: String) {}
    open fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {}
    open fun popBackStack(): Boolean = true
    open fun popBackStack(route: String, inclusive: Boolean): Boolean = true
}

class NavHostController : NavController()

class NavGraphBuilder {
    fun composable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable (NavBackStackEntry) -> Unit
    ) {
    }
}

class NavBackStackEntry {
    val arguments: NavArguments = NavArguments()
    fun <T> savedStateHandle(): SavedStateHandleStub = SavedStateHandleStub()
}

class SavedStateHandleStub {
    fun <T> get(key: String): T? = null
}

class NavArguments {
    fun getInt(key: String, default: Int = 0): Int = default
    fun getString(key: String): String? = null
    fun getBoolean(key: String, default: Boolean = false): Boolean = default
}

class NamedNavArgument(val name: String)

class NavOptionsBuilder {
    fun popUpTo(route: String, builder: PopUpToBuilder.() -> Unit = {}) {}
    var launchSingleTop: Boolean = false
}

class PopUpToBuilder {
    var inclusive: Boolean = false
}

object NavType {
    val IntType: Any = Any()
    val StringType: Any = Any()
    val BoolType: Any = Any()
}

fun navArgument(
    name: String,
    builder: NavArgumentBuilder.() -> Unit
): NamedNavArgument = NamedNavArgument(name)

class NavArgumentBuilder {
    var type: Any = NavType.StringType
    var defaultValue: Any? = null
    var nullable: Boolean = false
}
