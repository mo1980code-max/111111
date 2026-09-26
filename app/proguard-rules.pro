# R8 rules for the Dhikr app.
#
# The previous file contained `-keep class * { public private *; }`, which kept every member of
# every class and made minification pointless. Room, Hilt, Compose, Navigation and DataStore all
# ship their own consumer rules, so only the few app-specific entry points below are needed.

# Entities are reflected over by Room's generated code; their field names must survive.
-keep class com.clock.livewallpaper.data.local.DhikrEntity { *; }

# Broadcast receivers / widget providers are instantiated by name from the manifest.
-keep class com.clock.livewallpaper.reminder.** extends android.content.BroadcastReceiver
-keep class com.clock.livewallpaper.widget.** extends android.appwidget.AppWidgetProvider

# Keep annotation and signature metadata used by Hilt/Room generated code.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Line numbers make Play Console crash reports readable while still obfuscating names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
