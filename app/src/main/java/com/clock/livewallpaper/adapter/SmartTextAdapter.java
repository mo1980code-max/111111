package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.model.SmartClocks;
import com.clock.livewallpaper.viewUtils.SquareRelativeLayout;

import java.util.List;

/**
 * Smart clock tiles (clock over a background, with date and weather-style lines).
 *
 * <p>Like the digital section, the tile shows the bundled preview for both free and locked variants;
 * the separate background layer of the old layout is gone because the preview image already contains
 * the background of that style.
 */
public class SmartTextAdapter extends RecyclerView.Adapter<SmartTextAdapter.ViewHolder> {

    private final List<SmartClocks> smartClocks;
    private ClickListener clickListener;

    public interface ClickListener {
        void setClick(int position, SmartClocks smartClocks);
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

    public SmartTextAdapter(@NonNull List<SmartClocks> smartClocks) {
        this.smartClocks = smartClocks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_smarttextclock, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final SmartClocks item = this.smartClocks.get(position);
        final View tile = holder.itemView;
        final Context context = tile.getContext();
        LockOverlay.apply(tile, null, !LockOverlay.isAvailable(context, item));
        LockOverlay.loadPreview(holder.art, item.getPreviewAsset(), context);
        // NOTE: report the bound content position, not holder.getAdapterPosition(). This adapter is
        // wrapped by AdInsertingAdapter, so the holder's adapter position is the *wrapper* position
        // (shifted by ad rows, or NO_POSITION after a rebind), while `position` is this item's true
        // index in the clock list.
        final int contentPosition = position;
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SmartTextAdapter.this.clickListener != null) {
                    SmartTextAdapter.this.clickListener.setClick(contentPosition, item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.smartClocks.size();
    }
}
