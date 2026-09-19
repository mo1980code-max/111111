package com.clock.livewallpaper.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A wallpaper category: id, Arabic display name and its ordered items. */
public final class WallpaperSection {

    private final String id;
    private final String name;
    private final List<WallpaperEntry> entries;

    public WallpaperSection(String id, String name, List<WallpaperEntry> entries) {
        this.id = id;
        this.name = name;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<WallpaperEntry> getEntries() {
        return entries;
    }

    /** Cover tile: the first image of the section, which is always in the free tier. */
    public WallpaperEntry getCover() {
        return entries.isEmpty() ? null : entries.get(0);
    }

    public int freeCount() {
        int free = 0;
        for (WallpaperEntry entry : entries) {
            if (entry.isFreeByDefault()) {
                free++;
            }
        }
        return free;
    }
}
