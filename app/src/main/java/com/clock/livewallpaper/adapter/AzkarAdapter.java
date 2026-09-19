package com.clock.livewallpaper.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.azkar.AzkarFonts;
import com.clock.livewallpaper.azkar.AzkarItem;
import com.clock.livewallpaper.azkar.AzkarRepository;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the Azkar category screen: one card per Azkar with its own interactive counter.
 *
 * <p>Counter contract (per item, never shared): each tap calls {@link AzkarItem#countUp()},
 * which advances {@code currentCount} towards that item's own {@code repeatCount} and never
 * beyond it; the new count is persisted immediately through {@link AzkarRepository#save}.
 * Reaching the target flips the card and the counter to the emerald completed state with
 * "✓ Completed". The small Reset button returns the item to {@code 0}.
 *
 * <p>Only the large counter button advances the count; the rest of the card is inert so
 * scrolling never counts by accident. Completion changes are a plain background swap plus one
 * short scale pulse on the button — no excessive animation.
 */
public final class AzkarAdapter extends RecyclerView.Adapter<AzkarAdapter.AzkarViewHolder> {

    /** Notified after any count or reset so the screen can repaint its overall progress. */
    public interface OnProgressChangeListener {
        void onProgressChanged();
    }

    private final List<AzkarItem> items;
    private final AzkarRepository repository;
    private final OnProgressChangeListener listener;

    public AzkarAdapter(@NonNull List<AzkarItem> items,
                        @NonNull AzkarRepository repository,
                        @NonNull OnProgressChangeListener listener) {
        this.items = items;
        this.repository = repository;
        this.listener = listener;
        // Must be set before the adapter is attached to a RecyclerView.
        setHasStableIds(true);
    }

    /** @return the live items, so the screen can compute overall progress */
    @NonNull
    public List<AzkarItem> items() {
        return items;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id();
    }

    @NonNull
    @Override
    public AzkarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View card = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_azkar, parent, false);
        Typeface arabic = AzkarFonts.arabic(parent.getContext());
        return new AzkarViewHolder(card, arabic);
    }

    @Override
    public void onBindViewHolder(@NonNull AzkarViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class AzkarViewHolder extends RecyclerView.ViewHolder {

        private final View card;
        private final TextView numberView;
        private final TextView repeatView;
        private final TextView resetView;
        private final TextView textView;
        private final TextView virtueView;
        private final LinearLayout counterButton;
        private final TextView counterTextView;
        private final TextView counterHintView;

        AzkarViewHolder(@NonNull View itemView, Typeface arabic) {
            super(itemView);
            card = itemView.findViewById(R.id.azkar_card);
            numberView = itemView.findViewById(R.id.azkar_number);
            repeatView = itemView.findViewById(R.id.azkar_repeat);
            resetView = itemView.findViewById(R.id.azkar_reset);
            textView = itemView.findViewById(R.id.azkar_text);
            virtueView = itemView.findViewById(R.id.azkar_virtue);
            counterButton = itemView.findViewById(R.id.azkar_counter);
            counterTextView = itemView.findViewById(R.id.azkar_counter_text);
            counterHintView = itemView.findViewById(R.id.azkar_counter_hint);
            if (arabic != null) {
                textView.setTypeface(arabic);
                virtueView.setTypeface(arabic);
            }
        }

        void bind(@NonNull AzkarItem item) {
            numberView.setText(numberView.getContext().getString(
                    R.string.azkar_card_number, item.id()));
            repeatView.setText(repeatView.getContext().getString(
                    R.string.azkar_repeat_chip, item.repeatCount()));
            textView.setText(item.arabicText());

            String virtue = item.virtue();
            if (virtue == null || virtue.trim().isEmpty()) {
                virtueView.setVisibility(View.GONE);
            } else {
                virtueView.setVisibility(View.VISIBLE);
                virtueView.setText(virtue);
            }

            paintState(item);

            resetView.setVisibility(item.currentCount() > 0 ? View.VISIBLE : View.GONE);
            resetView.setContentDescription(resetView.getContext().getString(
                    R.string.azkar_cd_reset, item.id()));
            resetView.setOnClickListener(v -> {
                repository.reset(item);
                refreshPosition();
                listener.onProgressChanged();
            });

            counterButton.setContentDescription(counterButton.getContext().getString(
                    R.string.azkar_cd_counter, item.id(), item.currentCount(), item.repeatCount()));
            counterButton.setOnClickListener(v -> {
                if (!item.countUp()) {
                    return;
                }
                repository.save(item);
                pulse(counterButton);
                refreshPosition();
                listener.onProgressChanged();
            });
        }

        /**
         * Paints the normal vs. completed state: emerald card tint + emerald counter with
         * "✓ Completed" exactly when {@code currentCount == repeatCount}.
         */
        private void paintState(@NonNull AzkarItem item) {
            boolean done = item.isCompleted();
            card.setBackgroundResource(done
                    ? R.drawable.bg_azkar_card_completed
                    : R.drawable.bg_azkar_card);
            counterButton.setBackgroundResource(done
                    ? R.drawable.bg_azkar_counter_completed
                    : R.drawable.bg_azkar_counter);

            counterTextView.setText(String.format(Locale.US, "%d / %d",
                    item.currentCount(), item.repeatCount()));
            if (done) {
                int white = ContextCompat.getColor(counterTextView.getContext(),
                        R.color.azkar_on_primary);
                counterTextView.setTextColor(white);
                counterHintView.setText(R.string.azkar_completed);
                counterHintView.setTextColor(white);
            } else {
                counterTextView.setTextColor(ContextCompat.getColor(counterTextView.getContext(),
                        R.color.azkar_primary));
                counterHintView.setText(R.string.azkar_tap_to_count);
                counterHintView.setTextColor(ContextCompat.getColor(counterHintView.getContext(),
                        R.color.azkar_muted));
            }
        }

        /**
         * Rebinds this holder's position after a tap. Uses {@code getAdapterPosition()} (not the
         * 1.2+ {@code getBindingAdapterPosition()}) because the app ships recyclerview 1.1.0.
         */
        private void refreshPosition() {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }

        /** One short scale pulse on the tapped button; nothing else animates. */
        private void pulse(@NonNull View view) {
            view.animate().cancel();
            view.setScaleX(0.97f);
            view.setScaleY(0.97f);
            view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        }
    }
}
