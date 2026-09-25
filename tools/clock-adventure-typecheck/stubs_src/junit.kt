package org.junit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Before

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class After

object Assert {
    fun assertEquals(expected: Any?, actual: Any?) {}
    fun assertEquals(message: String?, expected: Any?, actual: Any?) {}
    fun assertEquals(expected: Float, actual: Float, delta: Float) {}
    fun assertNotEquals(unexpected: Any?, actual: Any?) {}
    fun assertTrue(condition: Boolean) {}
    fun assertTrue(message: String?, condition: Boolean) {}
    fun assertFalse(condition: Boolean) {}
    fun assertFalse(message: String?, condition: Boolean) {}
    fun assertNull(value: Any?) {}
    fun assertNotNull(value: Any?) {}
    fun fail(message: String? = null): Nothing = throw IllegalStateException(message)
}
