package com.clock.livewallpaper.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.adapter.AllahNameAdapter;
import com.clock.livewallpaper.clock.AllahNameCatalog;
import com.clock.livewallpaper.clock.ClockPreferences;
import com.clock.livewallpaper.unlock.UnlockPrompt;

/**
 * The local 99 Names gallery. It contains no ad rows: a locked Name is previewed in place and the
 * only unlock action is the explicit rewarded dialog before opening Clock Studio.
 */
public final class AllahNamesActivity extends AppCompatActivity {
    private AllahNameAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allah_names);
        ImageView back = findViewById(R.id.allahNamesBack);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        RecyclerView recycler = findViewById(R.id.allahNamesRecycler);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        this.adapter = new AllahNameAdapter(this, new AllahNameAdapter.Listener() {
            @Override
            public void onNameSelected(final int nameId) {
                openName(nameId);
            }
        });
        recycler.setAdapter(this.adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }
    }

    private void openName(final int nameId) {
        if (AllahNameCatalog.isFree(nameId)
                || AllahNameCatalog.isUsable(this, nameId)) {
            openStudio(nameId);
            return;
        }
        UnlockPrompt.showName(this, AllahNameCatalog.unlockKey(nameId),
                new UnlockPrompt.Result() {
                    @Override
                    public void onUnlocked() {
                        openStudio(nameId);
                    }

                    @Override
                    public void onUnavailable(String message) {
                        if (message != null) {
                            Toast.makeText(AllahNamesActivity.this, message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openStudio(int nameId) {
        if (!ClockPreferences.get(this).selectName(nameId)) {
            Toast.makeText(this, R.string.allah_name_locked_message,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new android.content.Intent(this, ClockStudioActivity.class));
    }
}
