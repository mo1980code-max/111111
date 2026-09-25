package androidx.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement

interface LazyListScope {
    fun items(count: Int, key: ((index: Int) -> Any)? = null, itemContent: @Composable LazyItemScope.(index: Int) -> Unit)
    fun <T> items(items: List<T>, key: ((item: T) -> Any)? = null, contentType: ((item: T) -> Any?)? = null, itemContent: @Composable LazyItemScope.(item: T) -> Unit)
    fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit)
}

interface LazyItemScope

class LazyListState

@Composable
fun rememberLazyListState(initialFirstVisibleItemIndex: Int = 0, initialFirstVisibleItemScrollOffset: Int = 0): LazyListState = TODO()

@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Any = Arrangement.Top,
    horizontalAlignment: Any = androidx.compose.ui.Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) = Unit

@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    horizontalArrangement: Any = Arrangement.Start,
    verticalAlignment: Any = androidx.compose.ui.Alignment.Top,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) = Unit
