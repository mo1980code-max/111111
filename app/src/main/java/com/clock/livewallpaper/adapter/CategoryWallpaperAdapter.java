package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.catalog.WallpaperCatalog;
import com.clock.livewallpaper.catalog.WallpaperSection;
import com.clock.livewallpaper.image.LocalImage;
import com.clock.livewallpaper.utils.ArabicDigits;

import java.util.List;

/**
 * The wallpaper sections grid (مساجد مكة، مساجد المدينة المنورة، المسجد الأقصى، مساجد أخرى، مآذن،
 * صور "الله" -- plus "كل الخلفيات").
 *
 * <p>The cover is the section's own first free image, so the grid needs no network either. The caption
 * states the offer before the user taps: how many images the section holds and how many of them are
 * free, which is the same number the free tier uses ({@link WallpaperCatalog#FREE_PER_SECTION}).
 */
public class CategoryWallpaperAdapter extends RecyclerView.Adapter<CategoryWallpaperAdapter.ViewHolder> {

    private final List<WallpaperSection> sections;
    private ClickListener clickListener;

    public interface ClickListener {
        void setClick(WallpaperSection section);
    }

    public ClickListener getClickListener() {
        return this.clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView name;
        final TextView count;

        public ViewHolder(View view) {
            super(view);
            this.cover = (ImageView) view.findViewById(R.id.iv_clock);
            this.name = (TextView) view.findViewById(R.id.textName);
            this.count = (TextView) view.findViewById(R.id.textCount);
        }
    }

    public CategoryWallpaperAdapter(@NonNull List<WallpaperSection> sections) {
        this.sections = sections;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_cat_wallpaper, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final WallpaperSection section = this.sections.get(position);
        final Context context = holder.itemView.getContext();
        holder.name.setText(section.getName());
        int total = section.getEntries().size();
        int free = Math.min(WallpaperCatalog.FREE_PER_SECTION, total);
        holder.count.setText(ArabicDigits.toArabicIndic(
                context.getString(R.string.wallpaper_section_count, total, free)));
        if (section.getCover() != null) {
            LocalImage.into(holder.cover, section.getCover().getAssetPath(),
                    LockOverlay.previewEdgePx(context));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CategoryWallpaperAdapter.this.clickListener != null) {
                    CategoryWallpaperAdapter.this.clickListener.setClick(section);
                }
            }
        });
        if (position < 3) {
            Log.d("CONTENT_DEBUG", "CategoryWallpaperAdapter.onBindViewHolder position=" + position
                    + " section=" + section.getId() + " entries=" + section.getEntries().size());
        }
    }

    @Override
    public int getItemCount() {
        return this.sections.size();
    }
}
