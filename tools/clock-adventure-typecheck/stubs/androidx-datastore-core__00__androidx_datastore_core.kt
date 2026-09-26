package androidx.datastore.core

interface DataStore<out T> {
    val data: kotlinx.coroutines.flow.Flow<T>
}
