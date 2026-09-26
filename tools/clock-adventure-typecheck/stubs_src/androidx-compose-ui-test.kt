package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

class SemanticsNodeInteraction {
    constructor()
    fun assertExists(): SemanticsNodeInteraction = this
    fun assertDoesNotExist(): SemanticsNodeInteraction = this
    fun assertIsDisplayed(): SemanticsNodeInteraction = this
    fun assertTextContains(value: String, substring: Boolean = false): SemanticsNodeInteraction = this
    fun performClick(): SemanticsNodeInteraction = this
    fun performTouchInput(block: TouchInjectionScope.() -> Unit): SemanticsNodeInteraction = this
    fun assertContentDescriptionContains(value: String): SemanticsNodeInteraction = this
}

class SemanticsNodeInteractionCollection {
    fun onFirst(): SemanticsNodeInteraction = SemanticsNodeInteraction()
    fun assertCountEquals(count: Int): SemanticsNodeInteractionCollection = this
}

interface InjectionScope {
    val visibleSize: androidx.compose.ui.unit.IntSize
    fun percentOffset(x: Float = 0f, y: Float = 0f): Offset = Offset.Zero
}

interface TouchInjectionScope : InjectionScope {
    fun down(position: Offset)
    fun move(): Unit
    fun moveTo(position: Offset)
    fun moveBy(delta: Offset)
    fun up(end: Int? = null)
    fun cancel()
    fun click(position: Offset? = null)
    fun swipe(start: Offset, end: Offset, durationMillis: Long = 200L)
    fun swipeLeft(startX: Float = 0f, endX: Float = 0f, durationMillis: Long = 200L)
    fun swipeRight(startX: Float = 0f, endX: Float = 0f, durationMillis: Long = 200L)
    fun swipeUp(startY: Float = 0f, endY: Float = 0f, durationMillis: Long = 200L)
    fun swipeDown(startY: Float = 0f, endY: Float = 0f, durationMillis: Long = 200L)
}

fun TouchInjectionScope.swipeRightCompat() = swipeRight()

class AndroidComposeTestRule<A : Any> {
    fun setContent(content: @Composable () -> Unit) {}
    fun onNodeWithTag(tag: String, useUnmergedTree: Boolean = false): SemanticsNodeInteraction = SemanticsNodeInteraction()
    fun onNodeWithText(text: String, substring: Boolean = false, ignoreCase: Boolean = false, useUnmergedTree: Boolean = false): SemanticsNodeInteraction = SemanticsNodeInteraction()
    fun onAllNodesWithTag(tag: String): SemanticsNodeInteractionCollection = SemanticsNodeInteractionCollection()
    fun waitForIdle() {}
}

package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.AndroidComposeTestRule

fun createComposeRule(): AndroidComposeTestRule<Any> = AndroidComposeTestRule()

fun <A : Any> createAndroidComposeRule(activityClass: Class<A>): AndroidComposeTestRule<A> = AndroidComposeTestRule()
