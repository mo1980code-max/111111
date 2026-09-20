package com.clock.livewallpaper.catalog;

import android.content.Context;
import android.util.Log;

import com.clock.livewallpaper.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The offline wallpaper catalog, read from {@code assets/wallpapers/index.json}.
 *
 * <p>Replaces the old remote catalog: no HTTP, no Glide URL loading, no "download" step -- the JSON
 * only lists local asset paths, and {@code tools/build_local_previews.py} regenerates the manifest
 * together with the images. Every file it names exists in the APK, which is what
 * {@code tests/test_catalog_ads_unlocks.py} checks.
 *
 * <p>{@link #SECTION_ALL} is a synthetic section that concatenates every real one in catalog order. It
 * exists so the wallpaper browser also has a list long enough to host in-feed native ads at the
 * configured spacing, while each individual section stays short and therefore ad free.
 */
public final class WallpaperCatalog {

    /** How many images of every section are free -- the number the tiles and the caption both show. */
    public static final int FREE_PER_SECTION = com.clock.livewallpaper.ads.AdConfig.FREE_WALLPAPERS_PER_SECTION;

    public static final String INDEX_ASSET = "wallpapers/index.json";
    public static final String SECTION_ALL = "all";

    private static final String TAG = "WallpaperCatalog";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static volatile List<WallpaperSection> cachedSections;
    private static volatile Map<String, WallpaperEntry> cachedById;

    private WallpaperCatalog() {
    }

    /** Real sections only, in catalog order. Parsed once per process. */
    public static List<WallpaperSection> sections(Context context) {
        List<WallpaperSection> local = cachedSections;
        if (local == null) {
            synchronized (WallpaperCatalog.class) {
                if (cachedSections == null) {
                    cachedSections = parse(context.getApplicationContext());
                }
                local = cachedSections;
            }
        }
        return local;
    }

    /** The real sections in catalog order, with the synthetic "all" list first, for the browser grid. */
    public static List<WallpaperSection> sectionsWithAll(Context context) {
        List<WallpaperSection> list = new ArrayList<>();
        list.add(new WallpaperSection(SECTION_ALL, allTitle(context), allEntries(context)));
        list.addAll(sections(context));
        return list;
    }

    public static List<WallpaperEntry> allEntries(Context context) {
        List<WallpaperEntry> entries = new ArrayList<>();
        for (WallpaperSection section : sections(context)) {
            entries.addAll(section.getEntries());
        }
        return entries;
    }

    /** Items of one section, or every item when {@link #SECTION_ALL} is requested. */
    public static List<WallpaperEntry> items(Context context, String sectionId) {
        if (SECTION_ALL.equals(sectionId)) {
            return allEntries(context);
        }
        for (WallpaperSection section : sections(context)) {
            if (section.getId().equals(sectionId)) {
                return section.getEntries();
            }
        }
        return Collections.emptyList();
    }

    public static String title(Context context, String sectionId) {
        if (SECTION_ALL.equals(sectionId)) {
            return allTitle(context);
        }
        for (WallpaperSection section : sections(context)) {
            if (section.getId().equals(sectionId)) {
                return section.getName();
            }
        }
        return "";
    }

    public static WallpaperEntry byId(Context context, String id) {
        Map<String, WallpaperEntry> local = cachedById;
        if (local == null) {
            synchronized (WallpaperCatalog.class) {
                if (cachedById == null) {
                    Map<String, WallpaperEntry> index = new HashMap<>();
                    for (WallpaperEntry entry : allEntries(context)) {
                        index.put(entry.getId(), entry);
                    }
                    cachedById = index;
                }
                local = cachedById;
            }
        }
        return id == null ? null : local.get(id);
    }

    /** The synthetic list's name is a resource, like the real names in the manifest. */
    private static String allTitle(Context context) {
        return context.getString(R.string.wallpaper_all_title);
    }

    private static List<WallpaperSection> parse(Context context) {
        List<WallpaperSection> sections = new ArrayList<>();
        try {
            String json = read(context.getAssets().open(INDEX_ASSET));
            JSONObject root = new JSONObject(json);
            JSONArray list = root.optJSONArray("sections");
            if (list == null) {
                return sections;
            }
            for (int i = 0; i < list.length(); i++) {
                JSONObject raw = list.getJSONObject(i);
                List<WallpaperEntry> entries = new ArrayList<>();
                JSONArray items = raw.optJSONArray("items");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject item = items.getJSONObject(j);
                        entries.add(new WallpaperEntry(raw.optString("id"), item.optString("id"),
                                item.optString("file"), item.optString("title"),
                                item.optBoolean("free")));
                    }
                }
                sections.add(new WallpaperSection(raw.optString("id"), raw.optString("name"), entries));
            }
        } catch (Exception error) {
            // A damaged catalog must not take the app down: the browser simply shows an empty list.
            Log.w(TAG, "unable to read " + INDEX_ASSET, error);
            Log.d("CONTENT_DEBUG", "WallpaperCatalog.parse FAILED: " + error);
            return sections;
        }
        int totalItems = 0;
        for (WallpaperSection section : sections) {
            totalItems += section.getEntries().size();
        }
        Log.d("CONTENT_DEBUG", "WallpaperCatalog.parse sections=" + sections.size()
                + " totalItems=" + totalItems);
        return sections;
    }

    private static String read(InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8 * 1024];
        int read;
        while ((read = stream.read(chunk)) > 0) {
            buffer.write(chunk, 0, read);
        }
        stream.close();
        return new String(buffer.toByteArray(), UTF8);
    }

    /** Test hook: lets a unit test force a re-parse. */
    static void invalidate() {
        cachedSections = null;
        cachedById = null;
    }
}
