package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdPolicy;
import com.clock.livewallpaper.adapter.AyahSearchAdapter;
import com.clock.livewallpaper.adapter.SurahListAdapter;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.QuranSettings;
import com.clock.livewallpaper.quran.QuranThemeColors;
import com.clock.livewallpaper.quran.SurahIndex;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Surah Index screen: all 114 surahs in a {@link RecyclerView}, plus the feature's entry points
 * for the reader's advanced capabilities.
 *
 * <h2>List rows</h2>
 * Rows come from the Java arrays in {@link SurahIndex}, never from the database, so the list is on
 * screen immediately on a cold start. Tapping a row hands {@code surah_id} and {@code surah_name}
 * to {@link QuranActivity} through an {@link Intent}; this activity never reads Quran text for the
 * list itself.
 *
 * <h2>Search</h2>
 * The {@link SearchView} filters the surah list by Arabic name, transliteration or meaning while
 * the user types. Submitting the query (keyboard action or magnifier) additionally runs a full-text
 * search inside the Mushaf through {@link QuranDatabaseHelper#searchQuran(String)} on a worker
 * thread and swaps the list to the matching ayahs; tapping a result opens the reader scrolled to
 * that ayah. Clearing the query restores the surah index.
 *
 * <h2>Continue Reading</h2>
 * When the reader's bookmark icon has saved a position, a green "Continue Reading" button floats
 * over the list and jumps straight back to the saved surah and {@code scrollY}.
 *
 * <h2>Theme</h2>
 * The moon/sun icon flips the palette persisted in {@link QuranSettings}; this screen and the
 * reader paint themselves from {@link QuranThemeColors}, so both always agree.
 *
 * <p>While the list paints, a single worker thread makes sure the bundled {@code quran.ar.uthmani.db}
 * is installed, so the first surah the user opens usually loads without a spinner. The install is
 * idempotent and guarded by a process-wide lock in {@link QuranDatabaseHelper}, so this pre-warm
 * cannot race with the one started from {@code AppClass.onCreate()}.
 */
public final class SurahListActivity extends AppCompatActivity
        implements SurahListAdapter.OnSurahClickListener, AyahSearchAdapter.OnAyahResultClickListener {

    private static final String TAG = "SurahListActivity";

    private final ExecutorService installer = Executors.newSingleThreadExecutor();
    private final ExecutorService searcher = Executors.newSingleThreadExecutor();

    private QuranSettings settings;

    private View root;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton themeToggleButton;
    private SearchView searchView;
    private TextView resultCountView;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private Button continueReadingButton;
    private View dividerTop;
    private View dividerBottom;
    private TextView attributionView;

    private SurahListAdapter surahAdapter;
    private AyahSearchAdapter searchAdapter;
    /** True while the RecyclerView shows ayah search results instead of the surah index. */
    private boolean showingSearchResults;
    /** Id of the in-flight search; stale results never replace newer ones. */
    private int searchSequence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surah_list);

        // Requirement 12: the whole Quran section is ad free. The flag is raised here rather than in
        // each call site so a later screen cannot forget to check it: while it is up, the ad layer
        // refuses every format at the source, including the app open ad on a foreground return.
        AdPolicy.enterQuranScreen();

        setTitle(R.string.quran_index_title);

        settings = QuranSettings.get(this);

        root = findViewById(R.id.surah_list_root);
        EdgeToEdgeInsets.apply(this, root, QuranThemeColors.background(this));

        bindViews();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        surahAdapter = new SurahListAdapter(this);
        recyclerView.setAdapter(surahAdapter);

        wireSearchView();
        prewarmDatabase();
        applyThemeToScreen();
    }

    private void bindViews() {
        titleView = findViewById(R.id.surah_list_title);
        subtitleView = findViewById(R.id.surah_list_subtitle);
        themeToggleButton = findViewById(R.id.surah_list_theme_toggle);
        searchView = findViewById(R.id.surah_list_search);
        resultCountView = findViewById(R.id.surah_list_result_count);
        recyclerView = findViewById(R.id.surah_recycler);
        emptyView = findViewById(R.id.surah_list_empty);
        continueReadingButton = findViewById(R.id.surah_list_continue_btn);
        dividerTop = findViewById(R.id.surah_list_divider_top);
        dividerBottom = findViewById(R.id.surah_list_divider_bottom);
        attributionView = findViewById(R.id.surah_list_attribution);

        ImageButton back = findViewById(R.id.surah_list_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        themeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.toggleDarkMode();
                applyThemeToScreen();
            }
        });

        continueReadingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                continueReading();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The bookmark may have been created or moved while the reader was in front; also pick up
        // a theme the reader toggled, so returning never shows a mismatched screen.
        updateContinueReadingButton();
        if (settings.isDarkMode() != lastAppliedDark) {
            applyThemeToScreen();
        }
    }

    /** Tracks {@link #applyThemeToScreen()} so onResume can detect a toggle done in the reader. */
    private boolean lastAppliedDark;

    // ---------------------------------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------------------------------

    private void wireSearchView() {
        searchView.setQueryHint(getString(R.string.quran_search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.trim().isEmpty()) {
                    showSurahList(null);
                    return true;
                }
                runAyahSearch(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Typing always means "filter surah names"; results mode ends as soon as the
                // query changes, and an empty query restores the full index.
                showSurahList(newText);
                return true;
            }
        });
    }

    /** Swaps back to the surah index, optionally filtered by name. */
    private void showSurahList(String query) {
        showingSearchResults = false;
        searchAdapter = null;
        resultCountView.setVisibility(View.GONE);
        if (recyclerView.getAdapter() != surahAdapter) {
            recyclerView.setAdapter(surahAdapter);
        }
        surahAdapter.getFilter().filter(query == null ? "" : query);
        emptyView.setVisibility(View.GONE);
    }

    /** Full-text search inside the Mushaf on a worker thread; results replace the list. */
    private void runAyahSearch(final String query) {
        final int sequence = ++searchSequence;
        emptyView.setText(R.string.quran_loading);
        emptyView.setVisibility(View.VISIBLE);
        searcher.execute(new Runnable() {
            @Override
            public void run() {
                List<QuranDatabaseHelper.Ayah> results;
                try {
                    results = new QuranDatabaseHelper(getApplicationContext()).searchQuran(query);
                } catch (IOException | RuntimeException error) {
                    Log.e(TAG, "Full-text search failed for \"" + query + "\"", error);
                    results = java.util.Collections.emptyList();
                }
                final List<QuranDatabaseHelper.Ayah> matches = results;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (sequence != searchSequence || isFinishing()) {
                            return; // A newer query already won; never show stale matches.
                        }
                        showSearchResults(query, matches);
                    }
                });
            }
        });
    }

    private void showSearchResults(String query, List<QuranDatabaseHelper.Ayah> matches) {
        showingSearchResults = true;
        searchAdapter = new AyahSearchAdapter(matches, this);
        recyclerView.setAdapter(searchAdapter);

        if (matches.isEmpty()) {
            emptyView.setText(getString(R.string.quran_search_no_results, query));
            emptyView.setVisibility(View.VISIBLE);
            resultCountView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            resultCountView.setText(getString(R.string.quran_search_count, matches.size()));
            resultCountView.setVisibility(View.VISIBLE);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Row clicks
    // ---------------------------------------------------------------------------------------------

    /** Launches the reader for one surah. */
    @Override
    public void onSurahClick(SurahIndex.Surah surah) {
        Intent intent = new Intent(this, QuranActivity.class);
        intent.putExtra(QuranActivity.EXTRA_SURAH_ID, surah.id);
        intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, surah.arabicName);
        startActivity(intent);
    }

    /** Launches the reader scrolled to the tapped ayah. */
    @Override
    public void onAyahResultClick(QuranDatabaseHelper.Ayah ayah) {
        Intent intent = new Intent(this, QuranActivity.class);
        intent.putExtra(QuranActivity.EXTRA_SURAH_ID, ayah.surah);
        intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, SurahIndex.arabicName(ayah.surah));
        intent.putExtra(QuranActivity.EXTRA_AYAH, ayah.number);
        startActivity(intent);
    }

    // ---------------------------------------------------------------------------------------------
    // Continue Reading (bookmark)
    // ---------------------------------------------------------------------------------------------

    /** Shows the Continue Reading button only when a saved bookmark points at a valid surah. */
    private void updateContinueReadingButton() {
        if (settings.hasBookmark()) {
            continueReadingButton.setVisibility(View.VISIBLE);
        } else {
            continueReadingButton.setVisibility(View.GONE);
        }
    }

    /** Jumps to the saved {@code last_read_surah_id} at the saved {@code scrollY}. */
    private void continueReading() {
        int surahId = settings.getBookmarkSurah();
        if (!SurahIndex.isValid(surahId)) {
            continueReadingButton.setVisibility(View.GONE);
            return;
        }
        Intent intent = new Intent(this, QuranActivity.class);
        intent.putExtra(QuranActivity.EXTRA_SURAH_ID, surahId);
        intent.putExtra(QuranActivity.EXTRA_SURAH_NAME, SurahIndex.arabicName(surahId));
        intent.putExtra(QuranActivity.EXTRA_SCROLL_Y, settings.getBookmarkScrollY());
        startActivity(intent);
    }

    // ---------------------------------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------------------------------

    /** Repaints every view this activity owns; row views rebind through the adapters. */
    private void applyThemeToScreen() {
        lastAppliedDark = settings.isDarkMode();

        root.setBackgroundColor(QuranThemeColors.background(this));
        dividerTop.setBackgroundColor(QuranThemeColors.divider(this));
        dividerBottom.setBackgroundColor(QuranThemeColors.divider(this));

        titleView.setTextColor(QuranThemeColors.text(this));
        subtitleView.setTextColor(QuranThemeColors.header(this));
        attributionView.setTextColor(QuranThemeColors.header(this));
        emptyView.setTextColor(QuranThemeColors.header(this));
        resultCountView.setTextColor(QuranThemeColors.header(this));

        int chrome = QuranThemeColors.header(this);
        ImageButton back = findViewById(R.id.surah_list_back);
        back.setColorFilter(QuranThemeColors.text(this));
        themeToggleButton.setImageResource(lastAppliedDark ? R.drawable.ic_sun : R.drawable.ic_moon);
        themeToggleButton.setColorFilter(chrome);
        themeToggleButton.setContentDescription(getString(lastAppliedDark
                ? R.string.quran_cd_switch_to_light
                : R.string.quran_cd_switch_to_dark));

        applySearchFieldTheme();

        // Row text colours come from the palette inside onBindViewHolder.
        if (surahAdapter != null) {
            surahAdapter.notifyDataSetChanged();
        }
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
        }
    }

    /** Colours the search field and its inner EditText for the active palette. */
    private void applySearchFieldTheme() {
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable field = new GradientDrawable();
        field.setColor(QuranThemeColors.badge(this));
        field.setCornerRadius(24 * density);
        field.setStroke(Math.max(1, (int) density), QuranThemeColors.divider(this));
        searchView.setBackground(field);

        EditText input = findEditText(searchView);
        if (input != null) {
            input.setTextColor(QuranThemeColors.text(this));
            input.setHintTextColor(QuranThemeColors.header(this));
        }
        // The magnifier and clear glyphs are ImageViews inside the widget; tint them like the
        // rest of the chrome so they stay legible on the dark field.
        tintImageViews(searchView, QuranThemeColors.header(this));
    }

    /** Walks the SearchView tree so no library-internal id has to be hard-coded. */
    private static EditText findEditText(View view) {
        if (view instanceof EditText) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findEditText(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void tintImageViews(View view, int color) {
        if (view instanceof android.widget.ImageView) {
            ((android.widget.ImageView) view).setColorFilter(color);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintImageViews(group.getChildAt(i), color);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Database pre-warm
    // ---------------------------------------------------------------------------------------------

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

    @Override
    protected void onDestroy() {
        AdPolicy.exitQuranScreen();
        // Cancels a copy or search that is still running; a later launch simply restarts the work.
        installer.shutdownNow();
        searcher.shutdownNow();
        super.onDestroy();
    }

}
