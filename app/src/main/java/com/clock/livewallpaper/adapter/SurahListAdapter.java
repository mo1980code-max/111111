package com.clock.livewallpaper.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.QuranThemeColors;
import com.clock.livewallpaper.quran.SurahIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the Surah Index screen: all 114 surahs, in Mushaf order, straight from the Java arrays
 * in {@link SurahIndex}.
 *
 * <p>Rows are filtered live by the index screen's SearchView through {@link Filterable}: a query
 * matches the Arabic name, the transliteration or the English meaning, case-insensitively for Latin
 * text. An empty query always restores all 114 rows.
 *
 * <p>Row colours are read from {@link QuranThemeColors} at bind time, so the light/dark toggle only
 * needs a rebind, and stable ids stay enabled — RecyclerView can reuse view holders across rotation
 * instead of rebinding all 114 rows from scratch.
 *
 * <p>Each row reports the tapped {@link SurahIndex.Surah} through {@link OnSurahClickListener}; the
 * adapter deliberately knows nothing about Intents or activities, which keeps it testable and
 * reusable.
 */
public final class SurahListAdapter extends RecyclerView.Adapter<SurahListAdapter.SurahViewHolder>
        implements Filterable {

    /** Notified when the user taps a surah row. */
    public interface OnSurahClickListener {
        /** @param surah the tapped surah, never {@code null} */
        void onSurahClick(SurahIndex.Surah surah);
    }

    private final List<SurahIndex.Surah> allSurahs;
    /** What is currently on screen; replaced wholesale by every completed filter pass. */
    private final List<SurahIndex.Surah> visibleSurahs;
    private final OnSurahClickListener listener;
    private final Filter filter = new SurahFilter();

    public SurahListAdapter(@NonNull OnSurahClickListener listener) {
        this.allSurahs = SurahIndex.all();
        this.visibleSurahs = new ArrayList<>(allSurahs);
        this.listener = listener;
        // Must be set before the adapter is attached to a RecyclerView.
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public SurahViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_surah, parent, false);
        return new SurahViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull SurahViewHolder holder, int position) {
        holder.bind(visibleSurahs.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return visibleSurahs.size();
    }

    @Override
    public long getItemId(int position) {
        // Stable, unique and meaningful: the 1-based surah number.
        return visibleSurahs.get(position).id;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    /** Filters surah rows by Arabic name, transliteration or English meaning. */
    private final class SurahFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String query = constraint == null ? "" : constraint.toString().trim();
            List<SurahIndex.Surah> matches;
            if (TextUtils.isEmpty(query)) {
                matches = allSurahs;
            } else {
                String lowered = query.toLowerCase(Locale.ROOT);
                matches = new ArrayList<>();
                for (int i = 0; i < allSurahs.size(); i++) {
                    SurahIndex.Surah surah = allSurahs.get(i);
                    if (surah.arabicName.contains(query)
                            || surah.englishName.toLowerCase(Locale.ROOT).contains(lowered)
                            || surah.englishMeaning.toLowerCase(Locale.ROOT).contains(lowered)) {
                        matches.add(surah);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = matches;
            results.count = matches.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            visibleSurahs.clear();
            visibleSurahs.addAll((List<SurahIndex.Surah>) results.values);
            notifyDataSetChanged();
        }
    }

    /**
     * One bound row of the index.
     *
     * <p>Public because it appears in this public class's {@code extends} clause; a package-private
     * type there compiles but cannot be named by a caller outside the package.
     */
    public static final class SurahViewHolder extends RecyclerView.ViewHolder {

        private final TextView numberView;
        private final TextView englishNameView;
        private final TextView englishMeaningView;
        private final TextView metaView;
        private final TextView arabicNameView;

        SurahViewHolder(@NonNull View row) {
            super(row);
            numberView = row.findViewById(R.id.surah_number);
            englishNameView = row.findViewById(R.id.surah_english_name);
            englishMeaningView = row.findViewById(R.id.surah_english_meaning);
            metaView = row.findViewById(R.id.surah_meta);
            arabicNameView = row.findViewById(R.id.surah_arabic_name);
        }

        void bind(final SurahIndex.Surah surah, final OnSurahClickListener listener) {
            String revelationPlace = itemView.getContext().getString(
                    surah.medinan ? R.string.quran_madinah : R.string.quran_makkah);

            numberView.setText(String.format(Locale.US, "%d", surah.id));
            englishNameView.setText(surah.englishName);
            englishMeaningView.setText(surah.englishMeaning);
            metaView.setText(itemView.getContext().getString(
                    R.string.quran_row_meta, surah.ayahCount, revelationPlace, surah.firstJuz));
            arabicNameView.setText(surah.arabicName);

            applyThemeColors();

            // TalkBack reads the row as one phrase instead of five disconnected labels.
            itemView.setContentDescription(itemView.getContext().getString(
                    R.string.quran_row_a11y, surah.id, surah.englishName, surah.arabicName,
                    surah.ayahCount, revelationPlace));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onSurahClick(surah);
                }
            });
        }

        /** Reads the active palette so both themes render from the same code path. */
        private void applyThemeColors() {
            android.content.Context context = itemView.getContext();
            float density = context.getResources().getDisplayMetrics().density;

            int text = QuranThemeColors.text(context);
            int muted = QuranThemeColors.header(context);
            englishNameView.setTextColor(text);
            arabicNameView.setTextColor(text);
            englishMeaningView.setTextColor(muted);
            metaView.setTextColor(muted);
            numberView.setTextColor(muted);

            // Number badge: rounded fill + hairline stroke in the active palette.
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(QuranThemeColors.badge(context));
            badge.setCornerRadius(12 * density);
            badge.setStroke(Math.max(1, (int) density), QuranThemeColors.divider(context));
            numberView.setBackground(badge);

            // Touch feedback clipped to the row's rounded bounds, tinted per palette.
            GradientDrawable mask = new GradientDrawable();
            mask.setColor(Color.WHITE);
            mask.setCornerRadius(14 * density);
            itemView.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(QuranThemeColors.highlight(context)), null, mask));
        }
    }
}
