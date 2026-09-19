package com.clock.livewallpaper.image;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads the app's own artwork: everything under {@code assets/} (clock previews, wallpapers) and
 * nothing else. There is no HTTP, no Glide and no third party disk cache in this path, so every image
 * in the wallpaper and clock browsers is guaranteed to exist on the device and to render offline.
 *
 * <p>Decoding happens off the main thread with a two-pass {@code inSampleSize}, and results are shared
 * through one process-wide {@link LruCache}. A view tag check discards a decode that lost the race
 * against a recycled row, so lists never flash the wrong image.
 */
public final class LocalImage {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService DISK = Executors.newFixedThreadPool(2);
    private static final int CACHE_BYTES = (int) (Runtime.getRuntime().maxMemory() / 8);
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private LocalImage() {
    }

    /** Asynchronous, cached load into an {@link ImageView}. Silence (no placeholder) on failure. */
    public static void into(final ImageView view, final String assetPath, final int requestedEdge) {
        if (view == null || assetPath == null) {
            return;
        }
        view.setTag(assetPath);
        Bitmap cached = cached(assetPath, requestedEdge);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageDrawable(null);
        final Context context = view.getContext().getApplicationContext();
        DISK.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decode(context, assetPath, requestedEdge);
                if (bitmap == null) {
                    return;
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (assetPath.equals(view.getTag())) {
                            view.setImageBitmap(bitmap);
                        }
                    }
                });
            }
        });
    }

    /**
     * Loads a picture the user picked themselves (the wallpaper editor's custom background). Still a
     * local file and still decoded off the main thread with a sample size, but never cached: the user may
     * replace the file between two visits, and a stale preview is worse than a re-decode.
     */
    public static void fromFile(final ImageView view, final File file, final int requestedEdge) {
        if (view == null || file == null) {
            return;
        }
        final String tag = file.getAbsolutePath();
        view.setTag(tag);
        DISK.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = decodeFile(file, requestedEdge);
                if (bitmap == null) {
                    return;
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (tag.equals(view.getTag())) {
                            view.setImageBitmap(bitmap);
                        }
                    }
                });
            }
        });
    }

    private static Bitmap decodeFile(File file, int requestedEdge) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, requestedEdge);
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Synchronous decode for one-shot screens (the wallpaper preview). */
    public static Bitmap load(Context context, String assetPath, int requestedEdge) {
        Bitmap cached = cached(assetPath, requestedEdge);
        return cached != null ? cached : decode(context.getApplicationContext(), assetPath, requestedEdge);
    }

    private static Bitmap cached(String assetPath, int requestedEdge) {
        return CACHE.get(key(assetPath, requestedEdge));
    }

    private static String key(String assetPath, int requestedEdge) {
        return assetPath + "@" + requestedEdge;
    }

    private static Bitmap decode(Context context, String assetPath, int requestedEdge) {
        if (assetPath == null || assetPath.isEmpty()) {
            return null;
        }
        String cacheKey = key(assetPath, requestedEdge);
        Bitmap existing = CACHE.get(cacheKey);
        if (existing != null && !existing.isRecycled()) {
            return existing;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream stream = context.getAssets().open(assetPath)) {
                BitmapFactory.decodeStream(stream, null, bounds);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, requestedEdge);
            try (InputStream stream = context.getAssets().open(assetPath)) {
                Bitmap decoded = BitmapFactory.decodeStream(stream, null, options);
                if (decoded != null) {
                    CACHE.put(cacheKey, decoded);
                }
                return decoded;
            }
        } catch (Exception ignored) {
            // Missing or unreadable asset: the caller keeps its placeholder.
            return null;
        }
    }

    private static int sampleSize(int width, int height, int requestedEdge) {
        if (width <= 0 || height <= 0 || requestedEdge <= 0) {
            return 1;
        }
        int sample = 1;
        while (Math.max(width, height) / (sample * 2) >= requestedEdge) {
            sample *= 2;
        }
        return sample;
    }

    /**
     * Copies a bundled wallpaper into {@code destination} so the live wallpaper service and the share
     * sheet can read a real file. Already-copied files are reused, which is what makes "set as
     * wallpaper" work with the network switched off.
     */
    public static File copyToCache(Context context, String assetPath, File directory) {
        String name = assetPath.substring(assetPath.lastIndexOf('/') + 1);
        File target = new File(directory, name);
        if (target.exists() && target.length() > 0) {
            return target;
        }
        try {
            directory.mkdirs();
            try (InputStream input = context.getAssets().open(assetPath);
                 OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            return target;
        } catch (Exception ignored) {
            return target.exists() ? target : null;
        }
    }

    /** Raw byte length of an asset, used to decide whether a copy is already complete. */
    public static long assetLength(Context context, String assetPath) {
        try (AssetFileDescriptor descriptor = context.getAssets().openFd(assetPath)) {
            return descriptor.getLength();
        } catch (Exception ignored) {
            return -1L;
        }
    }
}
