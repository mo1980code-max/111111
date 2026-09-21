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
import com.clock.livewallpaper.viewUtils.AnalogClock;
import com.clock.livewallpaper.viewUtils.SquareRelativeLayout;

import java.util.List;

/**
 * Analog clock tiles.
 *
 * <p>Free and already-unlocked tiles render the live {@link AnalogClock}. A locked tile keeps its
 * original preview visible -- the live view stays on screen (ticking paused while covered) and the
 * bundled preview artwork from {@code assets/previews/clock/analog/} is drawn over it, dimmed by the
 * scrim with the padlock badge on top. The preview is never hidden, so the user always sees what they
 * are about to unlock. Nothing is fetched from the network: the artwork for both states ships with
 * the app.
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
        final AnalogClock clock;
        final ImageView preview;

        public ViewHolder(View view) {
            super(view);
            this.layout = (SquareRelativeLayout) view.findViewById(R.id.layoutBackground);
            this.clock = (AnalogClock) view.findViewById(R.id.iv_clock);
            this.preview = (ImageView) view.findViewById(R.id.previewImage);
        }

        public AnalogClock getTextView() {
            return this.clock;
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

        // The live clock view stays VISIBLE in both states (LockOverlay never hides the primary).
        // A locked tile is the original preview artwork over it, dimmed by the scrim, with the
        // padlock badge on top -- the user always sees what they are about to unlock.
        LockOverlay.apply(holder.itemView, holder.clock, !available);
        try {
            holder.layout.setCardBackgroundColor(Color.parseColor(clock.getBgColor()));
        } catch (IllegalArgumentException ignored) {
            // A bad colour string must not break the list; the card keeps its default background.
        }
        if (available) {
            holder.preview.setVisibility(View.GONE);
            holder.clock.setClock(clock);
            sizeClock(holder);
            holder.clock.setAutoUpdate(true);
        } else {
            // Locked: original preview over the dimmed live clock; no ticking while covered.
            holder.preview.setVisibility(View.VISIBLE);
            LockOverlay.loadPreview(holder.preview, clock.getPreviewAsset(),
                    holder.itemView.getContext());
            holder.clock.setAutoUpdate(false);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CustomAdapter.this.clickListener != null) {
                    CustomAdapter.this.clickListener.setClick(clock);
                }
            }
        });
    }

    /** The clock is drawn at a fraction of the card, which is only known after the first layout. */
    private void sizeClock(@NonNull final ViewHolder holder) {
        holder.layout.post(new Runnable() {
            @Override
            public void run() {
                float width = holder.layout.getMeasuredWidth();
                float height = holder.layout.getMeasuredHeight();
                if (width <= 0f || height <= 0f) {
                    return;
                }
                holder.clock.setClockSize(width / 1.8f);
                holder.clock.setPosition(width / 2.18f, height / 2.18f);
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        // Stop the ticking runnable of a detached face: a scrolled-off tile must not keep a callback.
        holder.clock.setAutoUpdate(false);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return this.localDataSet.size();
    }
}
