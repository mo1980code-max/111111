package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdInsertingAdapter;
import com.clock.livewallpaper.ads.AdsManager;
import com.clock.livewallpaper.ads.NativePlacement;
import com.clock.livewallpaper.adapter.WallpaperAdapter;
import com.clock.livewallpaper.catalog.ContentAccess;
import com.clock.livewallpaper.catalog.Unlockable;
import com.clock.livewallpaper.catalog.WallpaperCatalog;
import com.clock.livewallpaper.catalog.WallpaperEntry;

import java.util.List;

/**
 * The images of one wallpaper section.
 *
 * <p>The three first images of every section are free and are applied immediately; the rest carry a
 * padlock and answer a tap with the "watch an ad to unlock this wallpaper" dialog. Nothing is loaded
 * or shown as an ad when this screen opens -- the only ads are the native cards the wrapper inserts
 * every {@code NATIVE_AD_INTERVAL} items, and they vanish on their own when the list is short (which
 * is the case for every section, each holding six images).
 */
public class WallpaperActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET = "asset_path";
    public static final String EXTRA_TITLE = "asset_title";

    private RecyclerView.Adapter<?> rawAdapter;
    private AdInsertingAdapter listAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_wallpaper);
        String sectionId = getIntent().getStringExtra(WallpaperCategoryActivity.EXTRA_SECTION);
        if (sectionId == null) {
            sectionId = WallpaperCatalog.SECTION_ALL;
        }
        TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
        TextView txtHint = (TextView) findViewById(R.id.txtHint);
        txtTitle.setText(WallpaperCatalog.title(this, sectionId));
        txtHint.setText(R.string.wallpaper_section_hint);
        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        List<WallpaperEntry> entries = WallpaperCatalog.items(this, sectionId);
        final WallpaperAdapter raw = new WallpaperAdapter(entries);
        this.rawAdapter = raw;
        raw.setClickListener(new WallpaperAdapter.ClickListener() {
            @Override
            public void setClick(int position, final WallpaperEntry entry) {
                ContentAccess.open(WallpaperActivity.this, entry, new Runnable() {
                    @Override
                    public void run() {
                        raw.notifyDataSetChanged();
                    }
                }, new ContentAccess.Listener() {
                    @Override
                    public void onReady(@NonNull Unlockable item) {
                        open((WallpaperEntry) item);
                    }

                    @Override
                    public void onBlocked(@Nullable String message) {
                        if (message != null) {
                            Toast.makeText(WallpaperActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerViewCategory);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        this.listAdapter = new AdInsertingAdapter(raw, new NativePlacement());
        recyclerView.setAdapter(this.listAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdsManager.get().preloadRewarded(this);
        if (this.rawAdapter != null) {
            this.rawAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        if (this.listAdapter != null) {
            this.listAdapter.destroy();
            this.listAdapter = null;
        }
        this.rawAdapter = null;
        super.onDestroy();
    }

    /**
     * The preview screen is reached only for content the user may use, so it never needs to know about
     * locks or ads -- and it shows none.
     */
    private void open(@NonNull WallpaperEntry entry) {
        Intent intent = new Intent(this, SetWallpaperActivity.class);
        intent.putExtra(EXTRA_ASSET, entry.getAssetPath());
        intent.putExtra(EXTRA_TITLE, entry.getTitle());
        startActivity(intent);
    }
}
