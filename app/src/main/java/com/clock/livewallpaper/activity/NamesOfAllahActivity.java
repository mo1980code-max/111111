package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdsManager;
import com.clock.livewallpaper.adapter.AllahNamesAdapter;
import com.clock.livewallpaper.catalog.AllahNamesCatalog;
import com.clock.livewallpaper.catalog.ContentAccess;
import com.clock.livewallpaper.catalog.Unlockable;
import com.clock.livewallpaper.model.AllahName;

/** The offline two-column gallery and the single entry point for Name unlocks. */
public final class NamesOfAllahActivity extends AppCompatActivity {

    private AllahNamesAdapter adapter;
    private RecyclerView recyclerView;
    private TextView progress;
    /** Guards the dialog/ad request so rapid taps can never associate two rewards with one flow. */
    private boolean unlockFlowActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_names_of_allah);

        ImageView back = findViewById(R.id.allah_names_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        progress = findViewById(R.id.allah_names_progress);

        recyclerView = findViewById(R.id.allah_names_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, gallerySpanCount()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(6);
        adapter = new AllahNamesAdapter(AllahNamesCatalog.getAll());
        adapter.setOnNameClickListener(new AllahNamesAdapter.OnNameClickListener() {
            @Override
            public void onNameSelected(@NonNull AllahName name) {
                handleNameClick(name);
            }
        });
        recyclerView.setAdapter(adapter);
        updateProgress();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Warm the shared cache only; the existing manager never auto-shows a rewarded ad.
        AdsManager.get().preloadRewarded(this);
        updateProgress();
    }

    private void handleNameClick(@NonNull final AllahName name) {
        if (unlockFlowActive) {
            return;
        }
        boolean alreadyAvailable = ContentAccess.isAvailable(this, name);
        if (!alreadyAvailable) {
            unlockFlowActive = true;
        }

        // ContentAccess handles the free/already-unlocked fast path without showing a dialog.
        ContentAccess.open(this, name, new Runnable() {
            @Override
            public void run() {
                // UnlockStore has already persisted the reward when this refresh runs.
                updateProgress();
            }
        }, new ContentAccess.Listener() {
            @Override
            public void onReady(@NonNull Unlockable item) {
                unlockFlowActive = false;
                if (!isActivityUsable()) {
                    return;
                }
                if (!alreadyAvailable) {
                    adapter.notifyItemChanged(name.getNumber() - 1);
                    updateProgress();
                }
                openClockStudio(name);
            }

            @Override
            public void onBlocked(@Nullable String message) {
                unlockFlowActive = false;
                if (message != null && !message.isEmpty() && isActivityUsable()) {
                    Toast.makeText(NamesOfAllahActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** Opens the reusable Clock Studio only after free access or a confirmed reward. */
    private void openClockStudio(@NonNull AllahName name) {
        Intent intent = new Intent(this, ClockStudioActivity.class);
        intent.putExtra(ClockStudioActivity.EXTRA_NAME_ID, name.getStableId());
        startActivity(intent);
        overridePendingTransition(R.anim.clock_studio_enter, R.anim.clock_studio_exit);
    }

    private int gallerySpanCount() {
        float density = getResources().getDisplayMetrics().density;
        float widthDp = getResources().getDisplayMetrics().widthPixels / density;
        return Math.max(2, Math.min(4, (int) (widthDp / 170f)));
    }

    private void updateProgress() {
        if (progress == null) {
            return;
        }
        int available = 0;
        for (AllahName name : AllahNamesCatalog.getAll()) {
            if (ContentAccess.isAvailable(this, name)) {
                available++;
            }
        }
        progress.setText(getString(R.string.allah_names_progress, available));
    }

    @Override
    protected void onPause() {
        cancelGalleryAnimations();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unlockFlowActive = false;
        cancelGalleryAnimations();
        super.onDestroy();
    }

    private void cancelGalleryAnimations() {
        if (recyclerView == null) {
            return;
        }
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            child.animate().cancel();
            child.setScaleX(1f);
            child.setScaleY(1f);
        }
    }

    private boolean isActivityUsable() {
        return !isFinishing() && (android.os.Build.VERSION.SDK_INT < 17 || !isDestroyed());
    }
}
