package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranDatabaseHelper;
import com.clock.livewallpaper.quran.QuranThemeColors;
import com.clock.livewallpaper.quran.SurahIndex;

import java.util.List;

/**
 * Rows of the full-text search results on the Surah Index screen: one matching ayah per row, with
 * a {@code Surah 2:255}-style reference above the Arabic excerpt.
 *
 * <p>The result set is immutable once handed to the constructor — a new adapter is built for every
 * completed query — so there is no {@code notifyDataSetChanged()} needed for data, only for theme
 * repaints. Taps are reported through {@link OnAyahResultClickListener}; the adapter never starts
 * activities itself, mirroring {@link SurahListAdapter}.
 */
public final class AyahSearchAdapter
        extends RecyclerView.Adapter<AyahSearchAdapter.ResultViewHolder> {

    /** Notified when the user taps a search result row. */
    public interface OnAyahResultClickListener {
        /** @param ayah the tapped ayah with its surah and ayah numbers, never {@code null} */
        void onAyahResultClick(QuranDatabaseHelper.Ayah ayah);
    }

    /** Uthmani script face for the Arabic excerpt; immutable once loaded from assets. */
    private static volatile Typeface cachedTypeface;
    private static final String FONT_ASSET_PATH = "fonts/quran_font.ttf";

    private final List<QuranDatabaseHelper.Ayah> results;
    private final OnAyahResultClickListener listener;

    public AyahSearchAdapter(@NonNull List<QuranDatabaseHelper.Ayah> results,
                             @NonNull OnAyahResultClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ResultViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        holder.bind(results.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    /** One bound search-result row. Public because it appears in the {@code extends} clause above. */
    public static final class ResultViewHolder extends RecyclerView.ViewHolder {

        private final TextView referenceView;
        private final TextView textView;

        ResultViewHolder(@NonNull View row) {
            super(row);
            referenceView = row.findViewById(R.id.search_result_ref);
            textView = row.findViewById(R.id.search_result_text);
            Typeface typeface = quranTypeface(row.getContext());
            if (typeface != null) {
                textView.setTypeface(typeface);
            }
        }

        void bind(final QuranDatabaseHelper.Ayah ayah, final OnAyahResultClickListener listener) {
            Context context = itemView.getContext();

            referenceView.setText(context.getString(R.string.quran_search_result_ref,
                    SurahIndex.englishName(ayah.surah), ayah.surah, ayah.number));
            textView.setText(ayah.text);

            // Theme colours are read at bind time so a palette toggle only needs a rebind.
            referenceView.setTextColor(QuranThemeColors.button(context));
            textView.setTextColor(QuranThemeColors.text(context));

            itemView.setContentDescription(context.getString(R.string.quran_search_result_ref,
                    SurahIndex.englishName(ayah.surah), ayah.surah, ayah.number));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onAyahResultClick(ayah);
                }
            });
        }

        private static Typeface quranTypeface(Context context) {
            Typeface typeface = cachedTypeface;
            if (typeface == null) {
                try {
                    typeface = Typeface.createFromAsset(context.getAssets(), FONT_ASSET_PATH);
                    cachedTypeface = typeface;
                } catch (RuntimeException ignored) {
                    // Missing face: fall back to the system font rather than crashing the list.
                    return null;
                }
            }
            return typeface;
        }
    }
}
