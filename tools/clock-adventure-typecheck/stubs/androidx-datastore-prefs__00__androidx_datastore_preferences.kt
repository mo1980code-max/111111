package androidx.datastore.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun preferencesDataStore(
    name: String
): kotlin.properties.ReadOnlyProperty<android.content.Context, DataStore<Preferences>> = TODO()
