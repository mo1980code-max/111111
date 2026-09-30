# Smart Clean AI - ProGuard / R8 Rules
-keep class com.smartclean.app.data.scanner.** { *; }
-keep class com.smartclean.app.domain.model.** { *; }
-keepattributes *Annotation*
-dontwarn javax.annotation.**
