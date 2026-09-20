package com.clock.livewallpaper.adapter;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.azkar.AzkarFontStore;
import com.clock.livewallpaper.azkar.AzkarItem;

import java.util.List;

/**
 * Adapter for the Azkar category screen: one card per Azkar with its own countdown counter.
 *
 * <p>Counter contract (per item, never shared, never persisted): each card shows how many
 * repetitions are <b>left</b>, starting from that item's own {@code repeatCount} (e.g. 3).
 * Each tap on the large counter button calls {@link AzkarItem#countDown()} ({@code remaining--},
 * clamped at zero) and rebinds the card — the equivalent of Flutter's {@code setState}.
 * Nothing is written to storage: reopening the screen rebuilds the items, so every counter
 * restarts from its original number. Taps on a completed item are ignored; the counter is
 * never reset except by reopening the screen.
 *
 * <p>Reaching zero flips the card and the counter to the emerald completed state and notifies
 * {@link OnProgressChangeListener#onItemCompleted(AzkarItem)} once, so the screen can show
 * its short "تم" toast.
 *
 * <p>Display settings (owned by the screen, persisted in {@link AzkarFontStore}) repaint
 * instantly without touching any counter: {@link #setFontSize(float)} resizes the dhikr
 * text (titles, buttons and the counter keep their fixed sizes),
 * {@link #setTypeface(Typeface)} swaps the Arabic face, and {@link #setNightMode(boolean)}
 * swaps the card/counter drawables and ink colours to the Azkar night palette. With night
 * mode off and the default settings, every pixel matches the original design.
 *
 * <p>Only the large counter button advances the count; the rest of the card is inert so
 * scrolling never counts by accident. Completion changes are a plain background swap plus one
 * short scale pulse on the button — no excessive animation.
 */
public final class AzkarAdapter extends RecyclerView.Adapter<AzkarAdapter.AzkarViewHolder> {

    /**
     * Notified after any count so the screen can repaint its overall progress, and exactly
     * once per item when its countdown reaches zero.
     */
    public interface OnProgressChangeListener {
        void onProgressChanged();

        void onItemCompleted(@NonNull AzkarItem item);
    }

    private final List<AzkarItem> items;
    private final OnProgressChangeListener listener;
    /** Dhikr text size in sp, applied to the dhikr text; owned by the screen, persisted by it. */
    private float fontSp;
    /** Arabic face for the dhikr text, or null for the platform default. */
    @Nullable
    private Typeface typeface;
    /** True while the Azkar-section night mode is on. */
    private boolean night;

    public AzkarAdapter(@NonNull List<AzkarItem> items,
                        float fontSp,
                        @Nullable Typeface typeface,
                        boolean night,
                        @NonNull OnProgressChangeListener listener) {
        this.items = items;
        this.fontSp = AzkarFontStore.clamp(fontSp);
        this.typeface = typeface;
        this.night = night;
        this.listener = listener;
        // Must be set before the adapter is attached to a RecyclerView.
        setHasStableIds(true);
    }

    /** Applies a new dhikr text size to all cards immediately (the screen persists it). */
    public void setFontSize(float fontSp) {
        this.fontSp = AzkarFontStore.clamp(fontSp);
        notifyDataSetChanged();
    }

    /** Applies a new Arabic face to all cards immediately (null = platform default). */
    public void setTypeface(@Nullable Typeface typeface) {
        this.typeface = typeface;
        notifyDataSetChanged();
    }

    /** Switches all cards between the day and night palettes immediately. */
    public void setNightMode(boolean night) {
        this.night = night;
        notifyDataSetChanged();
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
        return new AzkarViewHolder(card);
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
        private final TextView textView;
        private final TextView virtueView;
        private final LinearLayout counterButton;
        private final TextView counterTextView;
        private final TextView counterHintView;

        AzkarViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.azkar_card);
            numberView = itemView.findViewById(R.id.azkar_number);
            repeatView = itemView.findViewById(R.id.azkar_repeat);
            textView = itemView.findViewById(R.id.azkar_text);
            virtueView = itemView.findViewById(R.id.azkar_virtue);
            counterButton = itemView.findViewById(R.id.azkar_counter);
            counterTextView = itemView.findViewById(R.id.azkar_counter_text);
            counterHintView = itemView.findViewById(R.id.azkar_counter_hint);
        }

        void bind(@NonNull AzkarItem item) {
            numberView.setText(numberView.getContext().getString(
                    R.string.azkar_card_number, item.id()));
            repeatView.setText(repeatView.getContext().getString(
                    R.string.azkar_repeat_chip, item.repeatCount()));
            textView.setText(item.arabicText());

            // نوع الخط المختار لنص الذكر (null يعيد خط النظام الافتراضي).
            textView.setTypeface(typeface);
            virtueView.setTypeface(typeface);
            // حجم الخط المحفوظ لنص الذكر فقط؛ العناوين والأزرار والعدّاد بمقاسات ثابتة.
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp);
            virtueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(11f, fontSp - 7f));
            // حبر النص حسب الوضع (نهاري/ليلي).
            textView.setTextColor(ContextCompat.getColor(textView.getContext(),
                    night ? R.color.azkar_night_text : R.color.azkar_text));
            virtueView.setTextColor(ContextCompat.getColor(virtueView.getContext(),
                    night ? R.color.azkar_night_muted : R.color.azkar_muted));

            String virtue = item.virtue();
            if (virtue == null || virtue.trim().isEmpty()) {
                virtueView.setVisibility(View.GONE);
            } else {
                virtueView.setVisibility(View.VISIBLE);
                virtueView.setText(virtue);
            }

            paintState(item);

            counterButton.setContentDescription(counterButton.getContext().getString(
                    R.string.azkar_cd_counter, item.id(), item.remaining(), item.repeatCount()));
            counterButton.setOnClickListener(v -> {
                // ضغطة = واحد أقل. لا حفظ هنا إطلاقاً — الحالة في الذاكرة فقط.
                if (!item.countDown()) {
                    return; // مكتمل (صفر): يُتجاهل، ولا يعيد العد إلا بإعادة فتح الشاشة.
                }
                pulse(counterButton);
                if (item.isCompleted()) {
                    listener.onItemCompleted(item); // الشاشة تعرض "تم" مؤقتاً.
                }
                refreshPosition();
                listener.onProgressChanged();
            });
        }

        /**
         * Paints the normal vs. completed state in the active palette (day or night):
         * emerald card tint + emerald counter with "✓ Completed" exactly when the
         * countdown reached zero.
         */
        private void paintState(@NonNull AzkarItem item) {
            boolean done = item.isCompleted();
            if (night) {
                card.setBackgroundResource(done
                        ? R.drawable.bg_azkar_card_completed_night
                        : R.drawable.bg_azkar_card_night);
                counterButton.setBackgroundResource(done
                        ? R.drawable.bg_azkar_counter_completed_night
                        : R.drawable.bg_azkar_counter_night);
            } else {
                card.setBackgroundResource(done
                        ? R.drawable.bg_azkar_card_completed
                        : R.drawable.bg_azkar_card);
                counterButton.setBackgroundResource(done
                        ? R.drawable.bg_azkar_counter_completed
                        : R.drawable.bg_azkar_counter);
            }

            // يعرض المتبقي: يبدأ من العدد الأصلي (مثلاً 3) ويتناقص حتى 0.
            counterTextView.setText(String.valueOf(item.remaining()));
            if (done) {
                int white = ContextCompat.getColor(counterTextView.getContext(),
                        R.color.azkar_on_primary);
                counterTextView.setTextColor(white);
                counterHintView.setText(R.string.azkar_completed);
                counterHintView.setTextColor(white);
            } else {
                counterTextView.setTextColor(ContextCompat.getColor(counterTextView.getContext(),
                        night ? R.color.azkar_night_primary : R.color.azkar_primary));
                counterHintView.setText(R.string.azkar_tap_to_count);
                counterHintView.setTextColor(ContextCompat.getColor(counterHintView.getContext(),
                        night ? R.color.azkar_night_muted : R.color.azkar_muted));
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
