package com.clock.livewallpaper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.clock.AllahNameCatalog;
import com.clock.livewallpaper.unlock.UnlockStore;

/** A lightweight, ad-free gallery for the 99 Names used by Clock Studio. */
public final class AllahNameAdapter extends RecyclerView.Adapter<AllahNameAdapter.NameViewHolder> {
    public interface Listener {
        void onNameSelected(int nameId);
    }

    private final Context context;
    private final Listener listener;

    public AllahNameAdapter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_allah_name, parent, false);
        return new NameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NameViewHolder holder, int position) {
        final int nameId = position + 1;
        boolean free = AllahNameCatalog.isFree(nameId);
        boolean unlocked = free || UnlockStore.get(this.context)
                .isUnlocked(AllahNameCatalog.unlockKey(nameId));
        holder.number.setText(this.context.getString(R.string.allah_name_number, nameId));
        holder.name.setText(AllahNameCatalog.getArabicName(nameId));
        holder.lock.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        holder.itemView.setContentDescription(this.context.getString(
                unlocked ? R.string.allah_name_open_description : R.string.allah_name_locked_description,
                nameId));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AllahNameAdapter.this.listener.onNameSelected(nameId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return AllahNameCatalog.count();
    }

    public static final class NameViewHolder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView name;
        final TextView lock;

        NameViewHolder(@NonNull View itemView) {
            super(itemView);
            this.number = itemView.findViewById(R.id.allahNameNumber);
            this.name = itemView.findViewById(R.id.allahNameArabic);
            this.lock = itemView.findViewById(R.id.allahNameLock);
        }
    }
}
