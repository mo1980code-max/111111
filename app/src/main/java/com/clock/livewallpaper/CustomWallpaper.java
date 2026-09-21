package com.clock.livewallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.clock.livewallpaper.utils.TinyDB;



public class CustomWallpaper extends WallpaperService {
    private Context context;
    int height;
    protected ImageView imageView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    TinyDB tinyDB;
    protected WidgetGroup widgetGroup;
    int width;

    @Override
    public void onCreate() {
        super.onCreate();
        Context applicationContext = getApplicationContext();
        this.context = applicationContext;
        this.tinyDB = new TinyDB(applicationContext);
        init(this.context);
    }

    public void init(Context context) {
        WidgetGroup widgetGroup = new WidgetGroup(context);
        this.widgetGroup = widgetGroup;
        widgetGroup.removeAllViews();
        ImageView imageView = new ImageView(context);
        this.imageView = imageView;
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        this.widgetGroup.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        this.widgetGroup.setAddStatesFromChildren(true);
        this.widgetGroup.addView(this.imageView);
    }

    @Override
    public void onDestroy() {
        this.mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }



    public static class WidgetGroup extends ViewGroup {
        private final String TAG = getClass().getSimpleName();

        public WidgetGroup(Context context) {
            super(context);
            setWillNotDraw(true);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            for (int index = 0; index < getChildCount(); index++) {
                getChildAt(index).layout(0, 0, right - left, bottom - top);
            }
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
        private Bitmap wallpaperBitmap;
        private String loadedPath = "";

        ClockEngine() {
            super();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
        }

        @Override
        public void onDestroy() {
            this.mVisible = false;
            CustomWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            if (this.wallpaperBitmap != null && !this.wallpaperBitmap.isRecycled()) {
                this.wallpaperBitmap.recycle();
                this.wallpaperBitmap = null;
            }
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean z) {
            this.mVisible = z;
            if (z) {
                drawFrame();
            } else {
                CustomWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            super.onSurfaceChanged(surfaceHolder, i, i2, i3);
            CustomWallpaper.this.width = i2;
            CustomWallpaper.this.height = i3;
            if (this.mVisible) {
                drawFrame();
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
            this.mVisible = false;
            CustomWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
        }

        @Override
        public void onOffsetsChanged(float f, float f2, float f3, float f4, int i, int i2) {
            if (this.mVisible) {
                drawFrame();
            }
        }

        void drawFrame() {
            if (!this.mVisible || CustomWallpaper.this.width <= 0 || CustomWallpaper.this.height <= 0) {
                CustomWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
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
                CustomWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
                if (this.mVisible) {
                    CustomWallpaper.this.mHandler.postDelayed(this.mDrawClock, 10000);
                }
            } catch (Throwable th3) {
                th = th3;
                canvas = null;
            }
        }

        void drawClock(Canvas canvas) {
            canvas.save();
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            firstClock(canvas);
            canvas.restore();
        }

        public void firstClock(Canvas canvas) {
            CustomWallpaper.this.widgetGroup.layout(0, 0, CustomWallpaper.this.width, CustomWallpaper.this.height);
            String path = CustomWallpaper.this.tinyDB.getString("isWallpaper");
            if (!path.equals(this.loadedPath)) {
                if (this.wallpaperBitmap != null && !this.wallpaperBitmap.isRecycled()) {
                    this.wallpaperBitmap.recycle();
                }
                this.wallpaperBitmap = path.isEmpty() ? null : BitmapFactory.decodeFile(path);
                this.loadedPath = path;
            }
            if (this.wallpaperBitmap != null) {
                CustomWallpaper.this.imageView.setImageBitmap(this.wallpaperBitmap);
            }
            CustomWallpaper.this.imageView.layout(0, 0, CustomWallpaper.this.width, CustomWallpaper.this.height);
            CustomWallpaper.this.widgetGroup.draw(canvas);
        }
    }
}
