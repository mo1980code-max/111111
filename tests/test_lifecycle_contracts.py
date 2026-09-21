"""Lifecycle guards for the self-rescheduling clock views.

``AnalogClock.setTime(Calendar)`` used to end with::

    if (this.autoUpdate) {
        new Handler().postDelayed(new Runnable() {
            public void run() { AnalogClock.this.setTime(Calendar.getInstance()); }
        }, 800);
    }

The posted runnable calls ``setTime`` again, so every call to ``setTime`` or ``setAutoUpdate``
started an **independent, permanent** 800 ms chain:

* the ``Handler`` was allocated inline, so no reference survived and nothing could ever cancel it;
* ``CustomAdapter.onBindViewHolder`` calls ``setAutoUpdate(true)`` on every bind, so each scroll
  past a tile stacked another chain on top of the ones already running;
* each chain holds the ``Runnable`` -> the ``AnalogClock`` -> its ``Context``, keeping a finished
  Activity reachable for as long as the process lives;
* ``new Handler()`` with no looper adopts the calling thread's looper and throws if there is none.

The chains also kept running while the view was detached, where ``invalidate()`` cannot repaint
anything: ``LiveClockWallpaper`` holds its ``AnalogClock`` in a ``WidgetGroup`` that is never
attached to a window and drives painting from its own ``mHandler`` loop instead.

The fix is one owned ``Handler`` plus one owned ``Runnable``, cancelled before every re-arm and on
detach, and only armed while the view is attached. These tests pin that shape. They are static
checks, matching the rest of ``tests/`` -- this workspace has no JDK or Android SDK.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/clock/livewallpaper"
ANALOG = JAVA / "viewUtils/AnalogClock.java"
ADAPTER = JAVA / "adapter/CustomAdapter.java"
WALLPAPER = JAVA / "LiveClockWallpaper.java"
CUSTOM_WALLPAPER = JAVA / "CustomWallpaper.java"


def source(path):
    return path.read_text(encoding="utf-8")


def code_only(path):
    """Source with comments and string literals removed, so prose cannot satisfy a check."""
    text = source(path)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return re.sub(r'"(\\.|[^"\\])*"', '""', text)


class AnalogClockTickTests(unittest.TestCase):
    def test_no_anonymous_handler_is_allocated(self):
        # The whole defect in one line: an un-cancellable handler created on the fly.
        self.assertNotIn("new Handler()", code_only(ANALOG),
                         "AnalogClock must not allocate an unowned Handler; a tick that cannot be "
                         "cancelled leaks the view and stacks up on every rebind")

    def test_handler_and_runnable_are_owned_fields(self):
        code = code_only(ANALOG)
        self.assertRegex(code, r"private\s+final\s+Handler\s+tickHandler",
                         "the tick needs one final Handler field so it can be cancelled")
        self.assertRegex(code, r"private\s+final\s+Runnable\s+tickRunnable",
                         "the tick needs one final Runnable field so removeCallbacks can match it")

    def test_handler_is_bound_to_the_main_looper(self):
        # setAutoUpdate is reached from adapter and wallpaper code; bare new Handler() would adopt
        # whatever looper happened to be current, or throw when there is none.
        self.assertIn("new Handler(Looper.getMainLooper())", code_only(ANALOG))
        self.assertIn("import android.os.Looper;", source(ANALOG))

    def test_pending_tick_is_cancelled_before_rearming(self):
        body = self._method_body("scheduleTick")
        cancel = body.find("removeCallbacks")
        post = body.find("postDelayed")
        self.assertNotEqual(cancel, -1, "scheduleTick must cancel the pending tick first")
        self.assertNotEqual(post, -1, "scheduleTick must re-arm the tick")
        self.assertLess(cancel, post,
                        "removeCallbacks has to run before postDelayed, otherwise two chains "
                        "survive and the tick rate doubles on every rebind")

    def test_tick_only_runs_while_attached(self):
        body = self._method_body("scheduleTick")
        self.assertIn("isAttachedToWindow()", body,
                      "a detached view cannot repaint: invalidate() has no ViewRootImpl to "
                      "schedule against, so arming the tick only burns the main thread")
        self.assertIn("autoUpdate", body)

    def test_detach_cancels_the_tick(self):
        code = code_only(ANALOG)
        self.assertIn("protected void onDetachedFromWindow()", code,
                      "without this override a scrolled-off tile keeps its callback forever")
        body = self._method_body("onDetachedFromWindow")
        self.assertIn("removeCallbacks", body)

    def test_attach_restarts_the_tick(self):
        # EditorActivity calls setAutoUpdate(true) in onCreate, before the view is attached, so
        # the tick can only start from onAttachedToWindow.
        code = code_only(ANALOG)
        self.assertIn("protected void onAttachedToWindow()", code,
                      "setAutoUpdate is called before attach; without this override the clock "
                      "would never start ticking")
        self.assertIn("scheduleTick", self._method_body("onAttachedToWindow"))

    def test_set_time_reschedules_through_the_single_helper(self):
        code = code_only(ANALOG)
        self.assertEqual(code.count("postDelayed"), 1,
                         "exactly one place may arm the tick, otherwise the chains multiply again")
        self.assertIn("scheduleTick();", code)

    def _method_body(self, name):
        """Extract a method body by brace matching, so a check cannot pass on a neighbour."""
        code = code_only(ANALOG)
        match = re.search(r"\b" + re.escape(name) + r"\s*\([^)]*\)\s*\{", code)
        self.assertIsNotNone(match, f"{name}() not found in AnalogClock")
        start = match.end() - 1
        depth = 0
        for index in range(start, len(code)):
            if code[index] == "{":
                depth += 1
            elif code[index] == "}":
                depth -= 1
                if depth == 0:
                    return code[start:index + 1]
        self.fail(f"unbalanced braces while reading {name}()")


class AdapterRecyclingTests(unittest.TestCase):
    def test_recycled_tile_stops_ticking(self):
        code = code_only(ADAPTER)
        self.assertIn("onViewRecycled", code,
                      "a recycled tile must stop its clock, or scrolling leaves ticks behind")
        self.assertIn("setAutoUpdate(false)", code)

    def test_locked_tiles_keep_their_preview_visible(self):
        # Step 11 of the repair brief: locked content is preview + scrim + badge, never hidden.
        overlay = code_only(JAVA / "adapter/LockOverlay.java")
        self.assertNotRegex(
            overlay, r"primary\.setVisibility\(\s*locked\s*\?",
            "the primary preview must stay VISIBLE when locked; only the scrim and badge toggle")
        self.assertIn("primary.setVisibility(View.VISIBLE)", overlay)


class WallpaperEngineTests(unittest.TestCase):
    """The service owns its own repaint loop; it must stop when the wallpaper is hidden."""

    def test_engines_stop_drawing_when_not_visible(self):
        for path in (WALLPAPER, CUSTOM_WALLPAPER):
            code = code_only(path)
            with self.subTest(service=path.name):
                self.assertIn("onVisibilityChanged", code)
                self.assertIn("removeCallbacks", code,
                              f"{path.name} must cancel its draw callback when hidden")
                self.assertRegex(code, r"if\s*\(\s*(this\.)?mVisible\s*\)",
                                 f"{path.name} must only re-arm the draw loop while visible")

    def test_engines_cancel_on_destroy_and_surface_loss(self):
        for path in (WALLPAPER, CUSTOM_WALLPAPER):
            code = code_only(path)
            with self.subTest(service=path.name):
                for hook in ("onDestroy", "onSurfaceDestroyed"):
                    self.assertIn(hook, code, f"{path.name} must handle {hook}")

    def test_wallpaper_handler_is_a_field(self):
        for path in (WALLPAPER, CUSTOM_WALLPAPER):
            with self.subTest(service=path.name):
                self.assertRegex(code_only(path), r"private\s+final\s+Handler\s+mHandler",
                                 f"{path.name} needs an owned Handler to cancel its draw loop")


if __name__ == "__main__":
    unittest.main()
