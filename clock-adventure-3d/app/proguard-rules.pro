# Clock Adventure 3D - release shrinking rules.
#
# The heavy lifting (Compose, Room, Hilt, Kotlin coroutines) is done by the consumer rules that
# ship with those libraries. These rules only cover what the app itself declares.

# Keep the Room entities and the database class: the schema is addressed by name.
-keep class com.clockadventure.data.db.** { *; }

# Keep Kotlin metadata so that Room, Hilt and the Compose compiler can still read it.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class kotlin.Metadata { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.* <methods>;
}

# Hilt generated entry points and view model factories.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Domain models are also used reflectively by the achievement bookkeeping (enum valueOf).
-keep class com.clockadventure.domain.model.** { *; }
-keepclassmembers enum com.clockadventure.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.clockadventure.domain.catalog.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Useful diagnostics for a crash reported by a parent.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
