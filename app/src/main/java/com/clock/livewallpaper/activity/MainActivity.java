package com.clock.livewallpaper.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdPolicy;
import com.clock.livewallpaper.ads.AdsManager;
import com.clock.livewallpaper.ads.NativePlacement;

/**
 * Home screen: the four section buttons (Clocks, Names of Allah, Quran, Azkar) plus share / rate.
 *
 * <p>The bottom banner that used to live here is gone. The single ad allowed on this screen is one
 * in-feed native card below the buttons, clearly separated from them by margins and by its own paper
 * card with an "إعلان" badge; when no ad is available the container is empty and collapses, so the
 * screen looks exactly like an ad-free one.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Kept between configuration changes so a rotation does not burn a second ad request (the SDK
     * refuses another one for a minute anyway, which would leave the slot empty for no reason).
     */
    private static NativePlacement sHomePlacement;

    private NativePlacement homeAd;
    private FrameLayout homeAdSlot;
    private ImageView rate;
    private ImageView share;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_select_function);
        initView();
        setupHomeAd();
        AdsManager.get().initialize(this);
    }

    private void initView() {
        FrameLayout frameClock = (FrameLayout) findViewById(R.id.frameClock);
        FrameLayout frameNamesAllah = (FrameLayout) findViewById(R.id.frameNamesAllah);
        FrameLayout frameQuran = (FrameLayout) findViewById(R.id.frameQuran);
        FrameLayout frameAzkar = (FrameLayout) findViewById(R.id.frameAzkar);
        this.rate = findViewById(R.id.rateus);
        this.share = findViewById(R.id.share);

        this.rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AdPolicy.markSystemHandoff();
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + getPackageName())));
                } catch (ActivityNotFoundException unused) {
                    Toast.makeText(MainActivity.this, " unable to find market app",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        this.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name)
                        + "\n\nOpen this Link on Play Store\n\nhttps://play.google.com/store/apps/details?id="
                        + getPackageName());
                AdPolicy.markSystemHandoff();
                startActivity(Intent.createChooser(intent, getString(R.string.share)));
            }
        });

        frameClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ClockFuntionActivity.class));
            }
        });
        frameNamesAllah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NamesOfAllahActivity.class));
            }
        });
        // Quran icon: open the offline Surah index, which launches QuranActivity with a surah_id.
        frameQuran.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SurahListActivity.class));
            }
        });
        // Azkar icon: standalone entry point, independent of the Quran section.
        frameAzkar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AzkarHomeActivity.class));
            }
        });
    }

    /** Renders the one home ad slot, or leaves it empty. Requests are throttled inside the placement. */
    private void setupHomeAd() {
        this.homeAdSlot = (FrameLayout) findViewById(R.id.homeNativeAd);
        if (sHomePlacement == null) {
            sHomePlacement = new NativePlacement();
        }
        this.homeAd = sHomePlacement;
        this.homeAd.setDatasetChangedListener(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.homeAdSlot.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.homeAd.renderInto(MainActivity.this.homeAdSlot);
                    }
                });
            }
        });
        this.homeAd.renderInto(this.homeAdSlot);
    }

    @Override
    protected void onDestroy() {
        if (this.homeAd != null) {
            this.homeAd.setDatasetChangedListener(null);
            this.homeAd.release(this.homeAdSlot);
            if (isFinishing()) {
                this.homeAd.destroy();
                sHomePlacement = null;
            }
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
