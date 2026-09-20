package com.clock.livewallpaper.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.model.Clocks;
import com.clock.livewallpaper.viewUtils.SquareRelativeLayout;

import java.util.List;

/**
 * Static analog clock tiles.
 *
 * <p>The gallery uses the bundled preview for every state. A tile never owns a live clock, handler,
 * timer, or animation: only the single reusable Clock Studio creates a ticking renderer. Locked and
 * unlocked cards keep the same preview, with the shared scrim and padlock badge applied on top.
 * Nothing is fetched from the network: the artwork ships with the app.
 *
 * <p>Clicks are reported to the Activity, which is the only place allowed to decide between "open the
 * clock" and "offer a rewarded unlock".
 */
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private final List<Clocks> localDataSet;
    private ClickListener clickListener;

    public interface ClickListener {
        void setClick(Clocks clocks);
    }

    public ClickListener getClickListener() {
        return this.clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final SquareRelativeLayout layout;
        final ImageView preview;

        public ViewHolder(View view) {
            super(view);
            this.layout = (SquareRelativeLayout) view.findViewById(R.id.layoutBackground);
            this.preview = (ImageView) view.findViewById(R.id.previewImage);
        }
    }

    public CustomAdapter(@NonNull List<Clocks> dataSet) {
        this.localDataSet = dataSet;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_clocks, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Clocks clock = this.localDataSet.get(position);
        final boolean available = LockOverlay.isAvailable(holder.itemView.getContext(), clock);

        // The static preview stays VISIBLE in both states (LockOverlay never hides the primary).
        // A locked tile is the original preview dimmed by the scrim, with the padlock badge on top.
        LockOverlay.apply(holder.itemView, holder.preview, !available);
        LockOverlay.loadPreview(holder.preview, clock.getPreviewAsset(),
                holder.itemView.getContext());
        try {
            holder.layout.setCardBackgroundColor(Color.parseColor(clock.getBgColor()));
        } catch (IllegalArgumentException ignored) {
            // A bad colour string must not break the list; the card keeps its default background.
        }
        holder.preview.setVisibility(View.VISIBLE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CustomAdapter.this.clickListener != null) {
                    CustomAdapter.this.clickListener.setClick(clock);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.localDataSet.size();
    }
}
