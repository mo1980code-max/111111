package com.clock.livewallpaper.azkar;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the offline Azkar content from {@code assets/azkar.json}.
 *
 * <h2>Mandatory sources</h2>
 * <ul>
 *   <li>Morning Azkar: {@link #SOURCE_MORNING}</li>
 *   <li>Evening Azkar: {@link #SOURCE_EVENING}</li>
 *   <li>Tasbeeh: {@link #SOURCE_TASBEEH}</li>
 * </ul>
 * The JSON was transcribed from these pages preserving the Arabic text, the Azkar order, the
 * Azkar numbers and the exact repetition count of every single Azkar.
 *
 * <h2>Session-only counters</h2>
 * <p>Parsed content is cached per category; {@link #items(AzkarCategory)} builds <b>fresh</b>
 * {@link AzkarItem} copies on every call, and each copy starts its countdown from its own
 * original {@code repeatCount}. Nothing counter-related is read from or written to storage —
 * closing the app and reopening the screen always restarts every counter from its target.
 * The only persisted Azkar setting is the text size ({@link AzkarFontStore}).
 */
public final class AzkarRepository {

    /** Exact source of the Morning Azkar text and repetition counts. */
    public static final String SOURCE_MORNING =
            "https://www.islambook.com/azkar/1/أذكار-الصباح";
    /** Exact source of the Evening Azkar text and repetition counts. */
    public static final String SOURCE_EVENING =
            "https://www.islambook.com/azkar/2/أذكار-المساء";
    /** Exact source of the Tasbeeh text and repetition counts. */
    public static final String SOURCE_TASBEEH =
            "https://www.islamiokul.com/arabic/zikirler.html";

    private static final String TAG = "AzkarRepository";
    private static final String ASSET_NAME = "azkar.json";

    private static volatile AzkarRepository instance;

    private final Context appContext;
    /** Parsed content per category; guarded by {@code contentLock}. */
    private final Map<AzkarCategory, List<AzkarItem>> contentCache =
            new EnumMap<>(AzkarCategory.class);
    private final Object contentLock = new Object();
    private boolean loadFailed;

    private AzkarRepository(Context context) {
        appContext = context.getApplicationContext();
    }

    /** @return the process-wide instance, creating it on first use */
    @NonNull
    public static AzkarRepository get(@NonNull Context context) {
        AzkarRepository local = instance;
        if (local == null) {
            synchronized (AzkarRepository.class) {
                local = instance;
                if (local == null) {
                    local = new AzkarRepository(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * @return the items of {@code category} in source order, each with a fresh counter starting
     *         from its own original {@code repeatCount}; an empty list when the asset is missing
     *         or damaged (callers show the error state)
     */
    @NonNull
    public List<AzkarItem> items(@NonNull AzkarCategory category) {
        List<AzkarItem> content = content(category);
        List<AzkarItem> items = new ArrayList<>(content.size());
        for (AzkarItem template : content) {
            // نسخة جديدة في كل مرة: العدّاد يبدأ من العدد الأصلي دائماً، ولا يُقرأ من التخزين.
            items.add(new AzkarItem(
                    template.id(),
                    template.category(),
                    template.order(),
                    template.arabicText(),
                    template.repeatCount(),
                    template.virtue()));
        }
        return items;
    }

    /**
     * Home-screen progress. Counters live only inside the open category screen, so from the
     * home screen's point of view nothing is ever banked: always {@code 0}.
     */
    public int completedCount(@NonNull AzkarCategory category) {
        return 0;
    }

    /** @return the total number of imported items in {@code category} (never hard-coded) */
    public int totalCount(@NonNull AzkarCategory category) {
        return content(category).size();
    }

    @NonNull
    private List<AzkarItem> content(@NonNull AzkarCategory category) {
        synchronized (contentLock) {
            List<AzkarItem> cached = contentCache.get(category);
            if (cached != null) {
                return cached;
            }
            if (loadFailed) {
                return Collections.emptyList();
            }
            try {
                parseAssetLocked();
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Unable to load " + ASSET_NAME, e);
                loadFailed = true;
                return Collections.emptyList();
            }
            List<AzkarItem> parsed = contentCache.get(category);
            return parsed != null ? parsed : Collections.emptyList();
        }
    }

    /**
     * Parses the whole asset once and fills the cache for all three categories, in file order.
     * Must be called holding {@code contentLock}.
     */
    private void parseAssetLocked() throws IOException, JSONException {
        byte[] bytes;
        InputStream in = appContext.getAssets().open(ASSET_NAME);
        try {
            bytes = new byte[in.available()];
            int offset = 0;
            int read;
            while (offset < bytes.length
                    && (read = in.read(bytes, offset, bytes.length - offset)) != -1) {
                offset += read;
            }
        } finally {
            in.close();
        }
        JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        for (AzkarCategory category : AzkarCategory.values()) {
            JSONArray array = root.optJSONArray(category.key());
            List<AzkarItem> items = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    items.add(new AzkarItem(
                            o.optInt("id", i + 1),
                            category,
                            o.optInt("order", i + 1),
                            o.optString("text", ""),
                            Math.max(1, o.optInt("repeat", 1)),
                            o.optString("virtue", "")));
                }
            }
            contentCache.put(category, Collections.unmodifiableList(items));
        }
    }
}
