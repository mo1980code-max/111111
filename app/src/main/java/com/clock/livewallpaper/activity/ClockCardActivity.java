package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.graphics.Color;
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
import com.clock.livewallpaper.adapter.CustomAdapter;
import com.clock.livewallpaper.adapter.SmartTextAdapter;
import com.clock.livewallpaper.adapter.TextAdapter;
import com.clock.livewallpaper.catalog.ContentAccess;
import com.clock.livewallpaper.catalog.Unlockable;
import com.clock.livewallpaper.clock.ClockPreferences;
import com.clock.livewallpaper.model.Clocks;
import com.clock.livewallpaper.model.SmartClocks;
import com.clock.livewallpaper.model.TextClocks;
import com.clock.livewallpaper.utils.GetClocks;
import com.clock.livewallpaper.utils.TinyDB;

/**
 * The grid of one clock section: Analog (0), Digital (1) or Smart (2), chosen by {@code isWhich}.
 *
 * <p>What this class guarantees, per the ad rules of the app:
 * <ul>
 *   <li>entering a section never shows an ad -- no banner, no full-screen ad on {@code onCreate}. The
 *       only ads are native cards inserted every {@code NATIVE_AD_INTERVAL} items by
 *       {@link AdInsertingAdapter}, and they disappear by themselves on a short list;</li>
 *   <li>free clocks (the first {@code FREE_CLOCKS_PER_SECTION} of the section) open immediately; a
 *       locked clock opens the "watch an ad to unlock this clock" dialog first, and the rewarded ad is
 *       only ever requested from that dialog;</li>
 *   <li>the editor receives the clock's <em>style</em>, not its list index: {@code textClockPosition}
 *       must keep meaning the same thing after the catalog grows;</li>
 *   <li>the editor screen itself carries no ads at all.</li>
 * </ul>
 */
public class ClockCardActivity extends AppCompatActivity {

    /** Row count of the grid; the ad rows span all columns through the wrapper's SpanSizeLookup. */
    private static final int SPAN_COUNT = 2;

    private TinyDB tinyDB;
    private TextView txtTitle;
    private TextView txtHint;
    private RecyclerView recyclerViewCategory;

    private RecyclerView.Adapter<?> rawAdapter;
    private AdInsertingAdapter listAdapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.tinyDB = new TinyDB(this);
        setContentView(R.layout.activity_clock_card);
        this.txtTitle = (TextView) findViewById(R.id.txtTitle);
        this.txtHint = (TextView) findViewById(R.id.txtHint);
        this.recyclerViewCategory = (RecyclerView) findViewById(R.id.recyclerViewCategory);
        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        int isWhich = getIntent().getIntExtra("isWhich", 0);
        if (isWhich == 1) {
            showDigitalClocks();
        } else if (isWhich == 2) {
            showSmartClocks();
        } else {
            showAnalogClocks();
        }
    }

    /**
     * A rewarded ad is worth showing only when it can actually be served; preloading it here costs no
     * user interaction and keeps the dialog snappy if a locked clock is tapped.
     */
    @Override
    protected void onResume() {
        super.onResume();
        AdsManager.get().preloadRewarded(this);
        if (this.rawAdapter != null) {
            // An unlock earned on this screen (or on the previous visit) has to be reflected at once.
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

    // ---------------------------------------------------------------------
    // The three sections
    // ---------------------------------------------------------------------

    private void showAnalogClocks() {
        this.txtTitle.setText(R.string.clock_analog_title);
        this.txtHint.setText(R.string.clock_section_hint);
        final CustomAdapter adapter = new CustomAdapter(GetClocks.analogClocks());
        adapter.setClickListener(new CustomAdapter.ClickListener() {
            @Override
            public void setClick(final Clocks clocks) {
                ContentAccess.open(ClockCardActivity.this, clocks, refreshBadges(),
                        new ContentAccess.Listener() {
                            @Override
                            public void onReady(@NonNull Unlockable item) {
                                openAnalogClock((Clocks) item);
                            }

                            @Override
                            public void onBlocked(@Nullable String message) {
                                showBlocked(message);
                            }
                        });
            }
        });
        bindList(adapter, "analog");
    }

    private void showDigitalClocks() {
        this.txtTitle.setText(R.string.clock_digital_title);
        this.txtHint.setText(R.string.clock_section_hint);
        final TextAdapter adapter = new TextAdapter(GetClocks.digitalClocks());
        adapter.setClickListener(new TextAdapter.ClickListener() {
            @Override
            public void setClick(int position, final TextClocks textClocks) {
                ContentAccess.open(ClockCardActivity.this, textClocks, refreshBadges(),
                        new ContentAccess.Listener() {
                            @Override
                            public void onReady(@NonNull Unlockable item) {
                                openTextClock((TextClocks) item);
                            }

                            @Override
                            public void onBlocked(@Nullable String message) {
                                showBlocked(message);
                            }
                        });
            }
        });
        bindList(adapter, "digital");
    }

    private void showSmartClocks() {
        this.txtTitle.setText(R.string.clock_smart_title);
        this.txtHint.setText(R.string.clock_section_hint);
        final SmartTextAdapter adapter = new SmartTextAdapter(GetClocks.smartClocks());
        adapter.setClickListener(new SmartTextAdapter.ClickListener() {
            @Override
            public void setClick(int position, final SmartClocks smartClocks) {
                ContentAccess.open(ClockCardActivity.this, smartClocks, refreshBadges(),
                        new ContentAccess.Listener() {
                            @Override
                            public void onReady(@NonNull Unlockable item) {
                                openSmartClock((SmartClocks) item);
                            }

                            @Override
                            public void onBlocked(@Nullable String message) {
                                showBlocked(message);
                            }
                        });
            }
        });
        bindList(adapter, "smart");
    }

    /** Rebinds the content rows so a fresh unlock swaps the padlock off without reopening the screen. */
    @NonNull
    private Runnable refreshBadges() {
        return new Runnable() {
            @Override
            public void run() {
                if (rawAdapter != null) {
                    rawAdapter.notifyDataSetChanged();
                }
            }
        };
    }

    /** Wraps the section adapter so in-feed native ads land between the tiles, never on top of them. */
    private void bindList(@NonNull RecyclerView.Adapter<?> raw, @NonNull final String section) {
        this.rawAdapter = raw;
        this.listAdapter = new AdInsertingAdapter(raw, new NativePlacement());
        this.recyclerViewCategory.setLayoutManager(new GridLayoutManager(this, SPAN_COUNT));
        this.recyclerViewCategory.setAdapter(this.listAdapter);

    }

    // ---------------------------------------------------------------------
    // Opening a clock. These only run for content the user is allowed to use.
    // ---------------------------------------------------------------------

    private void openAnalogClock(@NonNull Clocks clocks) {
        this.tinyDB.putObject("clocks", clocks);
        this.tinyDB.putInt("clockType", 0);
        this.tinyDB.putBoolean("isImage", false);
        this.tinyDB.putBoolean("isCustomBg", false);
        this.tinyDB.putInt("bgColor", Color.parseColor(clocks.getBgColor()));
        ClockPreferences.get(this).selectClock(clocks.getId(), 0, 0,
                Color.parseColor(clocks.getBgColor()), 0, false);
        startActivity(new Intent(this, ClockStudioActivity.class));
    }

    private void openTextClock(@NonNull TextClocks textClocks) {
        // The style index, not the list position: see the note in GetClocks.
        this.tinyDB.putInt("textClockPosition", textClocks.getStyle());
        this.tinyDB.putInt("clockType", 2);
        this.tinyDB.putBoolean("isImage", false);
        this.tinyDB.putBoolean("isCustomBg", false);
        this.tinyDB.putInt("bgColor", Color.parseColor(textClocks.getBgColor()));
        ClockPreferences.get(this).selectClock(textClocks.getId(), 2, textClocks.getStyle(),
                Color.parseColor(textClocks.getBgColor()), 0, false);
        startActivity(new Intent(this, ClockStudioActivity.class));
    }

    private void openSmartClock(@NonNull SmartClocks smartClocks) {
        this.tinyDB.putInt("customBg", smartClocks.getBgColor());
        this.tinyDB.putBoolean("isImage", false);
        this.tinyDB.putBoolean("isCustomBg", true);
        this.tinyDB.putInt("textClockPosition", smartClocks.getStyle());
        this.tinyDB.putInt("clockType", 1);
        ClockPreferences.get(this).selectClock(smartClocks.getId(), 1, smartClocks.getStyle(),
                Color.BLACK, smartClocks.getBgColor(), true);
        startActivity(new Intent(this, ClockStudioActivity.class));
    }

    /** Declined, no fill or offline: say what happened, keep the tile locked, keep using free clocks. */
    private void showBlocked(@Nullable String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
