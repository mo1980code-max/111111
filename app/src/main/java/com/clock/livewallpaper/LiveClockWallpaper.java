package com.clock.livewallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.view.InputDeviceCompat;

import com.clock.livewallpaper.clock.ClockStudioWallpaperConfig;
import com.clock.livewallpaper.clock.ClockStudioWallpaperRenderer;
import com.clock.livewallpaper.model.Clocks;
import com.clock.livewallpaper.utils.TinyDB;
import com.clock.livewallpaper.viewUtils.AnalogClock;
import com.clock.livewallpaper.viewUtils.SmartClockPreview;
import com.clock.livewallpaper.viewUtils.TextClockPreview;


public class LiveClockWallpaper extends WallpaperService {
    protected TextClockPreview cat1Clock;
    private Context context;
    int height;
    protected ImageView imageView;
    protected AnalogClock imageViewBase;
    private int mClockSize;
    private int mHalfWidth;
    protected SmartClockPreview smartClockPreview;
    TinyDB tinyDB;
    protected WidgetGroup widgetGroup;
    int width;
    private final Handler mHandler = new Handler();
    private float mClockPosX = -1.0f;
    private float mClockPosY = -1.0f;
    private boolean studioMode;
    private ClockStudioWallpaperRenderer studioRenderer;

    @Override
    public void onCreate() {
        super.onCreate();
        Context applicationContext = getApplicationContext();
        this.context = applicationContext;
        this.studioMode = ClockStudioWallpaperConfig.isEnabled(applicationContext);
        if (this.studioMode) {
            this.studioRenderer = new ClockStudioWallpaperRenderer(applicationContext,
                    ClockStudioWallpaperConfig.read(applicationContext));
        } else {
            this.tinyDB = new TinyDB(applicationContext);
            init(this.context);
        }
    }

    public void init(Context context) {
        WidgetGroup widgetGroup = new WidgetGroup(context);
        this.widgetGroup = widgetGroup;
        widgetGroup.removeAllViews();
        this.imageViewBase = new AnalogClock(context);
        ImageView imageView = new ImageView(context);
        this.imageView = imageView;
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        this.cat1Clock = new TextClockPreview(context);
        this.smartClockPreview = new SmartClockPreview(context);
        // The engine owns the visibility lifecycle; do not keep a clock handler alive while the
        // wallpaper is only configured or the surface is hidden.
        this.imageViewBase.setAutoUpdate(false);
        this.widgetGroup.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        this.widgetGroup.setAddStatesFromChildren(true);
        this.widgetGroup.addView(this.imageView);
        this.widgetGroup.addView(this.imageViewBase);
        this.imageViewBase.setVisibility(View.GONE);
        this.widgetGroup.addView(this.cat1Clock);
        this.widgetGroup.addView(this.smartClockPreview);
    }

    @Override
    public void onDestroy() {
        if (this.studioRenderer != null) {
            this.studioRenderer.destroy();
            this.studioRenderer = null;
        }
        if (this.imageViewBase != null) {
            this.imageViewBase.setAutoUpdate(false);
        }
        this.mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    /**
     * Re-reads the handoff flag for new engines as well as a newly-created service. Android may keep
     * one WallpaperService instance alive while the user applies another snapshot from Clock Studio.
     */
    private void syncStudioMode() {
        boolean shouldUseStudio = ClockStudioWallpaperConfig.isEnabled(this.context);
        if (!shouldUseStudio) {
            if (this.studioMode && this.studioRenderer != null) {
                this.studioRenderer.destroy();
                this.studioRenderer = null;
            }
            this.studioMode = false;
            if (this.tinyDB == null) {
                this.tinyDB = new TinyDB(this.context);
                init(this.context);
            }
            return;
        }

        this.studioMode = true;
        if (this.studioRenderer != null) {
            this.studioRenderer.destroy();
        }
        this.studioRenderer = new ClockStudioWallpaperRenderer(this.context,
                ClockStudioWallpaperConfig.read(this.context));
        if (this.width > 0 && this.height > 0) {
            this.studioRenderer.setSize(this.width, this.height);
        }
    }


    public static class WidgetGroup extends ViewGroup {
        private final String TAG = getClass().getSimpleName();

        public WidgetGroup(Context context) {
            super(context);
            setWillNotDraw(true);
        }

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            layout(i, i2, i3, i4);
        }
    }


    class ClockEngine extends Engine {
        private final Runnable mDrawClock = new Runnable() {
            @Override
            public void run() {
                ClockEngine.this.drawFrame();
            }
        };
        private boolean mVisible;

        ClockEngine() {
            super();
        }


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            LiveClockWallpaper.this.syncStudioMode();
        }

        @Override
        public void onDestroy() {
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (!LiveClockWallpaper.this.studioMode
                    && LiveClockWallpaper.this.imageViewBase != null) {
                LiveClockWallpaper.this.imageViewBase.setAutoUpdate(false);
            }
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean z) {
            this.mVisible = z;
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (z) {
                LiveClockWallpaper.this.syncStudioMode();
            }
            if (LiveClockWallpaper.this.studioMode) {
                if (z) {
                    drawFrame();
                }
                return;
            }
            LiveClockWallpaper.this.imageViewBase.setAutoUpdate(z);
            if (z) {
                drawFrame();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            super.onSurfaceChanged(surfaceHolder, i, i2, i3);
            LiveClockWallpaper.this.width = i2;
            LiveClockWallpaper.this.height = i3;
            if (LiveClockWallpaper.this.studioMode
                    && LiveClockWallpaper.this.studioRenderer != null) {
                LiveClockWallpaper.this.studioRenderer.setSize(i2, i3);
            }
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            if (LiveClockWallpaper.this.studioMode && this.mVisible) {
                drawFrame();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
            this.mVisible = false;
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (!LiveClockWallpaper.this.studioMode
                    && LiveClockWallpaper.this.imageViewBase != null) {
                LiveClockWallpaper.this.imageViewBase.setAutoUpdate(false);
            }
        }

        @Override
        public void onOffsetsChanged(float f, float f2, float f3, float f4, int i, int i2) {
            if (this.mVisible) {
                drawFrame();
            }
        }

        void drawFrame() {
            if (LiveClockWallpaper.this.studioMode) {
                drawStudioFrame();
                return;
            }
            Throwable th;
            Canvas canvas;
            SurfaceHolder surfaceHolder = getSurfaceHolder();
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    try {
                        drawClock(canvas);
                    } catch (Throwable th2) {
                        th = th2;
                        if (canvas != null) {
                            try {
                                surfaceHolder.unlockCanvasAndPost(canvas);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        }
                        throw th;
                    }
                }
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException e2) {
                        e2.printStackTrace();
                    }
                }
                LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
                if (this.mVisible) {
                    LiveClockWallpaper.this.mHandler.postDelayed(this.mDrawClock, 1000);
                }
            } catch (Throwable th3) {
                th = th3;
                canvas = null;
            }
        }

        private void drawStudioFrame() {
            LiveClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (!this.mVisible || LiveClockWallpaper.this.studioRenderer == null) {
                return;
            }
            Canvas canvas = null;
            try {
                canvas = getSurfaceHolder().lockCanvas();
                if (canvas != null) {
                    LiveClockWallpaper.this.studioRenderer.draw(canvas);
                }
            } catch (RuntimeException ignored) {
                // Surface loss during launcher transitions is normal; the next visible callback retries.
            } finally {
                if (canvas != null) {
                    try {
                        getSurfaceHolder().unlockCanvasAndPost(canvas);
                    } catch (RuntimeException ignored) {
                        // The surface may have been destroyed between lock and post.
                    }
                }
            }
            if (this.mVisible && LiveClockWallpaper.this.studioRenderer != null) {
                LiveClockWallpaper.this.mHandler.postDelayed(this.mDrawClock,
                        LiveClockWallpaper.this.studioRenderer.nextFrameDelayMillis());
            }
        }

        void drawClock(Canvas canvas) {
            canvas.save();
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            firstClock(canvas);
            canvas.restore();
        }

        Bitmap aa;

        public void firstClock(Canvas canvas) {
            LiveClockWallpaper.this.widgetGroup.layout(0, 0, LiveClockWallpaper.this.width, LiveClockWallpaper.this.height);
            LiveClockWallpaper liveClockWallpaper = LiveClockWallpaper.this;
            liveClockWallpaper.mClockPosX = liveClockWallpaper.tinyDB.getFloat("prefClockPosX", ((float) LiveClockWallpaper.this.width) / 2.0f);
            LiveClockWallpaper liveClockWallpaper2 = LiveClockWallpaper.this;
            liveClockWallpaper2.mClockPosY = liveClockWallpaper2.tinyDB.getFloat("prefClockPosY", ((float) LiveClockWallpaper.this.height) / 2.0f);
            LiveClockWallpaper liveClockWallpaper3 = LiveClockWallpaper.this;
            liveClockWallpaper3.mClockSize = liveClockWallpaper3.tinyDB.getInt("prefSize");
            Clocks clocks = (Clocks) LiveClockWallpaper.this.tinyDB.getObject("clocks", Clocks.class);
            int i = LiveClockWallpaper.this.tinyDB.getInt("textClockPosition");
            if (LiveClockWallpaper.this.tinyDB.getBoolean("isImage")) {
                if (aa != null) {
                    LiveClockWallpaper.this.imageView.setImageBitmap(aa);
                } else {
                    aa = BitmapFactory.decodeFile(LiveClockWallpaper.this.tinyDB.getString("ImageString"));
                    LiveClockWallpaper.this.imageView.setImageBitmap(aa);
                }

            } else if (LiveClockWallpaper.this.tinyDB.getBoolean("isCustomBg")) {
                LiveClockWallpaper.this.imageView.setImageResource(LiveClockWallpaper.this.tinyDB.getInt("customBg"));
            } else {
                LiveClockWallpaper.this.imageView.setImageResource(0);
                LiveClockWallpaper.this.imageView.setBackgroundColor(LiveClockWallpaper.this.tinyDB.getInt("bgColor"));
            }
            LiveClockWallpaper.this.imageView.layout(0, 0, LiveClockWallpaper.this.width, LiveClockWallpaper.this.height);
            if (LiveClockWallpaper.this.tinyDB.getInt("clockType") == 0) {
                LiveClockWallpaper.this.imageViewBase.setAutoUpdate(this.mVisible);
                LiveClockWallpaper.this.imageViewBase.setClock(clocks);
                LiveClockWallpaper.this.imageViewBase.setClockSize((float) LiveClockWallpaper.this.mClockSize);
                LiveClockWallpaper.this.imageViewBase.setPosition(LiveClockWallpaper.this.mClockPosX, LiveClockWallpaper.this.mClockPosY);
                LiveClockWallpaper.this.imageViewBase.layout(0, 0, LiveClockWallpaper.this.width, LiveClockWallpaper.this.height);
                LiveClockWallpaper.this.imageViewBase.setVisibility(View.VISIBLE);
                LiveClockWallpaper.this.smartClockPreview.setVisibility(View.GONE);
                LiveClockWallpaper.this.cat1Clock.setVisibility(View.GONE);
            } else if (LiveClockWallpaper.this.tinyDB.getInt("clockType") == 1) {
                LiveClockWallpaper.this.imageViewBase.setAutoUpdate(false);
                LiveClockWallpaper.this.smartClockPreview.layout(0, 0, LiveClockWallpaper.this.width, LiveClockWallpaper.this.height);
                LiveClockWallpaper.this.smartClockPreview.setTextClockPosition(i);
                LiveClockWallpaper.this.smartClockPreview.config(LiveClockWallpaper.this.mClockPosX, LiveClockWallpaper.this.mClockPosY, LiveClockWallpaper.this.mClockSize * 2);
                LiveClockWallpaper.this.imageViewBase.setVisibility(View.GONE);
                LiveClockWallpaper.this.smartClockPreview.setVisibility(View.VISIBLE);
                LiveClockWallpaper.this.cat1Clock.setVisibility(View.GONE);
            } else if (LiveClockWallpaper.this.tinyDB.getInt("clockType") == 2) {
                LiveClockWallpaper.this.imageViewBase.setAutoUpdate(false);
                LiveClockWallpaper.this.cat1Clock.layout(0, 0, LiveClockWallpaper.this.width, LiveClockWallpaper.this.height);
                LiveClockWallpaper.this.cat1Clock.setTextClockPosition(i);
                LiveClockWallpaper.this.cat1Clock.setColors(LiveClockWallpaper.this.tinyDB.getInt("textColor1", -1), LiveClockWallpaper.this.tinyDB.getInt("textColor2", InputDeviceCompat.SOURCE_ANY));
                LiveClockWallpaper.this.cat1Clock.config(LiveClockWallpaper.this.mClockPosX, LiveClockWallpaper.this.mClockPosY, LiveClockWallpaper.this.mClockSize * 2);
                LiveClockWallpaper.this.imageViewBase.setVisibility(View.GONE);
                LiveClockWallpaper.this.smartClockPreview.setVisibility(View.GONE);
                LiveClockWallpaper.this.cat1Clock.setVisibility(View.VISIBLE);
            }
            LiveClockWallpaper.this.widgetGroup.draw(canvas);
        }
    }
}
