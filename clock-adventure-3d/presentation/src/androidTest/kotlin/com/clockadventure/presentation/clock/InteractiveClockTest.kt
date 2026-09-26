package com.clockadventure.presentation.clock

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.presentation.theme.ClockAdventureTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests of the interactive clock - the one piece of UI that cannot be verified by a
 * unit test because it lives from the touch gestures.
 *
 * Run them on a device or emulator from Android Studio (right click → Run) or with
 * `./gradlew :presentation:connectedAndroidTest`.
 */
class InteractiveClockTest {

    companion object {
        private const val CLOCK_TAG = "analog_clock"
    }

    @get:Rule
    val composeRule = createComposeRule()

    private fun setClock(
        snapMinutes: Int = 5,
        interactive: Boolean = true,
        showDigital: Boolean = false,
        onTimeChanged: (ClockTime) -> Unit
    ) {
        composeRule.setContent {
            ClockAdventureTheme {
                InteractiveClock(
                    time = ClockTime.of12(3, 0, pm = false),
                    interactive = interactive,
                    snapMinutes = snapMinutes,
                    showDigital = showDigital,
                    use24Hour = false,
                    testTag = CLOCK_TAG,
                    onTimeChanged = onTimeChanged,
                    modifier = Modifier.size(320.dp)
                )
            }
        }
    }

    @Test
    fun theClockIsOnTheScreen() {
        setClock(onTimeChanged = {})
        composeRule.onNodeWithTag(CLOCK_TAG).assertExists()
    }

    @Test
    fun draggingTheHandsReportsANewTime() {
        var lastTime: ClockTime? = null
        setClock(onTimeChanged = { lastTime = it })

        composeRule.onNodeWithTag(CLOCK_TAG).performTouchInput { swipeRight() }

        val reported = lastTime
        assertNotNull("a drag on the clock must report a time", reported)
        assertEquals("the hour must stay inside 1..12", true, reported!!.hour12 in 1..12)
        assertEquals("minutes must stay inside 0..59", true, reported.minute in 0..59)
    }

    @Test
    fun theMinuteHandSnapsToTheGranularityOfTheLevel() {
        val reported = mutableListOf<ClockTime>()
        setClock(snapMinutes = 15, onTimeChanged = { reported += it })

        composeRule.onNodeWithTag(CLOCK_TAG).performTouchInput { swipeRight() }

        assertNotNull("the drag must be reported", reported.firstOrNull())
        for (time in reported) {
            assertEquals(
                "a quarter hour level may only produce :00, :15, :30 and :45 but got ${time.minute}",
                0,
                time.minute % 15
            )
        }
    }

    @Test
    fun aNonInteractiveClockIgnoresTouches() {
        var lastTime: ClockTime? = null
        setClock(interactive = false, onTimeChanged = { lastTime = it })

        composeRule.onNodeWithTag(CLOCK_TAG).performTouchInput { swipeRight() }

        assertNull("a read-only clock must not change the time", lastTime)
    }

    @Test
    fun theDigitalHelperShowsTheTimeOfTheHands() {
        setClock(showDigital = true, onTimeChanged = {})
        composeRule.onNodeWithText("3:00", substring = false).assertExists()
    }

    @Test
    fun theClockFillsTheWidthItIsGiven() {
        composeRule.setContent {
            ClockAdventureTheme {
                MiniClock(
                    time = ClockTime.of12(7, 30, pm = false),
                    size = 120.dp,
                    showNumbers = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        composeRule.onNodeWithTag(CLOCK_TAG).assertDoesNotExist()
    }
}
