package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.model.TextClocks;
import com.clock.livewallpaper.viewUtils.SquareRelativeLayout;

import java.util.List;

/**
 * Digital clock tiles.
 *
 * <p>The tile art is always the local preview in {@code assets/previews/clock/digital/}, both for the
 * free tier and for a locked tile, so what the user sees in the list is what they get after an unlock.
 * {@code textClockPosition} in the editor still uses {@link TextClocks#getStyle()} -- the layout index,
 * deliberately independent from the position in this list, because unlocked variants may be added
 * without moving anybody's saved style.
 */
public class TextAdapter extends RecyclerView.Adapter<TextAdapter.ViewHolder> {

    private final List<TextClocks> textClocks;
    private ClickListener clickListener;

    public interface ClickListener {
        void setClick(int position, TextClocks textClocks);
    }

    public ClickListener getClickListener() {
        return this.clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView art;
        final SquareRelativeLayout layout;

        public ViewHolder(View view) {
            super(view);
            this.art = (ImageView) view.findViewById(R.id.clockwise);
            this.layout = (SquareRelativeLayout) view.findViewById(R.id.layoutBackground);
        }
    }

    public TextAdapter(@NonNull List<TextClocks> textClocks) {
        this.textClocks = textClocks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_textclock, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final TextClocks item = this.textClocks.get(position);
        final View tile = holder.itemView;
        final Context context = tile.getContext();
        // The preview is the artwork of both states, so only the scrim and the padlock change.
        LockOverlay.apply(tile, null, !LockOverlay.isAvailable(context, item));
        try {
            holder.layout.setCardBackgroundColor(Color.parseColor(item.getBgColor()));
        } catch (IllegalArgumentException ignored) {
            // A bad colour string must not break the list.
        }
        LockOverlay.loadPreview(holder.art, item.getPreviewAsset(), context);
        // NOTE: report the bound content position, not holder.getAdapterPosition(). This adapter is
        // wrapped by AdInsertingAdapter, so the holder's adapter position is the *wrapper* position
        // (shifted by ad rows, or NO_POSITION after a rebind), while `position` is this item's true
        // index in the clock list.
        final int contentPosition = position;
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextAdapter.this.clickListener != null) {
                    TextAdapter.this.clickListener.setClick(contentPosition, item);
                }
            }
        });
        if (position < 3) {
            Log.d("CONTENT_DEBUG", "TextAdapter.onBindViewHolder position=" + position
                    + " id=" + item.getId() + " available=" + LockOverlay.isAvailable(context, item)
                    + " artVisible=" + (holder.art.getVisibility() == View.VISIBLE)
                    + " artDrawable=" + (holder.art.getDrawable() != null));
        }
    }

    @Override
    public int getItemCount() {
        return this.textClocks.size();
    }
}
