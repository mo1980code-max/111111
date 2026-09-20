package com.clock.livewallpaper.clock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;

import com.clock.livewallpaper.date.HijriDate;
import com.clock.livewallpaper.date.HijriDateFormatter;
import com.clock.livewallpaper.date.HijriDateProvider;
import com.clock.livewallpaper.R;

import java.util.Calendar;
import java.util.Date;

/**
 * Canvas compositor used by the existing LiveClockWallpaper service.
 *
 * <p>Static composition work is rendered into one cached bitmap and the existing Clock Studio Canvas
 * views are drawn on top. The wallpaper engine owns the cadence; the views are never started and do not
 * create their own handlers. This keeps the app preview and wallpaper on the same ClockStyle renderer
 * without creating a second visual implementation.</p>
 */
public final class ClockStudioWallpaperRenderer {

    private final Context context;
    private final ClockStudioWallpaperConfig config;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint transliterationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF datePanelBounds = new RectF();
    private final RectF clockBounds = new RectF();
    private final RectF secondaryBounds = new RectF();
    private final Calendar calendar = Calendar.getInstance();

    private final AnalogClockView analogClock;
    private final DigitalClockView digitalClock;
    private final DigitalClockView secondaryClock;
    private final View activeClock;

    private Typeface arabicTypeface;
    private Bitmap staticLayer;
    private int width;
    private int height;
    private int staticDateKey = Integer.MIN_VALUE;
    private boolean dateAvailable;
    private String dayText = "";
    private String hijriText = "";
    private String gregorianText = "";
    private boolean laidOut;

    public ClockStudioWallpaperRenderer(@NonNull Context context,
                                        @NonNull ClockStudioWallpaperConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        Typeface loadedArabic = null;
        try {
            loadedArabic = Typeface.createFromAsset(this.context.getAssets(),
                    "fonts/cairo_regular.ttf");
        } catch (RuntimeException ignored) {
            // Android's platform Arabic face is a safe fallback.
        }
        arabicTypeface = loadedArabic;

        if (config.getStyle().isAnalog()) {
            analogClock = new AnalogClockView(this.context);
            analogClock.setStyle(config.getStyle());
            analogClock.setShowSeconds(config.showSeconds());
            analogClock.setAccentColor(config.getClockColor());
            digitalClock = null;
            activeClock = analogClock;
        } else {
            digitalClock = new DigitalClockView(this.context);
            digitalClock.setStyle(config.getStyle());
            digitalClock.setTwentyFourHour(config.isTwentyFourHour());
            digitalClock.setShowSeconds(config.showSeconds());
            digitalClock.setAccentColor(config.getClockColor());
            analogClock = null;
            activeClock = digitalClock;
        }

        if (config.getStyle().hasSecondaryDigitalClock()) {
            secondaryClock = new DigitalClockView(this.context);
            secondaryClock.setStyle(ClockStyleRegistry.byId("digital_elegant_thin"));
            secondaryClock.setTwentyFourHour(config.isTwentyFourHour());
            secondaryClock.setShowSeconds(config.showSeconds());
            secondaryClock.setAccentColor(config.getClockColor());
        } else {
            secondaryClock = null;
        }

        configurePaints();
    }

    /** Updates the surface size; static artwork is rebuilt only when size or date changes. */
    public void setSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            laidOut = false;
            recycleStaticLayer();
        }
    }

    /** Draws the cached composition and the current clock at the current surface size. */
    public void draw(@NonNull Canvas canvas) {
        if (width <= 0 || height <= 0) {
            return;
        }
        refreshDateIfNeeded();
        ensureStaticLayer();
        canvas.drawBitmap(staticLayer, 0f, 0f, null);
        if (!laidOut) {
            layoutClocks();
        }
        drawClock(canvas, activeClock, clockBounds);
        if (secondaryClock != null) {
            drawClock(canvas, secondaryClock, secondaryBounds);
        }
    }

    /** Cadence owned by the WallpaperService, not by the off-screen Clock Studio views. */
    public long nextFrameDelayMillis() {
        if (config.getStyle().isAnalog() && config.showSeconds()) {
            return 100L;
        }
        if (config.showSeconds()) {
            return Math.max(250L, 1000L - (System.currentTimeMillis() % 1000L) + 20L);
        }
        Calendar now = Calendar.getInstance();
        long untilMinute = 60_000L - now.get(Calendar.SECOND) * 1_000L
                - now.get(Calendar.MILLISECOND) + 40L;
        return Math.max(250L, untilMinute);
    }

    public void destroy() {
        if (analogClock != null) {
            analogClock.stopClock();
        }
        if (digitalClock != null) {
            digitalClock.stopClock();
        }
        if (secondaryClock != null) {
            secondaryClock.stopClock();
        }
        recycleStaticLayer();
    }

    private void configurePaints() {
        backgroundPaint.setDither(true);
        namePaint.setAntiAlias(true);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setColor(Color.rgb(255, 248, 231));
        namePaint.setTypeface(arabicTypeface != null
                ? Typeface.create(arabicTypeface, Typeface.BOLD)
                : Typeface.create("sans-serif", Typeface.BOLD));
        numberPaint.setAntiAlias(true);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        numberPaint.setColor(Color.rgb(242, 212, 141));
        transliterationPaint.setAntiAlias(true);
        transliterationPaint.setTextAlign(Paint.Align.CENTER);
        transliterationPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        transliterationPaint.setColor(Color.rgb(214, 201, 149));
        dividerPaint.setAntiAlias(true);
        panelPaint.setAntiAlias(true);
        datePaint.setAntiAlias(true);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTypeface(arabicTypeface != null
                ? Typeface.create(arabicTypeface, Typeface.BOLD)
                : Typeface.create("sans-serif", Typeface.BOLD));
        mutedDatePaint.setAntiAlias(true);
        mutedDatePaint.setTextAlign(Paint.Align.CENTER);
        mutedDatePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    private void refreshDateIfNeeded() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int dateKey = calendar.get(Calendar.YEAR) * 400 + calendar.get(Calendar.DAY_OF_YEAR);
        if (dateKey == staticDateKey) {
            return;
        }
        staticDateKey = dateKey;
        Date now = calendar.getTime();
        dateAvailable = config.showHijri() || config.showGregorian() || config.showDay();
        if (config.showHijri()) {
            HijriDate hijriDate = HijriDateProvider.today(config.getHijriAdjustment());
            hijriText = HijriDateFormatter.formatHijri(context, hijriDate);
        } else {
            hijriText = "";
        }
        if (config.showGregorian()) {
            gregorianText = HijriDateFormatter.formatGregorian(context, now);
        } else {
            gregorianText = "";
        }
        if (config.showDay()) {
            dayText = HijriDateFormatter.formatDay(context, now);
        } else {
            dayText = "";
        }
        recycleStaticLayer();
    }

    private void ensureStaticLayer() {
        if (staticLayer != null && !staticLayer.isRecycled()) {
            return;
        }
        staticLayer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas layerCanvas = new Canvas(staticLayer);
        drawStaticComposition(layerCanvas);
    }

    private void drawStaticComposition(Canvas canvas) {
        String theme = config.getThemeId();
        backgroundPaint.setShader(new LinearGradient(
                0f, 0f, width, height,
                new int[]{ClockStudioTheme.startColor(theme),
                        ClockStudioTheme.centerColor(theme), ClockStudioTheme.endColor(theme)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, backgroundPaint);
        backgroundPaint.setShader(null);

        float safeLeft = width * 0.07f;
        float safeRight = width * 0.93f;
        float safeTop = height * 0.075f;
        float safeBottom = height * 0.925f;
        float safeWidth = safeRight - safeLeft;
        float safeHeight = safeBottom - safeTop;

        drawName(canvas, safeLeft, safeRight, safeTop, safeHeight);
        if (dateAvailable) {
            drawDatePanel(canvas, safeLeft, safeRight, safeBottom, safeHeight);
        }
        // Keep the outer edge quiet and recognizable without drawing over the Arabic composition.
        dividerPaint.setColor(Color.argb(105,
                Color.red(ClockStudioTheme.accentColor(theme)),
                Color.green(ClockStudioTheme.accentColor(theme)),
                Color.blue(ClockStudioTheme.accentColor(theme))));
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(Math.max(1f, width * 0.0015f));
        canvas.drawRoundRect(safeLeft, safeTop, safeRight, safeBottom,
                width * 0.025f, width * 0.025f, dividerPaint);
    }

    private void drawName(Canvas canvas, float safeLeft, float safeRight,
                          float safeTop, float safeHeight) {
        float centerX = (safeLeft + safeRight) / 2f;
        float nameBandHeight = safeHeight * 0.25f;
        float numberSize = Math.max(12f, Math.min(width * 0.026f, height * 0.018f));
        float nameSize = Math.max(24f, Math.min(width * 0.09f, height * 0.055f));
        if (config.getName().getArabicName().length() > 14) {
            nameSize *= 0.78f;
        }
        namePaint.setTextSize(nameSize);
        float maxNameWidth = (safeRight - safeLeft) * 0.88f;
        while (namePaint.measureText(config.getName().getArabicName()) > maxNameWidth
                && nameSize > 18f) {
            nameSize -= 1f;
            namePaint.setTextSize(nameSize);
        }
        numberPaint.setTextSize(numberSize);
        transliterationPaint.setTextSize(Math.max(12f, nameSize * 0.30f));

        float numberBaseline = safeTop + nameBandHeight * 0.22f;
        float nameBaseline = safeTop + nameBandHeight * 0.62f;
        float transliterationBaseline = safeTop + nameBandHeight * 0.88f;
        canvas.drawText(context.getString(R.string.allah_name_number,
                config.getName().getNumber()), centerX, numberBaseline, numberPaint);
        canvas.drawText(config.getName().getArabicName(), centerX, nameBaseline, namePaint);
        canvas.drawText(config.getName().getTransliteration(), centerX,
                transliterationBaseline, transliterationPaint);

        dividerPaint.setColor(ClockStudioTheme.accentColor(config.getThemeId()));
        dividerPaint.setStrokeWidth(Math.max(1f, width * 0.002f));
        float dividerWidth = safeWidth * 0.16f;
        canvas.drawLine(centerX - dividerWidth, transliterationBaseline + nameSize * 0.22f,
                centerX + dividerWidth, transliterationBaseline + nameSize * 0.22f, dividerPaint);
    }

    private void drawDatePanel(Canvas canvas, float safeLeft, float safeRight,
                               float safeBottom, float safeHeight) {
        float panelHeight = safeHeight * 0.105f;
        float panelTop = safeBottom - panelHeight;
        datePanelBounds.set(safeLeft + (safeRight - safeLeft) * 0.04f,
                panelTop, safeRight - (safeRight - safeLeft) * 0.04f, safeBottom);
        boolean neon = config.getStyle().usesNeonTreatment();
        boolean glass = config.getStyle().usesGlassTreatment();
        boolean gold = "hybrid_gold_analog".equals(config.getStyle().getId())
                || "hybrid_luxury".equals(config.getStyle().getId())
                || "analog_luxury_gold".equals(config.getStyle().getId())
                || "digital_luxury_gold".equals(config.getStyle().getId());
        panelPaint.setStyle(Paint.Style.FILL);
        if (glass) {
            panelPaint.setColor("glass_light".equals(config.getStyle().getId())
                    ? Color.argb(150, 242, 247, 244) : Color.argb(145, 7, 15, 21));
        } else if (neon) {
            panelPaint.setColor(Color.argb(74, 2, 9, 19));
        } else if (gold) {
            panelPaint.setColor(Color.argb(115, 33, 24, 10));
        } else {
            panelPaint.setColor(Color.argb(100, 8, 24, 32));
        }
        canvas.drawRoundRect(datePanelBounds, width * 0.02f, width * 0.02f, panelPaint);
        panelPaint.setStyle(Paint.Style.STROKE);
        panelPaint.setStrokeWidth(Math.max(1f, width * 0.0015f));
        panelPaint.setColor(neon ? config.getStyle().getPreviewColor()
                : gold ? Color.rgb(216, 177, 90) : ClockStudioTheme.accentColor(config.getThemeId()));
        canvas.drawRoundRect(datePanelBounds, width * 0.02f, width * 0.02f, panelPaint);

        float lineHeight = panelHeight * 0.22f;
        float centerX = (safeLeft + safeRight) / 2f;
        float baseline = panelTop + lineHeight * 1.10f;
        if (config.showDay()) {
            mutedDatePaint.setTextSize(Math.max(12f, width * 0.022f));
            mutedDatePaint.setColor(gold || neon ? panelPaint.getColor() : Color.rgb(242, 212, 141));
            canvas.drawText(dayText, centerX, baseline, mutedDatePaint);
            baseline += lineHeight;
        }
        if (config.showHijri()) {
            datePaint.setTextSize(Math.max(14f, width * 0.027f));
            datePaint.setColor(neon ? config.getStyle().getPreviewColor() : Color.rgb(255, 248, 231));
            canvas.drawText(hijriText, centerX, baseline, datePaint);
            baseline += lineHeight;
        }
        if (config.showGregorian()) {
            mutedDatePaint.setTextSize(Math.max(11f, width * 0.020f));
            mutedDatePaint.setColor(Color.rgb(214, 201, 149));
            canvas.drawText(gregorianText, centerX, baseline, mutedDatePaint);
        }
    }

    private void layoutClocks() {
        float safeLeft = width * 0.07f;
        float safeRight = width * 0.93f;
        float safeTop = height * 0.075f;
        float safeBottom = height * 0.925f;
        float safeWidth = safeRight - safeLeft;
        float safeHeight = safeBottom - safeTop;
        float nameBottom = safeTop + safeHeight * 0.27f;
        float dateTop = dateAvailable ? safeBottom - safeHeight * 0.13f : safeBottom;
        float regionTop = nameBottom + safeHeight * 0.035f;
        float regionBottom = dateTop - safeHeight * 0.035f;
        float regionHeight = Math.max(1f, regionBottom - regionTop);
        float secondaryHeight = secondaryClock == null ? 0f : regionHeight * 0.17f;
        float primaryBottom = regionBottom - secondaryHeight;
        float primaryHeight = Math.max(1f, primaryBottom - regionTop);
        float factor = config.getSize() == ClockStudioWallpaperConfig.SIZE_SMALL ? 0.58f
                : config.getSize() == ClockStudioWallpaperConfig.SIZE_LARGE ? 0.94f : 0.76f;

        float centerFraction = config.getPosition() == ClockStudioWallpaperConfig.POSITION_TOP
                ? 0.24f : config.getPosition() == ClockStudioWallpaperConfig.POSITION_BOTTOM
                ? 0.76f : 0.50f;
        float centerY = regionTop + primaryHeight * centerFraction;
        if (activeClock instanceof AnalogClockView) {
            float side = Math.max(1f, Math.min(safeWidth, primaryHeight) * factor);
            clockBounds.set((width - side) / 2f, centerY - side / 2f,
                    (width + side) / 2f, centerY + side / 2f);
        } else {
            float clockHeight = Math.min(primaryHeight,
                    Math.max(height * 0.06f, primaryHeight * factor * 0.48f));
            float clockWidth = Math.min(safeWidth,
                    Math.max(width * 0.46f, clockHeight * 2.55f));
            clockBounds.set((width - clockWidth) / 2f, centerY - clockHeight / 2f,
                    (width + clockWidth) / 2f, centerY + clockHeight / 2f);
        }
        // Position is a preference, not permission to let the dial or panel leave its safe band.
        fitInsideVerticalBand(clockBounds, regionTop, primaryBottom);
        if (secondaryClock != null) {
            float secondaryWidth = Math.min(safeWidth, Math.max(width * 0.44f, secondaryHeight * 3f));
            secondaryBounds.set((width - secondaryWidth) / 2f,
                    regionBottom - secondaryHeight, (width + secondaryWidth) / 2f, regionBottom);
        }
        layoutView(activeClock, clockBounds);
        if (secondaryClock != null) {
            layoutView(secondaryClock, secondaryBounds);
        }
        laidOut = true;
    }

    private void fitInsideVerticalBand(RectF bounds, float bandTop, float bandBottom) {
        float bandHeight = Math.max(1f, bandBottom - bandTop);
        float clockHeight = Math.min(bounds.height(), bandHeight);
        float top = bounds.top;
        if (top < bandTop) {
            top = bandTop;
        }
        if (top + clockHeight > bandBottom) {
            top = bandBottom - clockHeight;
        }
        bounds.set(bounds.left, top, bounds.right, top + clockHeight);
    }

    private void layoutView(View view, RectF bounds) {
        int left = Math.round(bounds.left);
        int top = Math.round(bounds.top);
        int right = Math.round(bounds.right);
        int bottom = Math.round(bounds.bottom);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(Math.max(1, right - left),
                View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(Math.max(1, bottom - top),
                View.MeasureSpec.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(left, top, right, bottom);
    }

    private void drawClock(Canvas canvas, View view, RectF bounds) {
        int alpha = Math.round(config.getClockOpacity() * 255f);
        if (alpha >= 255) {
            canvas.save();
            canvas.translate(bounds.left, bounds.top);
            view.draw(canvas);
            canvas.restore();
            return;
        }
        int save = canvas.saveLayerAlpha(bounds, alpha);
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        view.draw(canvas);
        canvas.restore();
        canvas.restoreToCount(save);
    }

    private void recycleStaticLayer() {
        if (staticLayer != null && !staticLayer.isRecycled()) {
            staticLayer.recycle();
        }
        staticLayer = null;
    }
}
