package com.clock.livewallpaper.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.catalog.AllahNamesCatalog;
import com.clock.livewallpaper.catalog.ContentAccess;
import com.clock.livewallpaper.model.AllahName;

import java.util.List;

/** RecyclerView adapter for the 99 Names of Allah grid. */
public final class AllahNamesAdapter extends RecyclerView.Adapter<AllahNamesAdapter.NameViewHolder> {

    public interface OnNameClickListener {
        void onNameSelected(@NonNull AllahName name);
    }

    private static final int[] CARD_THEMES = {
            R.drawable.bg_allah_theme_navy,
            R.drawable.bg_allah_theme_emerald,
            R.drawable.bg_allah_theme_black_gold,
            R.drawable.bg_allah_theme_sapphire,
            R.drawable.bg_allah_theme_forest,
            R.drawable.bg_allah_theme_plum
    };

    private final List<AllahName> names;
    private Typeface arabicTypeface;
    private OnNameClickListener nameClickListener;

    public AllahNamesAdapter(@NonNull List<AllahName> names) {
        if (names.size() != AllahNamesCatalog.EXPECTED_COUNT) {
            throw new IllegalArgumentException("AllahNamesAdapter requires exactly 99 names");
        }
        this.names = names;
        setHasStableIds(true);
    }

    public void setOnNameClickListener(OnNameClickListener listener) {
        this.nameClickListener = listener;
    }

    @NonNull
    @Override
    public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (arabicTypeface == null) {
            try {
                arabicTypeface = Typeface.createFromAsset(
                        parent.getContext().getApplicationContext().getAssets(),
                        "fonts/cairo_regular.ttf");
            } catch (RuntimeException ignored) {
                // Android's default Arabic typeface remains a safe fallback.
            }
        }
        View card = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_allah_name, parent, false);
        return new NameViewHolder(card);
    }

    @Override
    public void onBindViewHolder(@NonNull NameViewHolder holder, int position) {
        AllahName name = names.get(position);
        holder.card.setBackgroundResource(CARD_THEMES[position % CARD_THEMES.length]);
        holder.number.setText(holder.number.getContext().getString(
                R.string.allah_name_number, name.getNumber()));
        holder.arabicName.setText(name.getArabicName());
        holder.transliteration.setText(name.getTransliteration());
        if (arabicTypeface != null) {
            holder.arabicName.setTypeface(Typeface.create(arabicTypeface, Typeface.BOLD));
        }
        holder.card.setContentDescription(holder.card.getContext().getString(
                R.string.allah_name_content_description,
                name.getNumber(), name.getArabicName(), name.getTransliteration()));
        boolean locked = !ContentAccess.isAvailable(holder.card.getContext(), name);
        LockOverlay.apply(holder.card, holder.arabicName, locked);
        holder.card.setClickable(this.nameClickListener != null);
        holder.card.setOnClickListener(this.nameClickListener == null ? null : new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AllahNamesAdapter.this.nameClickListener != null) {
                    AllahNamesAdapter.this.nameClickListener.onNameSelected(name);
                }
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return names.get(position).getNumber();
    }

    /** The bound data set is intentionally exactly 99 items for Stage 1. */
    @Override
    public int getItemCount() {
        return names.size();
    }

    static final class NameViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout card;
        final TextView number;
        final TextView arabicName;
        final TextView transliteration;

        NameViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (FrameLayout) itemView.findViewById(R.id.allah_name_card);
            number = (TextView) itemView.findViewById(R.id.allah_name_number);
            arabicName = (TextView) itemView.findViewById(R.id.allah_name_arabic);
            transliteration = (TextView) itemView.findViewById(R.id.allah_name_transliteration);
        }
    }
}
