package com.clock.livewallpaper;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.clock.livewallpaper.clock.ClockCompositionRenderer;
import com.clock.livewallpaper.clock.ClockPreferences;
import com.clock.livewallpaper.clock.ClockStudioConfig;

import java.util.Calendar;

/**
 * The one live wallpaper engine used by Clock Studio.
 *
 * <p>It reads the same persisted composition as the editor, renders directly to the wallpaper
 * surface, and owns exactly one cancellable callback.  There is deliberately no Activity, ad SDK,
 * View hierarchy or permanent background worker in this service.
 */
public class LiveClockWallpaper extends WallpaperService {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private ClockCompositionRenderer renderer;
    private ClockStudioConfig configuration;

    @Override
    public void onCreate() {
        super.onCreate();
        this.renderer = new ClockCompositionRenderer(this);
        this.configuration = ClockPreferences.get(this).load();
    }

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    @Override
    public void onDestroy() {
        if (this.renderer != null) {
            this.renderer.release();
            this.renderer = null;
        }
        this.mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void reloadConfiguration(int width, int height) {
        this.configuration = ClockPreferences.get(this).load();
        if (this.renderer != null) {
            this.renderer.setConfiguration(this.configuration, Math.max(1, width), Math.max(1, height));
        }
    }

    private long nextUpdateDelay(Calendar now) {
        if (this.configuration != null && this.configuration.showSeconds) {
            long elapsed = now.get(Calendar.MILLISECOND);
            return Math.max(100L, 1000L - elapsed);
        }
        long elapsed = now.get(Calendar.SECOND) * 1000L + now.get(Calendar.MILLISECOND);
        return Math.max(250L, 60_000L - elapsed);
    }

    private final class ClockEngine extends Engine {
        private final Runnable mDrawClock = new Runnable() {
            @Override
            public void run() {
                ClockEngine.this.drawFrame();
            }
        };
        private boolean mVisible;
        private boolean mSurfaceReady;
        private int mWidth;
        private int mHeight;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.mSurfaceReady = false;
        }

        @Override
        public void onDestroy() {
            this.mVisible = false;
            this.mSurfaceReady = false;
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.mVisible = visible;
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (visible) {
                // Re-read here so a changed Studio selection is picked up when Android shows the
                // preview again, and so a resumed wallpaper jumps directly to the current time.
                LiveClockWallpaper.this.reloadConfiguration(this.mWidth, this.mHeight);
                drawFrame();
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            this.mSurfaceReady = true;
            LiveClockWallpaper.this.reloadConfiguration(this.mWidth, this.mHeight);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);
            this.mWidth = width;
            this.mHeight = height;
            this.mSurfaceReady = true;
            LiveClockWallpaper.this.reloadConfiguration(width, height);
            if (this.mVisible) {
                drawFrame();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            this.mSurfaceReady = false;
            this.mVisible = false;
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            super.onSurfaceDestroyed(surfaceHolder);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
                                     float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            if (this.mVisible) {
                drawFrame();
            }
        }

        private void drawFrame() {
            if (!this.mVisible || !this.mSurfaceReady || LiveClockWallpaper.this.renderer == null) {
                LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
                return;
            }
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            Calendar now = Calendar.getInstance();
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    LiveClockWallpaper.this.renderer.draw(canvas, now);
                }
            } catch (RuntimeException ignored) {
                // A launcher can destroy the surface between lockCanvas and post.  The next
                // visibility/surface callback will safely rebuild it instead of crashing the service.
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (RuntimeException ignored) {
                        // The surface was lost; onSurfaceDestroyed cancels the scheduled callback.
                    }
                }
            }
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (this.mVisible) {
                LiveClockWallpaper.this.mHandler.postDelayed(this.mDrawClock,
                        LiveClockWallpaper.this.nextUpdateDelay(now));
            }
        }
    }
}
