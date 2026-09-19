package com.clock.livewallpaper.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.quran.SurahIndex;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the Surah Index screen: all 114 surahs, in Mushaf order, straight from the Java arrays
 * in {@link SurahIndex}.
 *
 * <p>The data set is fixed at construction, so there is no {@code notifyDataSetChanged()} anywhere in
 * this class and stable ids are enabled -- RecyclerView can then reuse view holders across rotation
 * instead of rebinding all 114 rows.
 *
 * <p>Each row reports the tapped {@link SurahIndex.Surah} through {@link OnSurahClickListener}; the
 * adapter deliberately knows nothing about Intents or activities, which keeps it testable and
 * reusable.
 */
public final class SurahListAdapter extends RecyclerView.Adapter<SurahListAdapter.SurahViewHolder> {

    /** Notified when the user taps a surah row. */
    public interface OnSurahClickListener {
        /** @param surah the tapped surah, never {@code null} */
        void onSurahClick(SurahIndex.Surah surah);
    }

    private final List<SurahIndex.Surah> surahs;
    private final OnSurahClickListener listener;

    public SurahListAdapter(@NonNull OnSurahClickListener listener) {
        this.surahs = SurahIndex.all();
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
        holder.bind(surahs.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return surahs.size();
    }

    @Override
    public long getItemId(int position) {
        // Stable, unique and meaningful: the 1-based surah number.
        return surahs.get(position).id;
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
    }
}
