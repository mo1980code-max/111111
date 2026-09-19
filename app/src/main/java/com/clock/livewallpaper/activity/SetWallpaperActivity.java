package com.clock.livewallpaper.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.clock.livewallpaper.CustomWallpaper;
import com.clock.livewallpaper.ads.AdPolicy;
import com.clock.livewallpaper.R;
import com.clock.livewallpaper.image.LocalImage;
import com.clock.livewallpaper.utils.TinyDB;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-size preview of one bundled wallpaper, with "set as live wallpaper" and "share".
 *
 * <p>The image already ships in the APK, so the old download step is gone: the file is only copied from
 * {@code assets/} into the app's external cache, because the live wallpaper service and the share sheet
 * both need a real path. That copy is what the spinner measures, it never touches the network, and both
 * buttons stay disabled until it is done. There is no ad on this screen: an ad may never sit next to the
 * set / download / share controls.
 */
public class SetWallpaperActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService copyExecutor = Executors.newSingleThreadExecutor();

    private TinyDB tinyDB;
    private ImageView imageMain;
    private TextView setWallpaper;
    private ProgressBar progressBar;
    private CardView cardShare;
    private AppCompatImageButton ivShare;

    @Nullable
    private volatile File cachedFile;
    private volatile boolean copying;
    private String assetPath;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        EditorActivity.isDone = false;
        this.tinyDB = new TinyDB(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.activity_set_wallpaper);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideMenu();
    }

    @Override
    protected void onDestroy() {
        this.copyExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void hideMenu() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    private void initView() {
        this.imageMain = (ImageView) findViewById(R.id.imageMain);
        this.setWallpaper = (TextView) findViewById(R.id.setWallpaper);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);
        this.cardShare = (CardView) findViewById(R.id.cardShare);
        this.ivShare = (AppCompatImageButton) findViewById(R.id.ivShare);

        this.assetPath = getIntent().getStringExtra(WallpaperActivity.EXTRA_ASSET);
        String title = getIntent().getStringExtra(WallpaperActivity.EXTRA_TITLE);
        if (title != null && !title.isEmpty()) {
            setTitle(title);
        }
        if (this.assetPath == null) {
            this.assetPath = "";
        }

        // Preview straight from the asset, decoded down to the screen height, so a 1350x2400 JPEG is
        // never held in memory at full size.
        int edge = getResources().getDisplayMetrics().heightPixels;
        LocalImage.into(this.imageMain, this.assetPath, edge);

        this.setWallpaper.setText(R.string.wallpaper_set);
        startCopy();

        this.setWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyAsWallpaper();
            }
        });
        this.ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                share();
            }
        });
    }

    /** Copies the bundled image into the external cache once; a previous copy is reused as is. */
    private void startCopy() {
        final File existing = cachedCopy();
        if (existing != null) {
            onCopied(existing);
            return;
        }
        if (this.copying || this.assetPath.isEmpty()) {
            return;
        }
        this.copying = true;
        this.progressBar.setVisibility(View.VISIBLE);
        this.copyExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final File copied = LocalImage.copyToCache(SetWallpaperActivity.this,
                        SetWallpaperActivity.this.assetPath, getExternalCacheDir());
                SetWallpaperActivity.this.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        SetWallpaperActivity.this.copying = false;
                        if (copied == null) {
                            SetWallpaperActivity.this.progressBar.setVisibility(View.GONE);
                            Toast.makeText(SetWallpaperActivity.this, R.string.wallpaper_missing,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        onCopied(copied);
                    }
                });
            }
        });
    }

    private void onCopied(@NonNull File file) {
        this.cachedFile = file;
        this.progressBar.setVisibility(View.GONE);
        this.cardShare.setVisibility(View.VISIBLE);
    }

    @Nullable
    private File cachedCopy() {
        File already = cachedFile;
        if (already != null && already.exists() && already.length() > 0) {
            return already;
        }
        File inCache = new File(getExternalCacheDir(), assetName());
        return inCache.exists() && inCache.length() > 0 ? inCache : null;
    }

    private String assetName() {
        return this.assetPath.substring(this.assetPath.lastIndexOf('/') + 1);
    }

    /**
     * Hands the file to the live wallpaper service. The path is stored so {@code CustomWallpaper} paints
     * the same image on the home screen without reading it from assets again.
     */
    private void applyAsWallpaper() {
        File file = cachedCopy();
        if (file == null) {
            // Still copying (or the asset is missing): retry the copy, do not pretend it worked.
            startCopy();
            Toast.makeText(this, R.string.ad_preparing, Toast.LENGTH_SHORT).show();
            return;
        }
        this.tinyDB.putString("isWallpaper", file.getAbsolutePath());
        EditorActivity.isDone = true;
        // The system wallpaper picker is the user's next step, not an ad slot.
        AdPolicy.markSystemHandoff();
        Intent intent = new Intent("android.service.wallpaper.CHANGE_LIVE_WALLPAPER");
        intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT",
                new ComponentName(this, CustomWallpaper.class));
        startActivity(intent);
    }

    private void share() {
        File file = cachedCopy();
        if (file == null) {
            startCopy();
            Toast.makeText(this, R.string.ad_preparing, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        AdPolicy.markSystemHandoff();
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }
}
