package com.clock.livewallpaper.activity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.adapter.AzkarAdapter;
import com.clock.livewallpaper.azkar.AzkarCategory;
import com.clock.livewallpaper.azkar.AzkarFontStore;
import com.clock.livewallpaper.azkar.AzkarFonts;
import com.clock.livewallpaper.azkar.AzkarItem;
import com.clock.livewallpaper.azkar.AzkarRepository;
import com.clock.livewallpaper.azkar.AzkarSettingsSheet;
import com.clock.livewallpaper.utils.EdgeToEdgeInsets;

import java.util.List;

/**
 * Azkar category screen: the exact Azkar of one section (Morning / Evening / Tasbeeh), each in
 * its own card with its own countdown counter starting from its own {@code repeatCount}.
 *
 * <h2>Entry contract</h2>
 * <pre>
 * Intent intent = new Intent(this, AzkarListActivity.class);
 * intent.putExtra(AzkarListActivity.EXTRA_CATEGORY, "morning"); // morning | evening | tasbeeh
 * startActivity(intent);
 * </pre>
 *
 * <h2>State contract (read carefully)</h2>
 * <ul>
 *   <li>العدّادات <b>لا تُحفظ إطلاقاً</b>: عند كل فتح للشاشة تُبنى عناصر جديدة من
 *       {@link AzkarRepository#items} فيبدأ كل عدّاد من عدده الأصلي (مثلاً 3) حتى لو كان
 *       قد وصل للصفر من قبل. لا قراءة ولا كتابة لأي عدّاد في التخزين.</li>
 *   <li>المحفوظ فقط هو <b>إعدادات العرض</b> ({@link AzkarFontStore}): حجم خط الذكر، ونوع
 *       الخط العربي، والوضع الليلي الخاص بقسم الأذكار. تُقرأ في {@link #onCreate} وتُطبَّق
 *       فوراً، وأي تغيير من ورقة الإعدادات يُحفَظ ويُطبَّق لحظياً دون مساس بأي عدّاد.</li>
 * </ul>
 *
 * <p>The header shows overall progress ("7 / 31 Completed" with a progress bar) computed from
 * the actual live items — the total is never hard-coded. When every item is complete the
 * header switches to the emerald "All Azkar Completed ✓" state. A tap that completes an
 * item shows a short "تم" toast; the counter itself is never reset except by reopening
 * the screen.
 */
public final class AzkarListActivity extends AppCompatActivity
        implements AzkarAdapter.OnProgressChangeListener {

    /** Category key extra: {@code "morning"}, {@code "evening"} or {@code "tasbeeh"}. */
    public static final String EXTRA_CATEGORY = "azkar_category";

    private View rootView;
    private ImageButton backButton;
    private ImageButton settingsButton;
    private TextView titleView;
    private TextView subtitleView;
    private TextView progressText;
    private ProgressBar progressBar;
    private TextView allDoneView;
    private TextView emptyView;
    private TextView sourceView;
    private View dividerTop;
    private View dividerBottom;
    private AzkarAdapter adapter;
    private AzkarFontStore fontStore;

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

        rootView = findViewById(R.id.azkar_list_root);
        backButton = findViewById(R.id.azkar_list_back);
        settingsButton = findViewById(R.id.azkar_list_settings);
        titleView = findViewById(R.id.azkar_list_title);
        subtitleView = findViewById(R.id.azkar_list_subtitle);
        sourceView = findViewById(R.id.azkar_list_source);
        emptyView = findViewById(R.id.azkar_list_empty);
        dividerTop = findViewById(R.id.azkar_list_divider_top);
        dividerBottom = findViewById(R.id.azkar_list_divider_bottom);

        backButton.setOnClickListener(v -> finish());
        settingsButton.setOnClickListener(v ->
                AzkarSettingsSheet.show(this, this::applyDisplaySettings));

        titleView.setText(category.titleRes());
        subtitleView.setText(category.subtitleRes());
        sourceView.setText(getString(R.string.azkar_source_note) + "\n" + category.sourceUrl());

        // عند الفتح: عناصر جديدة بعدّادات جديدة (كل واحد يبدأ من عدده الأصلي).
        // لا يُحمَّل أي عدّاد من التخزين هنا أبداً.
        AzkarRepository repository = AzkarRepository.get(this);
        List<AzkarItem> items = repository.items(category);

        // ما يُقرأ من التخزين عند الفتح: إعدادات العرض فقط (الحجم، الخط، الليلي).
        fontStore = AzkarFontStore.get(this);
        float fontSp = fontStore.getSp();

        progressText = findViewById(R.id.azkar_list_progress_text);
        progressBar = findViewById(R.id.azkar_list_progress_bar);
        allDoneView = findViewById(R.id.azkar_list_all_done);

        RecyclerView recycler = findViewById(R.id.azkar_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AzkarAdapter(items, fontSp,
                AzkarFonts.typefaceFor(this, fontStore.getFontFamily()),
                fontStore.isNightMode(), this);
        recycler.setAdapter(adapter);

        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);

        applyNightToChrome(fontStore.isNightMode());
        updateHeader();
    }

    @Override
    public void onProgressChanged() {
        updateHeader();
    }

    @Override
    public void onItemCompleted(@NonNull AzkarItem item) {
        // رسالة "تم" مؤقتة عند اكتمال العدّاد؛ العدّاد نفسه لا يعيد نفسه إلا بإعادة فتح الشاشة.
        Toast.makeText(this, R.string.azkar_done_toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * Re-reads the display settings and repaints everything at once (cards + chrome).
     * Called after every change made in the settings sheet. Counters are untouched:
     * repaints never reset any count.
     */
    private void applyDisplaySettings() {
        if (adapter != null) {
            adapter.setFontSize(fontStore.getSp());
            adapter.setTypeface(AzkarFonts.typefaceFor(this, fontStore.getFontFamily()));
            adapter.setNightMode(fontStore.isNightMode());
        }
        applyNightToChrome(fontStore.isNightMode());
    }

    /**
     * Paints the screen chrome (background, header, progress, dividers, footer, icons)
     * in the day or night palette. Night mode lives only in the Azkar section: with it
     * off, every pixel matches the original design.
     */
    private void applyNightToChrome(boolean night) {
        int background = ContextCompat.getColor(this,
                night ? R.color.azkar_night_background : R.color.azkar_background);
        int text = ContextCompat.getColor(this,
                night ? R.color.azkar_night_text : R.color.azkar_text);
        int muted = ContextCompat.getColor(this,
                night ? R.color.azkar_night_muted : R.color.azkar_muted);
        int accent = ContextCompat.getColor(this,
                night ? R.color.azkar_night_primary : R.color.azkar_primary);
        int divider = ContextCompat.getColor(this,
                night ? R.color.azkar_night_divider : R.color.azkar_divider);

        rootView.setBackgroundColor(background);
        EdgeToEdgeInsets.apply(this, rootView, background);
        titleView.setTextColor(text);
        subtitleView.setTextColor(muted);
        progressText.setTextColor(text);
        allDoneView.setTextColor(accent);
        emptyView.setTextColor(muted);
        sourceView.setTextColor(muted);
        dividerTop.setBackgroundColor(divider);
        dividerBottom.setBackgroundColor(divider);
        backButton.setImageTintList(ColorStateList.valueOf(text));
        settingsButton.setImageTintList(ColorStateList.valueOf(muted));
        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, night
                ? R.drawable.azkar_progress_night
                : R.drawable.azkar_progress));
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
