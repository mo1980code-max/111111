package com.clock.livewallpaper.ads;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inserts native ad rows into an existing list adapter without changing that adapter.
 *
 * <p>Rows are laid out as {@code [content...] [ad] [content...] [ad] ...} where the ad positions come
 * from {@link AdPolicy#nativeSlotPositions(int)}: one ad every {@code NATIVE_AD_INTERVAL} items, and
 * no ad at all for short lists. Because the ad row spans the full width
 * ({@link GridLayoutManager.SpanSizeLookup} below), a two column clock or wallpaper grid keeps its
 * rhythm and the ad never looks like just another card.
 *
 * <p>The wrapped adapter keeps owning its own view types; {@link #TYPE_AD} is far outside their range
 * so the two never collide.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AdInsertingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_AD = 100_001;
    private static final int CONTENT_ROW = -1;

    private final RecyclerView.Adapter contentAdapter;
    private final NativePlacement placement;
    private final List<Integer> rows = new ArrayList<>();
    private final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            AdInsertingAdapter.this.syncNow();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            AdInsertingAdapter.this.syncNow();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            AdInsertingAdapter.this.syncNow();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            // Positions only move when the row plan itself changes, so a plain re-plan is enough and
            // never animates a card out from under the user's finger.
            AdInsertingAdapter.this.syncNow();
        }
    };

    public AdInsertingAdapter(@NonNull RecyclerView.Adapter contentAdapter, @NonNull NativePlacement placement) {
        this.contentAdapter = contentAdapter;
        this.placement = placement;
        this.contentAdapter.registerAdapterDataObserver(observer);
        rebuild();
    }

    /** Recomputes the row plan and refreshes everything (ad insertions shift positions). */
    public void syncNow() {
        rebuild();
        notifyDataSetChanged();
    }

    public NativePlacement getPlacement() {
        return placement;
    }

    private void rebuild() {
        // CRITICAL INVARIANT: content rows are unconditional. The grid must render its clocks /
        // wallpapers no matter what the ad layer does (no network, no fill, protected section, or an
        // ad-free build). Ad rows are purely additive: they are inserted *between* content rows only
        // when policy allows them, and they can never replace or hide a content row.
        rows.clear();
        int contentCount = contentAdapter.getItemCount();
        List<Integer> slots = AdPolicy.nativeAdsAllowed()
                ? AdPolicy.nativeSlotPositions(contentCount)
                : Collections.<Integer>emptyList();
        int slotIndex = 0;
        for (int position = 0; position < contentCount; position++) {
            if (slotIndex < slots.size() && slots.get(slotIndex) == position) {
                rows.add(Integer.valueOf(TYPE_AD));
                slotIndex++;
            }
            rows.add(Integer.valueOf(position));
        }
        while (slotIndex < slots.size()) {
            rows.add(Integer.valueOf(TYPE_AD));
            slotIndex++;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            FrameLayout slot = (FrameLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ad_native_slot, parent, false);
            return new AdViewHolder(slot);
        }
        return contentAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int row = rows.get(position).intValue();
        if (row == TYPE_AD) {
            placement.renderInto(((AdViewHolder) holder).slot);
            return;
        }
        contentAdapter.onBindViewHolder(holder, row);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof AdViewHolder) {
            placement.release(((AdViewHolder) holder).slot);
            return;
        }
        contentAdapter.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        int row = rows.get(position).intValue();
        if (row == TYPE_AD) {
            return TYPE_AD;
        }
        return contentAdapter.getItemViewType(row);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
        // Only the ad row opts out of recycling (rebinding a NativeAdView mid-scroll can flicker).
        // Content tiles must recycle normally, otherwise every scroll inflates new tiles and the grid
        // stutters on low-end devices.
        if (holder instanceof AdViewHolder) {
            return true;
        }
        return contentAdapter.onFailedToRecycleView(holder);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        final RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            final GridLayoutManager grid = (GridLayoutManager) manager;
            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (position < 0 || position >= rows.size()) {
                        return grid.getSpanCount();
                    }
                    return rows.get(position).intValue() == TYPE_AD ? grid.getSpanCount() : 1;
                }
            });
        }
        placement.setDatasetChangedListener(new Runnable() {
            @Override
            public void run() {
                syncNow();
            }
        });
        // Only ask for an ad when this list is long enough to host one. A six image wallpaper section
        // therefore spends no request at all; the plan is recomputed on every data change, so an unlock
        // can never leave an ad row dangling at the end of a list that has become too short for one.
        if (hasAdRows()) {
            placement.prepare(recyclerView.getContext());
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    /** True when the current plan contains at least one ad row. */
    public boolean hasAdRows() {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).intValue() == TYPE_AD) {
                return true;
            }
        }
        return false;
    }

    /** Row that hosts a single in-feed native ad. */
    static final class AdViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout slot;

        AdViewHolder(FrameLayout slot) {
            super(slot);
            this.slot = slot;
        }
    }

    /** Release the wrapped adapter's observers plus the cached ad when the screen goes away. */
    public void destroy() {
        contentAdapter.unregisterAdapterDataObserver(observer);
        placement.destroy();
    }
}
