package com.clock.livewallpaper.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.adapter.AzkarAdapter;
import com.clock.livewallpaper.azkar.AzkarCategory;
import com.clock.livewallpaper.azkar.AzkarItem;
import com.clock.livewallpaper.azkar.AzkarRepository;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

import java.util.List;

/**
 * Azkar category screen: the exact Azkar of one section (Morning / Evening / Tasbeeh), each in
 * its own card with its own counter targeted at its own {@code repeatCount} from the source.
 *
 * <h2>Entry contract</h2>
 * <pre>
 * Intent intent = new Intent(this, AzkarListActivity.class);
 * intent.putExtra(AzkarListActivity.EXTRA_CATEGORY, "morning"); // morning | evening | tasbeeh
 * startActivity(intent);
 * </pre>
 *
 * <p>The header shows overall progress ("7 / 31 Completed" with a progress bar) computed from
 * the actual imported items — the total is never hard-coded. When every item is complete the
 * header switches to the emerald "All Azkar Completed ✓" state. Every tap persists immediately,
 * so progress survives leaving the page or closing the app.
 */
public final class AzkarListActivity extends AppCompatActivity
        implements AzkarAdapter.OnProgressChangeListener {

    /** Category key extra: {@code "morning"}, {@code "evening"} or {@code "tasbeeh"}. */
    public static final String EXTRA_CATEGORY = "azkar_category";

    private TextView progressText;
    private ProgressBar progressBar;
    private TextView allDoneView;
    private AzkarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_azkar_list);

        AzkarCategory category = AzkarCategory.fromKey(
                getIntent() != null ? getIntent().getStringExtra(EXTRA_CATEGORY) : null);
        if (category == null) {
            Toast.makeText(this, R.string.azkar_unknown_category, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setTitle(category.titleRes());

        View root = findViewById(R.id.azkar_list_root);
        EdgeToEdgeInsets.apply(this, root,
                ContextCompat.getColor(this, R.color.azkar_background));

        findViewById(R.id.azkar_list_back).setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.azkar_list_title)).setText(category.titleRes());
        ((TextView) findViewById(R.id.azkar_list_subtitle)).setText(category.subtitleRes());
        ((TextView) findViewById(R.id.azkar_list_source)).setText(
                getString(R.string.azkar_source_note) + "\n" + category.sourceUrl());

        AzkarRepository repository = AzkarRepository.get(this);
        List<AzkarItem> items = repository.items(category);

        progressText = findViewById(R.id.azkar_list_progress_text);
        progressBar = findViewById(R.id.azkar_list_progress_bar);
        allDoneView = findViewById(R.id.azkar_list_all_done);

        RecyclerView recycler = findViewById(R.id.azkar_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AzkarAdapter(items, repository, this);
        recycler.setAdapter(adapter);

        View emptyView = findViewById(R.id.azkar_list_empty);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);

        updateHeader();
    }

    @Override
    public void onProgressChanged() {
        updateHeader();
    }

    /** Repaints "X / N Completed", the bar and the all-done banner from the live items. */
    private void updateHeader() {
        if (adapter == null) {
            return;
        }
        List<AzkarItem> items = adapter.items();
        int total = items.size();
        int done = 0;
        for (AzkarItem item : items) {
            if (item.isCompleted()) {
                done++;
            }
        }
        progressText.setText(getString(R.string.azkar_progress, done, total));
        progressBar.setMax(Math.max(1, total));
        progressBar.setProgress(done);
        boolean allDone = total > 0 && done == total;
        allDoneView.setVisibility(allDone ? View.VISIBLE : View.GONE);
    }
}
