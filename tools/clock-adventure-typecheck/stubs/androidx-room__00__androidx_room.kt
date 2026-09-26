package androidx.room

@Target(AnnotationTarget.CLASS)
annotation class Entity(val tableName: String = "")

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class PrimaryKey(val autoGenerate: Boolean = false)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class ColumnInfo(val name: String = "")

@Target(AnnotationTarget.CLASS)
annotation class Dao

@Target(AnnotationTarget.FUNCTION)
annotation class Query(val value: String)

object OnConflictStrategy {
    const val REPLACE = 1
    const val ABORT = 3
    const val IGNORE = 5
}

@Target(AnnotationTarget.FUNCTION)
annotation class Insert(val onConflict: Int = OnConflictStrategy.ABORT)

@Target(AnnotationTarget.FUNCTION)
annotation class Update(val onConflict: Int = OnConflictStrategy.ABORT)

@Target(AnnotationTarget.FUNCTION)
annotation class Upsert

@Target(AnnotationTarget.FUNCTION)
annotation class Delete

@Target(AnnotationTarget.FUNCTION)
annotation class Transaction

@Target(AnnotationTarget.CLASS)
annotation class Database(
    val entities: Array<kotlin.reflect.KClass<*>>,
    val version: Int,
    val exportSchema: Boolean = true
)

abstract class RoomDatabase

class Builder<T : RoomDatabase> {
    fun fallbackToDestructiveMigration(): Builder<T> = TODO()
    fun build(): T = TODO()
}

object Room {
    fun <T : RoomDatabase> databaseBuilder(context: Any?, klass: Class<T>, name: String): Builder<T> = TODO()
}
