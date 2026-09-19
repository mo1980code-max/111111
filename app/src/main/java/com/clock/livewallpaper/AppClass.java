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
