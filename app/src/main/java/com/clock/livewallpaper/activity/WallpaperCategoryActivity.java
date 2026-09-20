package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdInsertingAdapter;
import com.clock.livewallpaper.ads.NativePlacement;
import com.clock.livewallpaper.adapter.CategoryWallpaperAdapter;
import com.clock.livewallpaper.catalog.WallpaperCatalog;
import com.clock.livewallpaper.catalog.WallpaperSection;

import java.util.List;

/**
 * The wallpaper sections grid: مساجد مكة، مساجد المدينة المنورة، المسجد الأقصى، مساجد أخرى، مآذن،
 * صور "الله" -- plus a synthetic "كل الخلفيات" first. Everything on this screen comes from
 * {@code assets/wallpapers/}, so it renders identically in airplane mode.
 *
 * <p>Opening this screen shows no ad. The list is wrapped in {@link AdInsertingAdapter}, which is the
 * only place wallpapers can get a native card; with seven rows it is below
 * {@code NATIVE_AD_MIN_ITEMS}, so nothing is requested here at all -- the short-list suppression rule
 * keeps entry points clean.
 */
public class WallpaperCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_SECTION = "section_id";

    private AdInsertingAdapter listAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_wallpaper_category);
        TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
        TextView txtHint = (TextView) findViewById(R.id.txtHint);
        txtTitle.setText(R.string.wallpaper_title);
        txtHint.setText(R.string.wallpaper_section_hint);
        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        List<WallpaperSection> sections = WallpaperCatalog.sectionsWithAll(this);
        final CategoryWallpaperAdapter raw = new CategoryWallpaperAdapter(sections);
        raw.setClickListener(new CategoryWallpaperAdapter.ClickListener() {
            @Override
            public void setClick(@NonNull WallpaperSection section) {
                Intent intent = new Intent(WallpaperCategoryActivity.this, WallpaperActivity.class);
                intent.putExtra(EXTRA_SECTION, section.getId());
                startActivity(intent);
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerViewCategory);
        recyclerView.setLayoutManager(new GridLayoutManager(this, gallerySpanCount()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(4);
        this.listAdapter = new AdInsertingAdapter(raw, new NativePlacement());
        recyclerView.setAdapter(this.listAdapter);

    }

    private int gallerySpanCount() {
        float density = getResources().getDisplayMetrics().density;
        float widthDp = getResources().getDisplayMetrics().widthPixels / density;
        return Math.max(2, Math.min(4, (int) (widthDp / 170f)));
    }

    @Override
    protected void onDestroy() {
        if (this.listAdapter != null) {
            this.listAdapter.destroy();
            this.listAdapter = null;
        }
        super.onDestroy();
    }
}
