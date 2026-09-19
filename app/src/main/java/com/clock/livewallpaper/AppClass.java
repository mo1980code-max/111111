package com.clock.livewallpaper;

import android.app.Application;
import android.util.Log;

import com.clock.livewallpaper.quran.QuranDatabaseHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppClass extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ExecutorService installer = Executors.newSingleThreadExecutor();
        installer.execute(() -> {
            try {
                new QuranDatabaseHelper(this).prepareDatabase();
            } catch (Exception error) {
                Log.e("AppClass", "Error during app initialization", error);
            }
        });
        installer.shutdown();
    }
}
