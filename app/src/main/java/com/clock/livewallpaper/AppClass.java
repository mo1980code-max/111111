package com.clock.livewallpaper;

import android.app.Application;
import android.util.Log;

import com.clock.livewallpaper.ads.AdPolicy;
import com.clock.livewallpaper.ads.AdsManager;
import com.clock.livewallpaper.ads.AppForegroundWatcher;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Process entry point.
 *
 * <p>Two ad-related jobs only, both deliberately passive: the SDK is initialised so a rewarded or native
 * ad can be filled when the user asks for one, and {@link AppForegroundWatcher} is registered so an app
 * open ad can appear on a return from background -- never on a cold start, and never while the Quran
 * section owns the screen ({@link AdPolicy#enterQuranScreen()}). Nothing is shown from here.
 */
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

        AdPolicy.registerLaunch(this);
        AdsManager.get().initialize(this);
        AppForegroundWatcher.register(this);
    }
}
