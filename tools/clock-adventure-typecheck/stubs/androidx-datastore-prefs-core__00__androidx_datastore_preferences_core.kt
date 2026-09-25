package androidx.datastore.preferences.core

import androidx.datastore.core.DataStore

interface Preferences {
    class Key<T>(val name: String)
    operator fun <T> get(key: Key<T>): T?
}

interface MutablePreferences : Preferences {
    operator fun <T> set(key: Preferences.Key<T>, value: T)
}

fun stringPreferencesKey(name: String): Preferences.Key<String> = TODO()
fun booleanPreferencesKey(name: String): Preferences.Key<Boolean> = TODO()
fun intPreferencesKey(name: String): Preferences.Key<Int> = TODO()
fun longPreferencesKey(name: String): Preferences.Key<Long> = TODO()
fun emptyPreferences(): Preferences = TODO()

suspend fun DataStore<Preferences>.edit(transform: suspend (MutablePreferences) -> Unit): Preferences = TODO()
