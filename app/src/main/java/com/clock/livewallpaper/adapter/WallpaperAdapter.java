package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.catalog.WallpaperEntry;

import java.util.List;

/**
 * Wallpaper tiles of one section.
 *
 * <p>Every tile is decoded from {@code assets/wallpapers/<section>/}: there is no image URL, no
 * placeholder-while-downloading state and nothing to wait for. A locked tile only differs by the scrim,
 * the padlock pill and the "open it with an ad" label, and its tap is answered by the Activity with the
 * unlock dialog -- the ad is never shown just for opening the list.
 */
public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

    private final List<WallpaperEntry> entries;
    private ClickListener clickListener;

    public interface ClickListener {
        void setClick(int position, WallpaperEntry entry);
    }

    public ClickListener getClickListener() {
        return this.clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;

        public ViewHolder(View view) {
            super(view);
            this.image = (ImageView) view.findViewById(R.id.iv_clock);
            this.title = (TextView) view.findViewById(R.id.textName);
        }
    }

    public WallpaperAdapter(@NonNull List<WallpaperEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_wallpaper, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final WallpaperEntry entry = this.entries.get(position);
        final View tile = holder.itemView;
        final Context context = tile.getContext();
        holder.title.setText(entry.getTitle());
        LockOverlay.apply(tile, null, !LockOverlay.isAvailable(context, entry));
        LockOverlay.loadPreview(holder.image, entry.getAssetPath(), context);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (WallpaperAdapter.this.clickListener != null) {
                    WallpaperAdapter.this.clickListener.setClick(holder.getAdapterPosition(), entry);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.entries.size();
    }
}
