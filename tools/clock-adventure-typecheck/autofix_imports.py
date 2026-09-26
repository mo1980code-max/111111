#!/usr/bin/env python3
"""
Adds missing imports to the presentation sources.

The type check reports `unresolved reference 'foo'`; most of those are simply a missing import of
a Compose extension (Compose extensions are top level functions, so they must be imported just
like a class). This script maps the known names to their package and inserts the import, then
re-runs the check until nothing is missing.
"""
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", "..", "clock-adventure-3d"))
PRES = os.path.join(PROJECT, "presentation", "src", "main", "kotlin")

LAYOUT = "androidx.compose.foundation.layout"
FOUNDATION = "androidx.compose.foundation"
DRAWSCOPE = "androidx.compose.ui.graphics.drawscope"
DRAW = "androidx.compose.ui.draw"
UNIT = "androidx.compose.ui.unit"

NAMES = {
    # layout
    "size": LAYOUT + ".size",
    "width": LAYOUT + ".width",
    "height": LAYOUT + ".height",
    "sizeIn": LAYOUT + ".sizeIn",
    "widthIn": LAYOUT + ".widthIn",
    "heightIn": LAYOUT + ".heightIn",
    "padding": LAYOUT + ".padding",
    "offset": LAYOUT + ".offset",
    "absoluteOffset": LAYOUT + ".absoluteOffset",
    "aspectRatio": LAYOUT + ".aspectRatio",
    "fillMaxWidth": LAYOUT + ".fillMaxWidth",
    "fillMaxHeight": LAYOUT + ".fillMaxHeight",
    "fillMaxSize": LAYOUT + ".fillMaxSize",
    "wrapContentWidth": LAYOUT + ".wrapContentWidth",
    "wrapContentHeight": LAYOUT + ".wrapContentHeight",
    "wrapContentSize": LAYOUT + ".wrapContentSize",
    "requiredSize": LAYOUT + ".requiredSize",
    "BoxWithConstraints": LAYOUT + ".BoxWithConstraints",
    "LazyColumn": "androidx.compose.foundation.lazy.LazyColumn",
    "LazyRow": "androidx.compose.foundation.lazy.LazyRow",
    "items": "androidx.compose.foundation.lazy.items",
    "rememberLazyListState": "androidx.compose.foundation.lazy.rememberLazyListState",
    "verticalScroll": "androidx.compose.foundation.verticalScroll",
    "horizontalScroll": "androidx.compose.foundation.horizontalScroll",
    "rememberScrollState": "androidx.compose.foundation.rememberScrollState",
    # foundation
    "background": FOUNDATION + ".background",
    "border": FOUNDATION + ".border",
    "clickable": FOUNDATION + ".clickable",
    "combinedClickable": FOUNDATION + ".combinedClickable",
    "Canvas": FOUNDATION + ".Canvas",
    "isSystemInDarkTheme": FOUNDATION + ".isSystemInDarkTheme",
    "MutableInteractionSource": FOUNDATION + ".interaction.MutableInteractionSource",
    "collectIsPressedAsState": FOUNDATION + ".interaction.collectIsPressedAsState",
    "RoundedCornerShape": FOUNDATION + ".shape.RoundedCornerShape",
    "CircleShape": FOUNDATION + ".shape.CircleShape",
    "detectDragGestures": FOUNDATION + ".gestures.detectDragGestures",
    "detectTapGestures": FOUNDATION + ".gestures.detectTapGestures",
    # draw
    "shadow": DRAW + ".shadow",
    "rotate": DRAW + ".rotate",
    "clip": DRAW + ".clip",
    "drawWithContent": DRAW + ".drawWithContent",
    "drawBehind": DRAW + ".drawBehind",
    # drawscope
    "drawCircle": DRAWSCOPE + ".drawCircle",
    "drawLine": DRAWSCOPE + ".drawLine",
    "drawRect": DRAWSCOPE + ".drawRect",
    "drawRoundRect": DRAWSCOPE + ".drawRoundRect",
    "drawOval": DRAWSCOPE + ".drawOval",
    "drawArc": DRAWSCOPE + ".drawArc",
    "drawPath": DRAWSCOPE + ".drawPath",
    # unit
    "dp": UNIT + ".dp",
    "sp": UNIT + ".sp",
    "times": UNIT + ".times",
    "plus": UNIT + ".plus",
    "minus": UNIT + ".minus",
    "div": UNIT + ".div",
    "roundToPx": UNIT + ".roundToPx",
    "toPx": UNIT + ".toPx",
    "Dp": UNIT + ".Dp",
    "IntOffset": UNIT + ".IntOffset",
    "LayoutDirection": UNIT + ".LayoutDirection",
    # graphics
    "Color": "androidx.compose.ui.graphics.Color",
    "Brush": "androidx.compose.ui.graphics.Brush",
    "Stroke": DRAWSCOPE + ".Stroke",
    "StrokeCap": "androidx.compose.ui.graphics.StrokeCap",
    "Path": "androidx.compose.ui.graphics.Path",
    "SolidColor": "androidx.compose.ui.graphics.SolidColor",
    "Offset": "androidx.compose.ui.geometry.Offset",
    "Size": "androidx.compose.ui.geometry.Size",
    "CornerRadius": "androidx.compose.ui.geometry.CornerRadius",
    "DrawScope": DRAWSCOPE + ".DrawScope",
    # text
    "TextStyle": "androidx.compose.ui.text.TextStyle",
    "FontFamily": "androidx.compose.ui.text.font.FontFamily",
    "Font": "androidx.compose.ui.text.font.Font",
    "FontWeight": "androidx.compose.ui.text.font.FontWeight",
    "FontStyle": "androidx.compose.ui.text.font.FontStyle",
    "TextAlign": "androidx.compose.ui.text.style.TextAlign",
    "stringResource": "androidx.compose.ui.res.stringResource",
    # material3
    "Text": "androidx.compose.material3.Text",
    "MaterialTheme": "androidx.compose.material3.MaterialTheme",
    "Button": "androidx.compose.material3.Button",
    "ButtonDefaults": "androidx.compose.material3.ButtonDefaults",
    "Surface": "androidx.compose.material3.Surface",
    "Card": "androidx.compose.material3.Card",
    "Slider": "androidx.compose.material3.Slider",
    "Switch": "androidx.compose.material3.Switch",
    "ripple": "androidx.compose.material3.ripple",
    "Dialog": "androidx.compose.ui.window.Dialog",
    # animation
    "animateFloatAsState": "androidx.compose.animation.core.animateFloatAsState",
    "animateDpAsState": "androidx.compose.animation.core.animateDpAsState",
    "animateColorAsState": "androidx.compose.animation.core.animateColorAsState",
    "spring": "androidx.compose.animation.core.spring",
    "tween": "androidx.compose.animation.core.tween",
    "infiniteRepeatable": "androidx.compose.animation.core.infiniteRepeatable",
    "rememberInfiniteTransition": "androidx.compose.animation.core.rememberInfiniteTransition",
    "RepeatMode": "androidx.compose.animation.core.RepeatMode",
    "LinearEasing": "androidx.compose.animation.core.LinearEasing",
    "animateFloat": "androidx.compose.animation.core.animateFloat",
    "AnimatedVisibility": "androidx.compose.animation.AnimatedVisibility",
    "fadeIn": "androidx.compose.animation.fadeIn",
    "fadeOut": "androidx.compose.animation.fadeOut",
    "scaleIn": "androidx.compose.animation.scaleIn",
    "scaleOut": "androidx.compose.animation.scaleOut",
    "slideInVertically": "androidx.compose.animation.slideInVertically",
    "slideOutVertically": "androidx.compose.animation.slideOutVertically",
    # runtime
    "remember": "androidx.compose.runtime.remember",
    "mutableStateOf": "androidx.compose.runtime.mutableStateOf",
    "getValue": "androidx.compose.runtime.getValue",
    "setValue": "androidx.compose.runtime.setValue",
    "LaunchedEffect": "androidx.compose.runtime.LaunchedEffect",
    "DisposableEffect": "androidx.compose.runtime.DisposableEffect",
    "SideEffect": "androidx.compose.runtime.SideEffect",
    "withFrameNanos": "androidx.compose.runtime.withFrameNanos",
    "CompositionLocalProvider": "androidx.compose.runtime.CompositionLocalProvider",
    "staticCompositionLocalOf": "androidx.compose.runtime.staticCompositionLocalOf",
    "compositionLocalOf": "androidx.compose.runtime.compositionLocalOf",
    "LocalDensity": "androidx.compose.ui.platform.LocalDensity",
    "LocalContext": "androidx.compose.ui.platform.LocalContext",
    "LocalConfiguration": "androidx.compose.ui.platform.LocalConfiguration",
    "LocalLayoutDirection": "androidx.compose.ui.platform.LocalLayoutDirection",
    "R": "com.clockadventure.presentation.R",
    "Job": "kotlinx.coroutines.Job",
    "delay": "kotlinx.coroutines.delay",
    "launch": "kotlinx.coroutines.launch",
    "collect": "kotlinx.coroutines.flow.collect",
    "first": "kotlinx.coroutines.flow.first",
    "withLock": "kotlinx.coroutines.sync.withLock",
    "update": "kotlinx.coroutines.flow.update",
}

# Names that are types of the same package as the file: skip (handled by hand).
ERROR_RE = re.compile(r"^(?P<file>[^:]+):\d+:\d+: error: unresolved reference '(?P<name>[^']+)'")


def run_check(module):
    proc = subprocess.run(
        [sys.executable, os.path.join(HERE, "check.py"), module],
        capture_output=True, text=True
    )
    return proc.stdout


def fix_module(module, max_rounds=12):
    for _ in range(max_rounds):
        output = run_check(module)
        missing = {}
        for line in output.splitlines():
            line = line.strip()
            match = ERROR_RE.match(line)
            if not match:
                continue
            name = match.group("name")
            if name in NAMES:
                missing.setdefault(match.group("file"), set()).add(NAMES[name])
        if not missing:
            print("no missing imports left")
            print(output[-2000:])
            return
        for rel_path, imports in missing.items():
            path = rel_path if os.path.isabs(rel_path) else os.path.join(PROJECT, rel_path)
            if not os.path.exists(path):
                continue
            lines = open(path).read().split("\n")
            existing = set(l for l in lines if l.startswith("import "))
            if any(not l.startswith("import ") and l.strip() == "" for l in lines):
                pass
            additions = sorted("import %s" % i for i in imports if ("import %s" % i) not in existing)
            if not additions:
                continue
            last = max(i for i, l in enumerate(lines) if l.startswith("import "))
            lines = lines[: last + 1] + additions + lines[last + 1:]
            open(path, "w").write("\n".join(lines))
            print("+%s: %s" % (os.path.basename(path), ", ".join(a.split(".")[-1] for a in additions)))
            if not additions:
                print("   (nothing to add, the error must be something else)")


if __name__ == "__main__":
    fix_module(sys.argv[1] if len(sys.argv) > 1 else "presentation")
