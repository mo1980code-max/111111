package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.AndroidComposeTestRule

fun createComposeRule(): AndroidComposeTestRule<Any> = AndroidComposeTestRule()

fun <A : Any> createAndroidComposeRule(activityClass: Class<A>): AndroidComposeTestRule<A> = AndroidComposeTestRule()
