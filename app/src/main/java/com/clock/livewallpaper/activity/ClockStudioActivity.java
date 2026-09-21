package com.clock.livewallpaper.activity;

/**
 * Named entry point for the existing editor.  The project historically called this screen
 * {@link EditorActivity}; keeping the subclass avoids a second editor implementation while giving
 * Clock Studio and the manifest a stable, descriptive name.
 */
public class ClockStudioActivity extends EditorActivity {
    // The rendering and saved-settings behaviour intentionally remains in EditorActivity.
}
