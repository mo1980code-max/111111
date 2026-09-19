package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.azkar.AzkarCategory;
import com.clock.livewallpaper.azkar.AzkarRepository;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

/**
 * Azkar home screen: the standalone entry point opened from the home-screen Azkar icon.
 *
 * <p>Shows exactly three sections — Morning Azkar, Evening Azkar, Tasbeeh — as identical cards
 * with their icon, titles and live "X / N Completed" progress. Tapping a card opens
 * {@link AzkarListActivity} with the matching category. Progress is refreshed in
 * {@link #onResume()} so counts made inside a category are visible on return.
 */
public final class AzkarHomeActivity extends AppCompatActivity {

    private AzkarRepository repository;

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

        View root = findViewById(R.id.azkar_home_root);
        EdgeToEdgeInsets.apply(this, root,
                ContextCompat.getColor(this, R.color.azkar_background));

        findViewById(R.id.azkar_home_back).setOnClickListener(v -> finish());

        morningProgress = findViewById(R.id.azkar_home_progress_morning);
        morningBar = findViewById(R.id.azkar_home_bar_morning);
        eveningProgress = findViewById(R.id.azkar_home_progress_evening);
        eveningBar = findViewById(R.id.azkar_home_bar_evening);
        tasbeehProgress = findViewById(R.id.azkar_home_progress_tasbeeh);
        tasbeehBar = findViewById(R.id.azkar_home_bar_tasbeeh);

        findViewById(R.id.azkar_card_morning).setOnClickListener(
                v -> openCategory(AzkarCategory.MORNING));
        findViewById(R.id.azkar_card_evening).setOnClickListener(
                v -> openCategory(AzkarCategory.EVENING));
        findViewById(R.id.azkar_card_tasbeeh).setOnClickListener(
                v -> openCategory(AzkarCategory.TASBEEH));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProgress();
    }

    private void openCategory(AzkarCategory category) {
        Intent intent = new Intent(this, AzkarListActivity.class);
        intent.putExtra(AzkarListActivity.EXTRA_CATEGORY, category.key());
        startActivity(intent);
    }

    /** Repaints the three progress labels from the persisted counters. */
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
}
