package com.clock.livewallpaper.adapter;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.clock.ClockStyle;

import java.util.List;

/** Horizontal, static style picker. It never creates or runs a clock preview per item. */
public final class ClockStyleAdapter extends RecyclerView.Adapter<ClockStyleAdapter.StyleViewHolder> {

    public interface OnStyleClickListener {
        void onStyleSelected(@NonNull ClockStyle style);
    }

    private final List<ClockStyle> styles;
    private final OnStyleClickListener listener;
    private String selectedId;

    public ClockStyleAdapter(@NonNull List<ClockStyle> styles,
                             @NonNull String selectedId,
                             @NonNull OnStyleClickListener listener) {
        this.styles = styles;
        this.selectedId = selectedId;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setSelectedId(@NonNull String selectedId) {
        String oldId = this.selectedId;
        this.selectedId = selectedId;
        int oldIndex = indexOf(oldId);
        int newIndex = indexOf(selectedId);
        if (oldIndex >= 0) {
            notifyItemChanged(oldIndex);
        }
        if (newIndex >= 0 && newIndex != oldIndex) {
            notifyItemChanged(newIndex);
        }
    }

    @NonNull
    @Override
    public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StyleViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clock_style, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
        ClockStyle style = styles.get(position);
        holder.title.setText(style.getTitleResId());
        holder.family.setText(style.getFamilyResId());
        holder.preview.setText(style.isAnalog() ? "◷" : "12:45");
        holder.preview.setTypeface(style.isAnalog()
                ? Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                : Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        holder.preview.setTextColor(style.getPreviewColor());
        holder.accent.setBackgroundColor(style.getPreviewColor());
        boolean selected = style.getId().equals(selectedId);
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? 0xE62A3D45 : 0xB8172932);
        background.setCornerRadius(16f);
        background.setStroke(selected ? 2 : 1,
                selected ? 0xFFF0C96A : 0x507D9699);
        holder.card.setBackground(background);
        holder.card.setContentDescription(holder.card.getContext().getString(
                R.string.clock_style_content_description,
                holder.card.getContext().getString(style.getTitleResId())));
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClockStyleAdapter.this.listener.onStyleSelected(style);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return styles.get(position).getId().hashCode();
    }

    @Override
    public int getItemCount() {
        return styles.size();
    }

    private int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < styles.size(); i++) {
            if (id.equals(styles.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    static final class StyleViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout card;
        final View accent;
        final TextView preview;
        final TextView title;
        final TextView family;

        StyleViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (LinearLayout) itemView.findViewById(R.id.clock_style_card);
            accent = itemView.findViewById(R.id.clock_style_accent);
            preview = (TextView) itemView.findViewById(R.id.clock_style_preview);
            title = (TextView) itemView.findViewById(R.id.clock_style_title);
            family = (TextView) itemView.findViewById(R.id.clock_style_family);
        }
    }
}
