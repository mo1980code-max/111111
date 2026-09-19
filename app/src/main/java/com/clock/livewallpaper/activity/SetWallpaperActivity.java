package com.clock.livewallpaper.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.clock.livewallpaper.R;

import com.clock.livewallpaper.CustomWallpaper;
import com.clock.livewallpaper.utils.TinyDB;

import java.io.File;




public class SetWallpaperActivity extends AppCompatActivity {
    private RelativeLayout adContainer;
    private CardView cardShare;
    private ImageView imageMain;
    private AppCompatImageButton ivShare;
    private ProgressBar progressBar;
    private TextView setWallpaper;
    TinyDB tinyDB;

    @Override

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        EditorActivity.isDone = false;
        this.tinyDB = new TinyDB(this);
        getWindow().setFlags(1024, 1024);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.activity_set_wallpaper);
        initView();
    }

    @Override
    public void onBackPressed() {


        SetWallpaperActivity.this.finish();

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

    public void hideMenu() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    private void initView() {
        this.imageMain = (ImageView) findViewById(R.id.imageMain);
        this.setWallpaper = (TextView) findViewById(R.id.setWallpaper);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);
        this.cardShare = (CardView) findViewById(R.id.cardShare);
        this.ivShare = (AppCompatImageButton) findViewById(R.id.ivShare);
        this.adContainer = (RelativeLayout) findViewById(R.id.adContainer);
        final String resourceName = getIntent().getStringExtra("imageFile");
        final int resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
        final File file2 = new File(getExternalCacheDir() + File.separator + resourceName + ".png");
        final File file = file2;
        if (!file2.exists() && resourceId != 0) {
            try (java.io.InputStream input = getResources().openRawResource(resourceId); java.io.OutputStream output = new java.io.FileOutputStream(file2)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            } catch (java.io.IOException ignored) { }
        }
        if (file2.exists()) {
            this.setWallpaper.setText("Set Wallpaper");
            Glide.with((FragmentActivity) this).load(file2).into(this.imageMain);
            this.cardShare.setVisibility(View.VISIBLE);
        } else {
            Glide.with((FragmentActivity) this).load(getIntent().getStringExtra("imageFile")).placeholder((int) R.drawable.placeholder).into(this.imageMain);
            this.setWallpaper.setText("Download Wallpaper");
            this.cardShare.setVisibility(View.GONE);
        }
        this.setWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (file2.exists()) {
                    SetWallpaperActivity.this.tinyDB.putString("isWallpaper", file2.getAbsolutePath());

                    EditorActivity.isDone = true;
                    Intent intent = new Intent("android.service.wallpaper.CHANGE_LIVE_WALLPAPER");
                    intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", new ComponentName(SetWallpaperActivity.this, CustomWallpaper.class));
                    SetWallpaperActivity.this.startActivity(intent);

                    return;
                }
                return;
            }
        });
        this.ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetWallpaperActivity setWallpaperActivity = SetWallpaperActivity.this;
                Uri uriForFile = FileProvider.getUriForFile(setWallpaperActivity, SetWallpaperActivity.this.getApplicationContext().getPackageName() + ".provider", file2);
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("image/jpeg");
                intent.putExtra("android.intent.extra.STREAM", uriForFile);
                SetWallpaperActivity.this.startActivity(Intent.createChooser(intent, "Select"));
            }
        });
    }
}
