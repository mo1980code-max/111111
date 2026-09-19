# Complete offline Quran integration — Java 17 / Android SDK 36

This document contains the full implementation inline, so no source-file links are needed. It is a snapshot of the files in this project. The source files remain the authoritative implementation.

## Prerequisites

- JDK 17 for both the Gradle runtime and compiler.
- Android Gradle Plugin 8.13.2, Gradle 8.13, compileSdk/targetSdk 36, minSdk 23.
- Supply the verified database at `app/src/main/assets/databases/quran.ar.uthmani.db`.
- Supply the compatible Uthmani font at `app/src/main/assets/fonts/quran_font.ttf`.
- Neither binary is currently supplied. The reader makes no runtime network requests.
- All supporting Java and resource files below are required. For another app, adapt package names and the R import, and merge—not replace—existing application initialization and manifest configuration.

## Manifest registration

Inside your existing `<application>` element:

```xml
<activity
    android:name="com.clock.livewallpaper.activity.QuranActivity"
    android:exported="false"
    android:label="@string/quran_title"
    android:theme="@style/QuranTheme" />
```

The existing manifest already registers `com.clock.livewallpaper.AppClass` as its Application class. Preserve that registration for the startup preparation hook below.

## Open a Surah

From an existing Activity:

```java
import android.content.Intent;
import com.clock.livewallpaper.activity.QuranActivity;

// Inside your click handler:
Intent intent = new Intent(this, QuranActivity.class);
intent.putExtra(QuranActivity.EXTRA_SURAH, 2);
intent.putExtra(QuranActivity.EXTRA_AYAH, 255); // Optional; defaults to 1.
startActivity(intent);
```

Page and Juz labels describe the selected ayah. This is a whole-Surah, reflowable text reader, not a fixed-line printed-page renderer.

## App-level Gradle configuration

File: `app/build.gradle`

```groovy
apply plugin: 'com.android.application'

android {
    compileSdk 36
    namespace 'com.clock.livewallpaper'
    defaultConfig {
        applicationId "com.clock.livewallpaper"
        minSdk 23
        targetSdk 36
        versionCode 1
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        resConfigs "en"
        multiDexEnabled true
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    buildFeatures {
        dataBinding true
    }
    bundle {
        density {
            enableSplit true
        }
        abi {
            enableSplit true
        }
        language {
            enableSplit false
        }
    }
    buildTypes {
        release {
            debuggable false
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }

        debug {
            debuggable true
            minifyEnabled false
            shrinkResources false
        }
    }

}
// Selects the compiler JDK; also run Gradle itself with JDK 17 (JAVA_HOME/Android Studio).
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {

    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation 'androidx.appcompat:appcompat:1.3.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.0'
    implementation 'com.github.bumptech.glide:glide:4.9.0'
    implementation 'com.android.volley:volley:1.1.1'
    implementation 'com.github.QuadFlask:colorpicker:0.0.14'
    implementation 'com.github.ybq:Android-SpinKit:1.2.0'
    implementation 'com.google.code.gson:gson:2.8.9'
    implementation 'com.isseiaoki:simplecropview:1.1.8'
    implementation 'com.liulishuo.okdownload:okdownload:1.0.7'
    implementation 'com.liulishuo.filedownloader:library:1.7.7'
    implementation 'com.ogaclejapan.smarttablayout:utils-v4:2.0.0'
    implementation 'com.squareup.picasso:picasso:2.8'
    implementation 'com.squareup.okhttp3:okhttp:4.9.3'
    implementation 'com.squareup.okio:okio:2.10.0'
    implementation 'cn.dreamtobe.filedownloader:filedownloader-okhttp3-connection:1.1.0'
    implementation 'androidx.recyclerview:recyclerview:1.1.0'
    implementation 'com.google.android.material:material:1.0.0'
    implementation 'com.google.android.gms:play-services-ads:22.6.0'

}
```

## Project-level Gradle configuration

File: `build.gradle`

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.13.2'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        // Legacy Clock/Wallpaper dependencies may still require this repository.
        // Prefer Maven Central; migrate these dependencies before removing the fallback.
        maven { url 'https://jcenter.bintray.com' }
    }
}

tasks.register('clean', Delete) {
    delete rootProject.layout.buildDirectory
}
```

## Gradle wrapper configuration

File: `gradle/wrapper/gradle-wrapper.properties`

```properties
#Fri Oct 11 09:42:38 IST 2019
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
```

## Project Gradle properties

File: `gradle.properties`

```properties
# Project-wide Gradle settings.

# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.

# For more details on how to configure your build environment visit
# http:

# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
android.enableJetifier=true
android.useAndroidX=true
org.gradle.jvmargs=-Xmx1536m

# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. More details, visit
# http:
# org.gradle.parallel=true

# AGP 8 defaults to non-final R IDs; legacy EditorActivity uses switch (R.id...).
android.nonFinalResIds=false
```

## Database helper

File: `app/src/main/java/com/clock/livewallpaper/quran/QuranDatabaseHelper.java`

```java
package com.clock.livewallpaper.quran;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs and reads the bundled Quran database. Call getVersesBySurah off the UI thread.
 * Deliberately not a SQLiteOpenHelper: this prebuilt, read-only database must not be
 * replaced with an empty onCreate database or have its upstream user_version changed.
 */
public final class QuranDatabaseHelper {
    public static final String DATABASE_NAME = "quran.ar.uthmani.db";
    private static final String ASSET_PATH = "databases/" + DATABASE_NAME;
    private static final Object COPY_LOCK = new Object();
    private final Context context;

    public QuranDatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Installs/validates the bundled database. Invoke on a worker thread at app startup. */
    public void prepareDatabase() throws IOException {
        installIfNeeded();
    }

    public List<Ayah> getVersesBySurah(int surah) throws IOException {
        if (surah < 1 || surah > 114) {
            throw new IllegalArgumentException("Surah must be between 1 and 114.");
        }
        File file = installIfNeeded();
        List<Ayah> verses = new ArrayList<>();
        // Both handles are closed before returning; no Cursor escapes to the Activity.
        try (SQLiteDatabase database = openReadOnly(file);
             Cursor cursor = database.query("arabic_text",
                     new String[]{"sura", "ayah", "text"},
                     "sura = ?", new String[]{String.valueOf(surah)},
                     null, null, "ayah ASC")) {
            int suraColumn = cursor.getColumnIndexOrThrow("sura");
            int ayahColumn = cursor.getColumnIndexOrThrow("ayah");
            int textColumn = cursor.getColumnIndexOrThrow("text");
            while (cursor.moveToNext()) {
                int number = cursor.getInt(ayahColumn);
                String text = cursor.getString(textColumn);
                if (number != verses.size() + 1 || text == null || text.trim().isEmpty()) {
                    throw new IOException("Missing, duplicate or empty ayah in Surah " + surah);
                }
                verses.add(new Ayah(cursor.getInt(suraColumn), number, text));
            }
        }
        if (verses.size() != QuranMetadata.ayahCount(surah)) {
            throw new IOException("Incomplete Surah " + surah + " in " + DATABASE_NAME);
        }
        return verses;
    }

    private File installIfNeeded() throws IOException {
        // Serializes first-use installation across helper instances in this app process.
        synchronized (COPY_LOCK) {
            File destination = context.getDatabasePath(DATABASE_NAME);
            if (destination.isFile() && destination.length() > 0) {
                try {
                    validateDatabase(destination);
                    return destination;
                } catch (SQLiteException | IOException invalidCopy) {
                    // Recover an old corrupt/partial copy from the packaged asset.
                }
            }
            File directory = destination.getParentFile();
            if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
                throw new IOException("Cannot create the private database directory.");
            }
            // Never copy directly to the final name: a killed first run must be retryable.
            File temporary = File.createTempFile("quran-install-", ".db", directory);
            try {
                try (InputStream input = context.getAssets().open(ASSET_PATH);
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                    output.flush();
                    output.getFD().sync();
                }
                validateDatabase(temporary);
                // Rename within the same filesystem only after a complete, validated copy.
                if (destination.exists() && !destination.delete()) {
                    throw new IOException("Cannot replace the invalid Quran database.");
                }
                if (!temporary.renameTo(destination)) {
                    throw new IOException("Cannot install the Quran database.");
                }
                return destination;
            } finally {
                if (temporary.exists()) {
                    // Best-effort cleanup after an unsuccessful install.
                    temporary.delete();
                }
            }
        }
    }

    private static SQLiteDatabase openReadOnly(File file) {
        return SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    private static void validateDatabase(File file) throws IOException {
        try (SQLiteDatabase database = openReadOnly(file)) {
            try (Cursor check = database.rawQuery("PRAGMA quick_check(1)", null)) {
                if (!check.moveToFirst() || !"ok".equalsIgnoreCase(check.getString(0))) {
                    throw new IOException("The Quran database failed its integrity check.");
                }
            }
            // Explicit projection checks the actual table and all required column names.
            try (Cursor schema = database.rawQuery(
                    "SELECT sura, ayah, text FROM arabic_text LIMIT 1", null)) {
                if (!schema.moveToFirst()) {
                    throw new IOException("The arabic_text table is empty.");
                }
            }
        }
    }

    public static final class Ayah {
        public final int surah;
        public final int number;
        public final String text;

        public Ayah(int surah, int number, String text) {
            this.surah = surah;
            this.number = number;
            this.text = text;
        }
    }
}
```

## Quran activity

File: `app/src/main/java/com/clock/livewallpaper/activity/QuranActivity.java`

```java
package com.clock.livewallpaper.activity;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ScrollView;
import android.widget.TextView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.QuranDatabaseHelper.Ayah;
import com.clock.livewallpaper.quran.QuranMetadata;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native, reflowable offline Surah reader. Page/Juz labels refer to the selected ayah. */
public final class QuranActivity extends Activity {
    public static final String EXTRA_SURAH = "quran.surah";
    public static final String EXTRA_AYAH = "quran.ayah";
    private static final String STATE_AYAH = "selected_ayah";
    private static final String STATE_SCROLL = "scroll_y";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean destroyed;
    private int surah;
    private int selectedAyah;
    private TextView versesView;
    private TextView juzView;
    private TextView pageView;
    private TextView statusView;
    private ScrollView scrollView;
    private View progress;
    private Spannable renderedText;
    private BackgroundColorSpan selection;
    private int[] verseStarts;
    private int[] verseEnds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quran);
        configureSystemBars();
        versesView = findViewById(R.id.quran_verses);
        juzView = findViewById(R.id.quran_juz);
        pageView = findViewById(R.id.quran_page);
        statusView = findViewById(R.id.quran_status);
        scrollView = findViewById(R.id.quran_scroll);
        progress = findViewById(R.id.quran_progress);

        surah = getIntent().getIntExtra(EXTRA_SURAH, 1);
        if (surah < 1 || surah > 114) {
            showError(R.string.quran_invalid_surah);
            return;
        }
        selectedAyah = savedInstanceState == null
                ? getIntent().getIntExtra(EXTRA_AYAH, 1)
                : savedInstanceState.getInt(STATE_AYAH, 1);
        if (selectedAyah < 1 || selectedAyah > QuranMetadata.ayahCount(surah)) {
            showError(R.string.quran_invalid_ayah);
            return;
        }
        TextView surahView = findViewById(R.id.quran_surah_name);
        String name = getResources().getStringArray(R.array.quran_surah_names)[surah - 1];
        surahView.setText(getString(R.string.quran_surah_label, name));
        setTitle(surahView.getText());
        versesView.setMovementMethod(LinkMovementMethod.getInstance());

        QuranDatabaseHelper database = new QuranDatabaseHelper(getApplicationContext());
        AssetManager assets = getAssets();
        int requestedSurah = surah;
        int restoredScroll = savedInstanceState == null
                ? -1 : savedInstanceState.getInt(STATE_SCROLL, 0);
        executor.execute(() -> {
            try {
                // Do not silently replace a missing Quran font with a system fallback.
                Typeface font = Typeface.createFromAsset(assets, "fonts/quran_font.ttf");
                List<Ayah> verses = database.getVersesBySurah(requestedSurah);
                if (destroyed) return;
                mainHandler.post(() -> {
                    if (destroyed || isFinishing()) return;
                    versesView.setTypeface(font);
                    displayVerses(verses);
                    progress.setVisibility(View.GONE);
                    statusView.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    scrollView.post(() -> {
                        if (destroyed || isFinishing()) return;
                        if (restoredScroll >= 0) {
                            scrollView.scrollTo(0, restoredScroll);
                        } else if (versesView.getLayout() != null) {
                            int line = versesView.getLayout().getLineForOffset(
                                    verseStarts[selectedAyah - 1]);
                            scrollView.scrollTo(0, versesView.getLayout().getLineTop(line));
                        }
                    });
                });
            } catch (IOException | RuntimeException error) {
                Log.e("QuranActivity", "Unable to open the bundled Quran assets", error);
                if (destroyed) return;
                mainHandler.post(() -> {
                    if (!destroyed && !isFinishing()) showError(R.string.quran_load_error);
                });
            }
        });
    }

    /** Android 15/16 enforce edge-to-edge for this target; protect header/footer/cutouts. */
    @SuppressWarnings("deprecation")
    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            View root = findViewById(R.id.quran_root);
            int left = root.getPaddingLeft();
            int top = root.getPaddingTop();
            int right = root.getPaddingRight();
            int bottom = root.getPaddingBottom();
            root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets safe = windowInsets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                // Always add to the original padding, not previously applied insets.
                view.setPadding(left + safe.left, top + safe.top,
                        right + safe.right, bottom + safe.bottom);
                return WindowInsets.CONSUMED;
            });
            root.requestApplyInsets();
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBars, lightBars);
            }
            if (Build.VERSION.SDK_INT < 35) {
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                getWindow().setNavigationBarColor(Color.TRANSPARENT);
            }
        } else {
            // API 23–29: keep the platform's default non-edge-to-edge content fitting.
            getWindow().setStatusBarColor(getColor(R.color.quran_paper));
            getWindow().setNavigationBarColor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? getColor(R.color.quran_paper) : Color.BLACK);
            int flags = getWindow().getDecorView().getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void displayVerses(List<Ayah> verses) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        verseStarts = new int[verses.size()];
        verseEnds = new int[verses.size()];
        for (Ayah verse : verses) {
            if (builder.length() > 0) builder.append(' ');
            int start = builder.length();
            // Preserve Quran text verbatim. Nonbreaking spaces keep each marker together.
            builder.append(verse.text).append(" \uFD3F\u00A0")
                    .append(QuranMetadata.arabicNumber(verse.number)).append("\u00A0\uFD3E");
            int end = builder.length();
            verseStarts[verse.number - 1] = start;
            verseEnds[verse.number - 1] = end;
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    selectAyah(verse.number);
                }

                @Override
                public void updateDrawState(TextPaint paint) {
                    paint.setUnderlineText(false); // Preserve ink color, not link blue.
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        versesView.setText(builder, TextView.BufferType.SPANNABLE);
        renderedText = (Spannable) versesView.getText();
        selectAyah(selectedAyah);
    }

    private void selectAyah(int ayah) {
        selectedAyah = ayah;
        if (selection != null) renderedText.removeSpan(selection);
        selection = new BackgroundColorSpan(getColor(R.color.quran_selected));
        renderedText.setSpan(selection, verseStarts[ayah - 1], verseEnds[ayah - 1],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        juzView.setText(getString(R.string.quran_juz_label,
                QuranMetadata.arabicNumber(QuranMetadata.juzFor(surah, ayah))));
        pageView.setText(getString(R.string.quran_page_label,
                QuranMetadata.arabicNumber(QuranMetadata.pageFor(surah, ayah))));
        versesView.invalidate();
    }

    private void showError(int message) {
        progress.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(message);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_AYAH, selectedAyah);
        outState.putInt(STATE_SCROLL, scrollView.getScrollY());
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
```

## Activity layout

File: `app/src/main/res/layout/activity_quran.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/quran_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/quran_paper"
    android:orientation="vertical"
    android:paddingStart="20dp"
    android:paddingEnd="20dp"
    tools:context=".activity.QuranActivity">

    <!-- Fixed physical placement: Juz left, Surah right, in either device locale. -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="56dp"
        android:gravity="center_vertical"
        android:layoutDirection="ltr"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/quran_juz"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="left"
            android:textColor="@color/quran_muted"
            android:textDirection="rtl"
            android:textSize="16sp"
            tools:text="الجزء ١" />

        <TextView
            android:id="@+id/quran_surah_name"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="2"
            android:gravity="right"
            android:textColor="@color/quran_muted"
            android:textDirection="rtl"
            android:textSize="18sp"
            tools:text="سورة الفَاتِحَةِ" />
    </LinearLayout>

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <ScrollView
            android:id="@+id/quran_scroll"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:fillViewport="true"
            android:saveEnabled="false"
            android:scrollbars="vertical"
            android:visibility="gone">

            <TextView
                android:id="@+id/quran_verses"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:includeFontPadding="true"
                android:layoutDirection="rtl"
                android:lineSpacingExtra="10dp"
                android:lineSpacingMultiplier="1.15"
                android:paddingTop="16dp"
                android:paddingBottom="24dp"
                android:saveEnabled="false"
                android:textColor="@color/quran_ink"
                android:textColorHighlight="@color/quran_selected"
                android:textDirection="rtl"
                android:textSize="28sp" />
        </ScrollView>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:orientation="vertical">

            <ProgressBar
                android:id="@+id/quran_progress"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:contentDescription="@string/quran_loading" />

            <TextView
                android:id="@+id/quran_status"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:accessibilityLiveRegion="polite"
                android:gravity="center"
                android:text="@string/quran_loading"
                android:textColor="@color/quran_muted"
                android:textSize="16sp" />
        </LinearLayout>
    </FrameLayout>

    <TextView
        android:id="@+id/quran_page"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="48dp"
        android:gravity="right|center_vertical"
        android:textColor="@color/quran_muted"
        android:textDirection="rtl"
        android:textSize="16sp"
        tools:text="صفحة ١" />
</LinearLayout>
```

## Colors, strings, and theme

File: `app/src/main/res/values/quran.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="quran_paper">#FBF9F0</color>
    <color name="quran_muted">#7A7A7A</color>
    <color name="quran_selected">#D0ECE4</color>
    <color name="quran_ink">#1A1A1A</color>

    <string name="quran_title">Quran</string>
    <string name="quran_loading">Loading the offline Quran…</string>
    <string name="quran_invalid_surah">Please open a Surah between 1 and 114.</string>
    <string name="quran_invalid_ayah">This ayah number is not valid for the selected Surah.</string>
    <string name="quran_load_error">The offline Quran could not be loaded. Make sure this app bundles a valid databases/quran.ar.uthmani.db and fonts/quran_font.ttf, then reopen this screen.</string>
    <string name="quran_surah_label" translatable="false">سورة %1$s</string>
    <string name="quran_juz_label" translatable="false">الجزء %1$s</string>
    <string name="quran_page_label" translatable="false">صفحة %1$s</string>

    <!-- Native theme; this Activity does not depend on AppCompat or network libraries. -->
    <style name="QuranTheme" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowBackground">@color/quran_paper</item>
        <item name="android:windowActionModeOverlay">true</item>
    </style>
</resources>
```

## Madani metadata

File: `app/src/main/java/com/clock/livewallpaper/quran/QuranMetadata.java`

```java
package com.clock.livewallpaper.quran;

/**
 * Offline metadata for the standard 604-page Madani Mushaf (Hafs numbering).
 * Numeric metadata adapted from quran/quran_android's MadaniDataSource.kt.
 * See docs/OFFLINE_QURAN.md and docs/licenses/quran_android-GPL-3.0.txt.
 * Metadata portions: SPDX-License-Identifier: GPL-3.0-only
 */
public final class QuranMetadata {
    private QuranMetadata() { }

    private static final int[] AYAH_COUNTS = {
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
        89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
        30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    };

    // Entries use surah * 1000 + ayah for lexicographic ordering.
    private static final int[] PAGE_STARTS = {
        1001, 2001, 2006, 2017, 2025, 2030, 2038, 2049, 2058, 2062, 2070, 2077, 2084, 2089, 2094,
        2102, 2106, 2113, 2120, 2127, 2135, 2142, 2146, 2154, 2164, 2170, 2177, 2182, 2187, 2191,
        2197, 2203, 2211, 2216, 2220, 2225, 2231, 2234, 2238, 2246, 2249, 2253, 2257, 2260, 2265,
        2270, 2275, 2282, 2283, 3001, 3010, 3016, 3023, 3030, 3038, 3046, 3053, 3062, 3071, 3078,
        3084, 3092, 3101, 3109, 3116, 3122, 3133, 3141, 3149, 3154, 3158, 3166, 3174, 3181, 3187,
        3195, 4001, 4007, 4012, 4015, 4020, 4024, 4027, 4034, 4038, 4045, 4052, 4060, 4066, 4075,
        4080, 4087, 4092, 4095, 4102, 4106, 4114, 4122, 4128, 4135, 4141, 4148, 4155, 4163, 4171,
        4176, 5003, 5006, 5010, 5014, 5018, 5024, 5032, 5037, 5042, 5046, 5051, 5058, 5065, 5071,
        5077, 5083, 5090, 5096, 5104, 5109, 5114, 6001, 6009, 6019, 6028, 6036, 6045, 6053, 6060,
        6069, 6074, 6082, 6091, 6095, 6102, 6111, 6119, 6125, 6132, 6138, 6143, 6147, 6152, 6158,
        7001, 7012, 7023, 7031, 7038, 7044, 7052, 7058, 7068, 7074, 7082, 7088, 7096, 7105, 7121,
        7131, 7138, 7144, 7150, 7156, 7160, 7164, 7171, 7179, 7188, 7196, 8001, 8009, 8017, 8026,
        8034, 8041, 8046, 8053, 8062, 8070, 9001, 9007, 9014, 9021, 9027, 9032, 9037, 9041, 9048,
        9055, 9062, 9069, 9073, 9080, 9087, 9094, 9100, 9107, 9112, 9118, 9123, 10001, 10007, 10015,
        10021, 10026, 10034, 10043, 10054, 10062, 10071, 10079, 10089, 10098, 10107, 11006, 11013,
        11020, 11029, 11038, 11046, 11054, 11063, 11072, 11082, 11089, 11098, 11109, 11118, 12005,
        12015, 12023, 12031, 12038, 12044, 12053, 12064, 12070, 12079, 12087, 12096, 12104, 13001,
        13006, 13014, 13019, 13029, 13035, 13043, 14006, 14011, 14019, 14025, 14034, 14043, 15001,
        15016, 15032, 15052, 15071, 15091, 16007, 16015, 16027, 16035, 16043, 16055, 16065, 16073,
        16080, 16088, 16094, 16103, 16111, 16119, 17001, 17008, 17018, 17028, 17039, 17050, 17059,
        17067, 17076, 17087, 17097, 17105, 18005, 18016, 18021, 18028, 18035, 18046, 18054, 18062,
        18075, 18084, 18098, 19001, 19012, 19026, 19039, 19052, 19065, 19077, 19096, 20013, 20038,
        20052, 20065, 20077, 20088, 20099, 20114, 20126, 21001, 21011, 21025, 21036, 21045, 21058,
        21073, 21082, 21091, 21102, 22001, 22006, 22016, 22024, 22031, 22039, 22047, 22056, 22065,
        22073, 23001, 23018, 23028, 23043, 23060, 23075, 23090, 23105, 24001, 24011, 24021, 24028,
        24032, 24037, 24044, 24054, 24059, 24062, 25003, 25012, 25021, 25033, 25044, 25056, 25068,
        26001, 26020, 26040, 26061, 26084, 26112, 26137, 26160, 26184, 26207, 27001, 27014, 27023,
        27036, 27045, 27056, 27064, 27077, 27089, 28006, 28014, 28022, 28029, 28036, 28044, 28051,
        28060, 28071, 28078, 28085, 29007, 29015, 29024, 29031, 29039, 29046, 29053, 29064, 30006,
        30016, 30025, 30033, 30042, 30051, 31001, 31012, 31020, 31029, 32001, 32012, 32021, 33001,
        33007, 33016, 33023, 33031, 33036, 33044, 33051, 33055, 33063, 34001, 34008, 34015, 34023,
        34032, 34040, 34049, 35004, 35012, 35019, 35031, 35039, 35045, 36013, 36028, 36041, 36055,
        36071, 37001, 37025, 37052, 37077, 37103, 37127, 37154, 38001, 38017, 38027, 38043, 38062,
        38084, 39006, 39011, 39022, 39032, 39041, 39048, 39057, 39068, 39075, 40008, 40017, 40026,
        40034, 40041, 40050, 40059, 40067, 40078, 41001, 41012, 41021, 41030, 41039, 41047, 42001,
        42011, 42016, 42023, 42032, 42045, 42052, 43011, 43023, 43034, 43048, 43061, 43074, 44001,
        44019, 44040, 45001, 45014, 45023, 45033, 46006, 46015, 46021, 46029, 47001, 47012, 47020,
        47030, 48001, 48010, 48016, 48024, 48029, 49005, 49012, 50001, 50016, 50036, 51007, 51031,
        51052, 52015, 52032, 53001, 53027, 53045, 54007, 54028, 54050, 55017, 55041, 55068, 56017,
        56051, 56077, 57004, 57012, 57019, 57025, 58001, 58007, 58012, 58022, 59004, 59010, 59017,
        60001, 60006, 60012, 61006, 62001, 62009, 63005, 64001, 64010, 65001, 65006, 66001, 66008,
        67001, 67013, 67027, 68016, 68043, 69009, 69035, 70011, 70040, 71011, 72001, 72014, 73001,
        73020, 74018, 74048, 75020, 76006, 76026, 77020, 78001, 78031, 79016, 80001, 81001, 82001,
        83007, 83035, 85001, 86001, 87016, 89001, 89024, 91001, 92015, 95001, 97001, 98008, 100010,
        103001, 106001, 109001, 112001
    };

    // Entries use surah * 1000 + ayah for lexicographic ordering.
    private static final int[] JUZ_STARTS = {
        1001, 2142, 2253, 3093, 4024, 4148, 5082, 6111, 7088, 8041, 9093, 11006, 12053, 15001,
        17001, 18075, 21001, 23001, 25021, 27056, 29046, 33031, 36028, 39032, 41047, 46001, 51031,
        58001, 67001, 78001
    };

    public static int ayahCount(int surah) {
        if (surah < 1 || surah > 114) {
            throw new IllegalArgumentException("Surah must be between 1 and 114.");
        }
        return AYAH_COUNTS[surah - 1];
    }

    public static int pageFor(int surah, int ayah) {
        return sectionFor(PAGE_STARTS, surah, ayah);
    }

    public static int juzFor(int surah, int ayah) {
        return sectionFor(JUZ_STARTS, surah, ayah);
    }

    private static int sectionFor(int[] starts, int surah, int ayah) {
        if (ayah < 1 || ayah > ayahCount(surah)) {
            throw new IllegalArgumentException("Invalid ayah for Surah " + surah);
        }
        int key = surah * 1000 + ayah;
        int low = 0;
        int high = starts.length;
        // Upper bound: the last section starting at or before this ayah, one-based.
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (starts[middle] <= key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    public static String arabicNumber(int value) {
        String western = Integer.toString(value);
        StringBuilder arabic = new StringBuilder(western.length());
        for (int i = 0; i < western.length(); i++) {
            char digit = western.charAt(i);
            arabic.append(digit >= '0' && digit <= '9'
                    ? (char) ('\u0660' + digit - '0') : digit);
        }
        return arabic.toString();
    }
}
```

## Arabic Surah names

File: `app/src/main/res/values/quran_surah_names.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Arabic names from quran/quran_android (GPL-3.0-only).
     See docs/OFFLINE_QURAN.md for the pinned source and license. -->
<resources>
  <string-array name="quran_surah_names" translatable="false">
    <item>الفَاتِحَةِ</item>
    <item>البَقَرَةِ</item>
    <item>آلِ عِمۡرَانَ</item>
    <item>النِّسَاءِ</item>
    <item>المَائـِدَةِ</item>
    <item>الأَنۡعَامِ</item>
    <item>الأَعۡرَافِ</item>
    <item>الأَنفَالِ</item>
    <item>التَّوۡبَةِ</item>
    <item>يُونُسَ</item>
    <item>هُودٍ</item>
    <item>يُوسُفَ</item>
    <item>الرَّعۡدِ</item>
    <item>إِبۡرَاهِيمَ</item>
    <item>الحِجۡرِ</item>
    <item>النَّحۡلِ</item>
    <item>الإِسۡرَاءِ</item>
    <item>الكَهۡفِ</item>
    <item>مَرۡيَمَ</item>
    <item>طه</item>
    <item>الأَنبِيَاءِ</item>
    <item>الحَجِّ</item>
    <item>المُؤۡمِنُونَ</item>
    <item>النُّورِ</item>
    <item>الفُرۡقَانِ</item>
    <item>الشُّعَرَاءِ</item>
    <item>النَّمۡلِ</item>
    <item>القَصَصِ</item>
    <item>العَنكَبُوتِ</item>
    <item>الرُّومِ</item>
    <item>لُقۡمَانَ</item>
    <item>السَّجۡدَةِ</item>
    <item>الأَحۡزَابِ</item>
    <item>سَبَإٍ</item>
    <item>فَاطِرٍ</item>
    <item>يسٓ</item>
    <item>الصَّافَّاتِ</item>
    <item>صٓ</item>
    <item>الزُّمَرِ</item>
    <item>غَافِرٍ</item>
    <item>فُصِّلَتۡ</item>
    <item>الشُّورَىٰ</item>
    <item>الزُّخۡرُفِ</item>
    <item>الدُّخَانِ</item>
    <item>الجَاثِيَةِ</item>
    <item>الأَحۡقَافِ</item>
    <item>مُحَمَّدٍ</item>
    <item>الفَتۡحِ</item>
    <item>الحُجُرَاتِ</item>
    <item>قٓ</item>
    <item>الذَّارِيَاتِ</item>
    <item>الطُّورِ</item>
    <item>النَّجۡمِ</item>
    <item>القَمَرِ</item>
    <item>الرَّحۡمَٰن</item>
    <item>الوَاقِعَةِ</item>
    <item>الحَدِيدِ</item>
    <item>المُجَادلَةِ</item>
    <item>الحَشۡرِ</item>
    <item>المُمۡتَحنَةِ</item>
    <item>الصَّفِّ</item>
    <item>الجُمُعَةِ</item>
    <item>المُنَافِقُونَ</item>
    <item>التَّغَابُنِ</item>
    <item>الطَّلَاقِ</item>
    <item>التَّحۡرِيمِ</item>
    <item>المُلۡكِ</item>
    <item>القَلَمِ</item>
    <item>الحَاقَّةِ</item>
    <item>المَعَارِجِ</item>
    <item>نُوحٍ</item>
    <item>الجِنِّ</item>
    <item>المُزَّمِّلِ</item>
    <item>المُدَّثِّرِ</item>
    <item>القِيَامَةِ</item>
    <item>الإِنسَانِ</item>
    <item>المُرۡسَلَاتِ</item>
    <item>النَّبَإِ</item>
    <item>النَّازِعَاتِ</item>
    <item>عَبَسَ</item>
    <item>التَّكۡوِيرِ</item>
    <item>الانفِطَارِ</item>
    <item>المُطَفِّفِينَ</item>
    <item>الانشِقَاقِ</item>
    <item>البُرُوجِ</item>
    <item>الطَّارِقِ</item>
    <item>الأَعۡلَىٰ</item>
    <item>الغَاشِيَةِ</item>
    <item>الفَجۡرِ</item>
    <item>البَلَدِ</item>
    <item>الشَّمۡسِ</item>
    <item>اللَّيۡلِ</item>
    <item>الضُّحَىٰ</item>
    <item>الشَّرۡحِ</item>
    <item>التِّينِ</item>
    <item>العَلَقِ</item>
    <item>القَدۡرِ</item>
    <item>البَيِّنَةِ</item>
    <item>الزَّلۡزَلَةِ</item>
    <item>العَادِيَاتِ</item>
    <item>القَارِعَةِ</item>
    <item>التَّكَاثُرِ</item>
    <item>العَصۡرِ</item>
    <item>الهُمَزَةِ</item>
    <item>الفِيلِ</item>
    <item>قُرَيۡشٍ</item>
    <item>المَاعُونِ</item>
    <item>الكَوۡثَرِ</item>
    <item>الكَافِرُونَ</item>
    <item>النَّصۡرِ</item>
    <item>المَسَدِ</item>
    <item>الإِخۡلَاصِ</item>
    <item>الفَلَقِ</item>
    <item>النَّاسِ</item>
  </string-array>
</resources>
```

## Database preparation at application startup

File: `app/src/main/java/com/clock/livewallpaper/AppClass.java`

```java
package com.clock.livewallpaper;

import android.app.Application;
import android.util.Log;

import com.clock.livewallpaper.quran.QuranDatabaseHelper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppClass extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Prepare local Quran data at app launch without delaying the Clock/Wallpaper UI.
        // The helper's lock also covers a reader opened while this task is still running.
        ExecutorService installer = Executors.newSingleThreadExecutor();
        installer.execute(() -> {
            try {
                new QuranDatabaseHelper(this).prepareDatabase();
            } catch (IOException | RuntimeException error) {
                // Keep other app features usable. QuranActivity retries and displays an error.
                Log.e("AppClass", "Cannot prepare the bundled offline Quran database", error);
            }
        });
        installer.shutdown(); // Runs the submitted task, then releases this startup thread.
    }
}
```

## Verification and licensing

Six SDK-independent checks passed. Three checks were skipped because the JDK and real database/font assets were not available. An Android build was attempted but could not launch without a JDK; compilation and device behavior remain unverified.

After installing JDK 17 and Android SDK 36, run:

```sh
bash gradlew :app:assembleDebug :app:lintDebug
```

The Quran metadata and Arabic Surah names are adapted from the GPL-3.0 project `https://github.com/quran/quran_android`, revision `1ec595ecced95ab215b1e9c542c910c4c5436166`. Preserve the upstream license provided at `docs/licenses/quran_android-GPL-3.0.txt` and comply with distribution obligations. Verify the database and font licenses separately.

The Quran screen includes native system-bar/cutout inset handling. The rest of the legacy Clock/Wallpaper app and its third-party dependencies still need a full target-SDK-36 compatibility audit before release.
