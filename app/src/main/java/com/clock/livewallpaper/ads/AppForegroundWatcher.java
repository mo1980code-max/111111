package com.clock.livewallpaper.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Watches background -> foreground transitions and gives the app open ad its only chance to appear.
 *
 * <p>Implemented with plain {@code ActivityLifecycleCallbacks} (no extra dependency): the count of
 * started activities falling to zero means the app went to the background, and rising from zero means
 * it came back. The ad is requested while backgrounded and shown on return, which is the flow Google's
 * app open guide prescribes. It is deliberately <b>not</b> shown on the first foreground of a cold
 * start -- the user would otherwise be yanked out of an app they just opened -- and
 * {@link AdPolicy#canShowAppOpen} blocks it whenever a protected section is on screen.
 */
public final class AppForegroundWatcher implements Application.ActivityLifecycleCallbacks {

    private int startedActivities;
    private boolean hasBeenBackgrounded;

    /** Installs the watcher exactly once, from {@code AppClass.onCreate()}. */
    public static void register(Application application) {
        application.registerActivityLifecycleCallbacks(new AppForegroundWatcher());
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        boolean returningToForeground = startedActivities == 0 && hasBeenBackgrounded;
        startedActivities++;
        if (returningToForeground && !AdPolicy.consumeSystemHandoff()) {
            AdsManager.get().showAppOpenIfAllowed(activity);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (startedActivities > 0) {
            startedActivities--;
        }
        // A rotation (or any configuration change) stops and immediately restarts the same screen:
        // that is not "leaving the app", so it must neither arm the background flag nor pop an app
        // open ad when the new instance starts. Only a real trip to the background qualifies.
        if (startedActivities == 0 && !activity.isChangingConfigurations()) {
            hasBeenBackgrounded = true;
            // Have one ready for the next foreground instead of requesting it in front of the user.
            AdsManager.get().preloadAppOpen(activity.getApplication());
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
