package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.azkar.AzkarCategory;
import com.clock.livewallpaper.azkar.AzkarFontStore;
import com.clock.livewallpaper.azkar.AzkarRepository;
import com.clock.livewallpaper.azkar.AzkarSettingsSheet;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

/**
 * Azkar home screen: the standalone entry point opened from the home-screen Azkar icon.
 *
 * <p>Shows exactly three sections — Morning Azkar, Evening Azkar, Tasbeeh — as identical cards
 * with their icon, titles and "X / N Completed" progress. Tapping a card opens
 * {@link AzkarListActivity} with the matching category. Counters are session-only (they
 * restart from their original numbers on every open and are never stored), so the home
 * cards always show 0 / N; live progress is visible inside the open category screen.
 *
 * <p>The header settings button opens the same display-settings sheet as the category
 * screen (text size, font family, Azkar-only night mode). Display settings repaint in
 * {@link #onResume()} as well, so a change made inside a category is visible on return.
 */
public final class AzkarHomeActivity extends AppCompatActivity {

    private AzkarRepository repository;
    private AzkarFontStore fontStore;

    private View rootView;
    private ImageButton backButton;
    private ImageButton settingsButton;
    private TextView titleView;
    private TextView subtitleView;
    private View dividerView;

    private View morningCard;
    private View eveningCard;
    private View tasbeehCard;
    private TextView morningName;
    private TextView eveningName;
    private TextView tasbeehName;
    private TextView morningSub;
    private TextView eveningSub;
    private TextView tasbeehSub;

    private TextView morningProgress;
    private ProgressBar morningBar;
    private TextView eveningProgress;
    private ProgressBar eveningBar;
    private TextView tasbeehProgress;
    private ProgressBar tasbeehBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_azkar_home);
        setTitle(R.string.azkar_title);

        repository = AzkarRepository.get(this);
        fontStore = AzkarFontStore.get(this);

        rootView = findViewById(R.id.azkar_home_root);
        backButton = findViewById(R.id.azkar_home_back);
        settingsButton = findViewById(R.id.azkar_home_settings);
        titleView = findViewById(R.id.azkar_home_title);
        subtitleView = findViewById(R.id.azkar_home_subtitle);
        dividerView = findViewById(R.id.azkar_home_divider);

        backButton.setOnClickListener(v -> finish());
        settingsButton.setOnClickListener(v ->
                AzkarSettingsSheet.show(this, this::applyDisplaySettings));

        morningCard = findViewById(R.id.azkar_card_morning);
        eveningCard = findViewById(R.id.azkar_card_evening);
        tasbeehCard = findViewById(R.id.azkar_card_tasbeeh);
        morningName = findViewById(R.id.azkar_home_name_morning);
        eveningName = findViewById(R.id.azkar_home_name_evening);
        tasbeehName = findViewById(R.id.azkar_home_name_tasbeeh);
        morningSub = findViewById(R.id.azkar_home_sub_morning);
        eveningSub = findViewById(R.id.azkar_home_sub_evening);
        tasbeehSub = findViewById(R.id.azkar_home_sub_tasbeeh);

        morningProgress = findViewById(R.id.azkar_home_progress_morning);
        morningBar = findViewById(R.id.azkar_home_bar_morning);
        eveningProgress = findViewById(R.id.azkar_home_progress_evening);
        eveningBar = findViewById(R.id.azkar_home_bar_evening);
        tasbeehProgress = findViewById(R.id.azkar_home_progress_tasbeeh);
        tasbeehBar = findViewById(R.id.azkar_home_bar_tasbeeh);

        morningCard.setOnClickListener(v -> openCategory(AzkarCategory.MORNING));
        eveningCard.setOnClickListener(v -> openCategory(AzkarCategory.EVENING));
        tasbeehCard.setOnClickListener(v -> openCategory(AzkarCategory.TASBEEH));

        applyDisplaySettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProgress();
        // A display change made inside a category (e.g. night mode) shows on return.
        applyDisplaySettings();
    }

    private void openCategory(AzkarCategory category) {
        Intent intent = new Intent(this, AzkarListActivity.class);
        intent.putExtra(AzkarListActivity.EXTRA_CATEGORY, category.key());
        startActivity(intent);
    }

    /** Repaints the three progress labels from the repository totals. */
    private void refreshProgress() {
        paintCategory(AzkarCategory.MORNING, morningProgress, morningBar);
        paintCategory(AzkarCategory.EVENING, eveningProgress, eveningBar);
        paintCategory(AzkarCategory.TASBEEH, tasbeehProgress, tasbeehBar);
    }

    private void paintCategory(AzkarCategory category, TextView label, ProgressBar bar) {
        int total = repository.totalCount(category);
        int done = repository.completedCount(category);
        label.setText(getString(R.string.azkar_progress, done, total));
        bar.setMax(Math.max(1, total));
        bar.setProgress(done);
    }

    /**
     * Re-reads the display settings and repaints the whole screen in the day or night
     * palette. Night mode lives only in the Azkar section: with it off, every pixel
     * matches the original design.
     */
    private void applyDisplaySettings() {
        boolean night = fontStore.isNightMode();
        int background = ContextCompat.getColor(this,
                night ? R.color.azkar_night_background : R.color.azkar_background);
        int text = ContextCompat.getColor(this,
                night ? R.color.azkar_night_text : R.color.azkar_text);
        int muted = ContextCompat.getColor(this,
                night ? R.color.azkar_night_muted : R.color.azkar_muted);

        rootView.setBackgroundColor(background);
        EdgeToEdgeInsets.apply(this, rootView, background);
        backButton.setImageTintList(ColorStateList.valueOf(text));
        settingsButton.setImageTintList(ColorStateList.valueOf(muted));
        titleView.setTextColor(text);
        subtitleView.setTextColor(muted);
        dividerView.setBackgroundColor(ContextCompat.getColor(this,
                night ? R.color.azkar_night_divider : R.color.azkar_divider));

        int cardBg = night ? R.drawable.bg_azkar_home_card_night : R.drawable.bg_azkar_home_card;
        morningCard.setBackgroundResource(cardBg);
        eveningCard.setBackgroundResource(cardBg);
        tasbeehCard.setBackgroundResource(cardBg);
        morningName.setTextColor(text);
        eveningName.setTextColor(text);
        tasbeehName.setTextColor(text);
        morningSub.setTextColor(muted);
        eveningSub.setTextColor(muted);
        tasbeehSub.setTextColor(muted);
        morningProgress.setTextColor(muted);
        eveningProgress.setTextColor(muted);
        tasbeehProgress.setTextColor(muted);

        int barBg = night ? R.drawable.azkar_progress_night : R.drawable.azkar_progress;
        morningBar.setProgressDrawable(ContextCompat.getDrawable(this, barBg));
        eveningBar.setProgressDrawable(ContextCompat.getDrawable(this, barBg));
        tasbeehBar.setProgressDrawable(ContextCompat.getDrawable(this, barBg));
    }
}
