package org.junit

/**
 * Minimal JUnit replacement used by tools/clock-adventure-typecheck/run_tests.py.
 *
 * Maven Central cannot be reached from this sandbox, so the real JUnit jar cannot be downloaded.
 * This stub provides the same five assertions the domain tests use - with real checking, so a
 * failing assertion really does fail the run - plus the `@Test` annotation, which the runner
 * discovers by reflection.
 */
annotation class Test

object Assert {

    fun fail(message: String = "assertion failed"): Nothing = throw AssertionError(message)

    fun assertTrue(condition: Boolean) {
        if (!condition) fail("expected true")
    }

    fun assertTrue(message: String, condition: Boolean) {
        if (!condition) fail(message)
    }

    fun assertFalse(condition: Boolean) {
        if (condition) fail("expected false")
    }

    fun assertFalse(message: String, condition: Boolean) {
        if (condition) fail(message)
    }

    fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) fail("expected <$expected> but was <$actual>")
    }

    fun assertEquals(message: String, expected: Any?, actual: Any?) {
        if (expected != actual) fail("$message: expected <$expected> but was <$actual>")
    }

    fun assertEquals(expected: Long, actual: Long) {
        if (expected != actual) fail("expected <$expected> but was <$actual>")
    }

    fun assertEquals(expected: Int, actual: Int) {
        if (expected != actual) fail("expected <$expected> but was <$actual>")
    }

    fun assertEquals(expected: Float, actual: Float, delta: Float = 0.0001f) {
        if (kotlin.math.abs(expected - actual) > delta) fail("expected <$expected> but was <$actual>")
    }

    fun assertNotEquals(unexpected: Any?, actual: Any?) {
        if (unexpected == actual) fail("expected a value different from <$unexpected>")
    }

    fun assertNull(actual: Any?) {
        if (actual != null) fail("expected null but was <$actual>")
    }

    fun assertNotNull(actual: Any?) {
        if (actual == null) fail("expected a value but was null")
    }
}
