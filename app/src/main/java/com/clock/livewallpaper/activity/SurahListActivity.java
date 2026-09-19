package com.clock.livewallpaper.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.adapter.SurahListAdapter;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.SurahIndex;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Surah Index screen: a {@link RecyclerView} listing all 114 surahs offline.
 *
 * <p>Rows come from the Java arrays in {@link SurahIndex}, never from the database, so the list is on
 * screen immediately on a cold start. Tapping a row hands {@code surah_id} and {@code surah_name} to
 * {@link QuranActivity} through an {@link Intent}; this activity never reads Quran text itself.
 *
 * <p>While the list paints, a single worker thread makes sure the bundled {@code quran.ar.uthmani.db}
 * is installed, so the first surah the user opens usually loads without a spinner. The install is
 * idempotent and guarded by a process-wide lock in {@link QuranDatabaseHelper}, so this pre-warm
 * cannot race with the one started from {@code AppClass.onCreate()}.
 */
public final class SurahListActivity extends Activity
        implements SurahListAdapter.OnSurahClickListener {

    private static final String TAG = "SurahListActivity";

    private final ExecutorService installer = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surah_list);
        setTitle(R.string.quran_index_title);

        View root = findViewById(R.id.surah_list_root);
        EdgeToEdgeInsets.apply(this, root, getColor(R.color.quran_background));

        ImageButton back = findViewById(R.id.surah_list_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.surah_recycler);
        // Fixed size lets RecyclerView skip a full re-measure on every layout pass.
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new SurahListAdapter(this));

        prewarmDatabase();
    }

    /** Copies the database asset out on a worker thread so the reader opens without waiting. */
    private void prewarmDatabase() {
        installer.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    new QuranDatabaseHelper(getApplicationContext()).prepareDatabase();
                } catch (Exception error) {
                    // Non-fatal: QuranActivity surfaces an actionable error if the install really failed.
                    Log.w(TAG, "Pre-warm of " + QuranDatabaseHelper.DATABASE_NAME + " failed", error);
                }
            }
        });
    }

    /**
     * Launches the reader for one surah.
     *
     * @param surah the tapped surah
     */
    @Override
    public void onSurahClick(SurahIndex.Surah surah) {
        Intent intent = new Intent(this, QuranActivity.class);
        intent.putExtra(QuranActivity.EXTRA_SURAH_ID, surah.id);
        intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, surah.arabicName);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        // Cancels a copy that is still running; a later launch simply restarts the install.
        installer.shutdownNow();
        super.onDestroy();
    }
}
