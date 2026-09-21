package com.clock.livewallpaper.clock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.clock.livewallpaper.model.Clocks;
import com.clock.livewallpaper.utils.GetClocks;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shared, allocation-light Canvas renderer for the Clock Studio composition.
 *
 * <p>Static resources (background bitmap, theme shader, Arabic typeface and analog art) are loaded
 * when the configuration or surface changes.  A frame only updates clock hands/text and cached date
 * strings; no bitmap is decoded and no Paint is created from the wallpaper tick.
 */
public final class ClockCompositionRenderer {
    private static final int[][] THEME_COLORS = {
            {Color.rgb(8, 13, 20), Color.rgb(26, 45, 67)},
            {Color.rgb(8, 35, 30), Color.rgb(16, 82, 67)},
            {Color.rgb(30, 17, 45), Color.rgb(74, 35, 88)},
            {Color.rgb(42, 27, 15), Color.rgb(108, 70, 25)},
            {Color.rgb(15, 22, 42), Color.rgb(37, 64, 107)},
            {Color.rgb(30, 30, 30), Color.rgb(72, 72, 72)}
    };

    private final Context context;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final TextPaint datePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final TextPaint clockPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect sourceRect = new Rect();
    private final Rect destinationRect = new Rect();
    private final RectF roundRect = new RectF();

    private Typeface arabicTypeface;
    private Typeface digitalTypeface;
    private ClockStudioConfig config = ClockStudioConfig.safeDefault();
    private int width;
    private int height;
    private int safeTop;
    private int safeBottom;
    private int textAlpha = 255;
    private Bitmap backgroundBitmap;
    private Drawable analogFace;
    private Drawable hourHand;
    private Drawable minuteHand;
    private Drawable secondHand;
    private String cachedDateKey = "";
    private String cachedHijri = "";
    private String cachedGregorian = "";
    private String cachedDay = "";

    public ClockCompositionRenderer(Context context) {
        this.context = context.getApplicationContext();
        this.arabicTypeface = loadTypeface("fonts/cairo_regular.ttf", Typeface.DEFAULT);
        this.digitalTypeface = loadTypeface("jetbrains_mono_medium.ttf", Typeface.MONOSPACE);
        this.namePaint.setTextAlign(Paint.Align.CENTER);
        this.datePaint.setTextAlign(Paint.Align.CENTER);
        this.clockPaint.setTextAlign(Paint.Align.CENTER);
        this.namePaint.setTypeface(this.arabicTypeface);
        this.datePaint.setTypeface(this.arabicTypeface);
        this.clockPaint.setTypeface(this.digitalTypeface);
        this.ringPaint.setStyle(Paint.Style.STROKE);
    }

    public void setConfiguration(ClockStudioConfig value, int width, int height) {
        this.config = value == null ? ClockStudioConfig.safeDefault() : value;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.safeTop = Math.round(this.height * 0.08f);
        this.safeBottom = Math.round(this.height * 0.92f);
        this.textAlpha = ClockStudioConfig.clamp(Math.round(this.config.opacity * 255.0f), 12, 255);
        this.cachedDateKey = "";
        loadBackground();
        loadAnalogArt();
        buildThemeShader();
    }

    public void release() {
        if (this.backgroundBitmap != null && !this.backgroundBitmap.isRecycled()) {
            this.backgroundBitmap.recycle();
        }
        this.backgroundBitmap = null;
        this.analogFace = null;
        this.hourHand = null;
        this.minuteHand = null;
        this.secondHand = null;
    }

    public void draw(Canvas canvas, Calendar now) {
        if (canvas == null) {
            return;
        }
        Calendar time = now == null ? Calendar.getInstance() : now;
        drawBackground(canvas);

        float minimum = Math.min(this.width, this.height);
        float diameter = ClockStudioConfig.clamp(minimum * this.config.sizeFraction,
                minimum * 0.12f, minimum * 0.72f);
        float radius = diameter / 2.0f;
        float centerX = clamp(this.config.positionX * this.width, radius + this.width * 0.05f,
                this.width - radius - this.width * 0.05f);
        float centerY = clamp(this.config.positionY * this.height, this.safeTop + radius,
                this.safeBottom - radius);

        drawClock(canvas, time, centerX, centerY, radius);
        drawNameAndDates(canvas, time, centerX, centerY, radius);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, this.width, this.height, this.backgroundPaint);
        if (this.backgroundBitmap == null) {
            return;
        }
        int bitmapWidth = this.backgroundBitmap.getWidth();
        int bitmapHeight = this.backgroundBitmap.getHeight();
        if (bitmapWidth <= 0 || bitmapHeight <= 0) {
            return;
        }
        float scale = Math.max(this.width / (float) bitmapWidth, this.height / (float) bitmapHeight);
        int cropWidth = Math.min(bitmapWidth, Math.round(this.width / scale));
        int cropHeight = Math.min(bitmapHeight, Math.round(this.height / scale));
        int left = Math.max(0, (bitmapWidth - cropWidth) / 2);
        int top = Math.max(0, (bitmapHeight - cropHeight) / 2);
        this.sourceRect.set(left, top, left + cropWidth, top + cropHeight);
        this.destinationRect.set(0, 0, this.width, this.height);
        this.overlayPaint.setAlpha(255);
        canvas.drawBitmap(this.backgroundBitmap, this.sourceRect, this.destinationRect, this.overlayPaint);
        // A small stable scrim preserves legibility without doing a real-time blur.
        this.overlayPaint.setColor(Color.argb(54, 0, 0, 0));
        canvas.drawRect(0, 0, this.width, this.height, this.overlayPaint);
    }

    private void drawClock(Canvas canvas, Calendar now, float centerX, float centerY, float radius) {
        String style = this.config.clockStyleId.toLowerCase(Locale.US);
        boolean analog = this.config.clockType == 0 || style.startsWith("analog") || style.contains("analog");
        if (analog) {
            drawAnalog(canvas, now, centerX, centerY, radius);
        } else {
            drawDigital(canvas, now, centerX, centerY, radius, style);
        }
    }

    private void drawAnalog(Canvas canvas, Calendar now, float centerX, float centerY, float radius) {
        if (this.analogFace == null) {
            drawFallbackDial(canvas, centerX, centerY, radius);
        } else {
            drawScaledDrawable(canvas, this.analogFace, centerX, centerY, radius * 2.0f, textAlpha);
        }

        float seconds = this.config.showSeconds
                ? now.get(Calendar.SECOND) + now.get(Calendar.MILLISECOND) / 1000.0f
                : 0.0f;
        float minuteAngle = (now.get(Calendar.MINUTE) + seconds / 60.0f) * 6.0f;
        float hourAngle = ((now.get(Calendar.HOUR) % 12) + now.get(Calendar.MINUTE) / 60.0f
                + seconds / 3600.0f) * 30.0f;
        drawHand(canvas, this.hourHand, centerX, centerY, radius * 2.0f, hourAngle);
        drawHand(canvas, this.minuteHand, centerX, centerY, radius * 2.0f, minuteAngle);
        if (this.config.showSeconds && this.secondHand != null) {
            drawHand(canvas, this.secondHand, centerX, centerY, radius * 2.0f, seconds * 6.0f);
        }
    }

    private void drawFallbackDial(Canvas canvas, float centerX, float centerY, float radius) {
        this.ringPaint.setStyle(Paint.Style.STROKE);
        this.ringPaint.setStrokeWidth(Math.max(2.0f, radius * 0.018f));
        this.ringPaint.setColor(withAlpha(this.config.clockColor, textAlpha));
        canvas.drawCircle(centerX, centerY, radius, this.ringPaint);
        this.ringPaint.setStyle(Paint.Style.FILL);
        for (int mark = 0; mark < 12; mark++) {
            double angle = Math.toRadians(mark * 30.0 - 90.0);
            float x = centerX + (float) Math.cos(angle) * radius * 0.86f;
            float y = centerY + (float) Math.sin(angle) * radius * 0.86f;
            canvas.drawCircle(x, y, Math.max(2.0f, radius * 0.025f), this.ringPaint);
        }
    }

    private void drawHand(Canvas canvas, Drawable hand, float centerX, float centerY,
                          float diameter, float angle) {
        if (hand == null) {
            return;
        }
        int max = Math.max(1, Math.max(hand.getIntrinsicWidth(), hand.getIntrinsicHeight()));
        float scale = diameter / max;
        int handWidth = Math.max(1, Math.round(hand.getIntrinsicWidth() * scale));
        int handHeight = Math.max(1, Math.round(hand.getIntrinsicHeight() * scale));
        int left = Math.round(centerX - handWidth / 2.0f);
        int top = Math.round(centerY - handHeight / 2.0f);
        canvas.save();
        canvas.rotate(angle, centerX, centerY);
        hand.setBounds(left, top, left + handWidth, top + handHeight);
        hand.setAlpha(textAlpha);
        hand.draw(canvas);
        hand.setAlpha(255);
        canvas.restore();
    }

    private void drawDigital(Canvas canvas, Calendar now, float centerX, float centerY,
                             float radius, String style) {
        int color = this.config.clockColor == Color.WHITE ? this.config.textColor1 : this.config.clockColor;
        if (color == 0) {
            color = Color.WHITE;
        }
        boolean neon = style.startsWith("neon") || style.contains("neon");
        boolean glass = style.startsWith("glass") || style.contains("glass");
        boolean luxury = style.startsWith("luxury") || style.contains("luxury");
        boolean hybrid = style.startsWith("hybrid") || style.contains("hybrid");
        if (glass || hybrid) {
            this.overlayPaint.setColor(Color.argb(48, 255, 255, 255));
            this.overlayPaint.setStyle(Paint.Style.FILL);
            this.roundRect.set(centerX - radius * 1.15f, centerY - radius * 0.62f,
                    centerX + radius * 1.15f, centerY + radius * 0.62f);
            canvas.drawRoundRect(this.roundRect, radius * 0.16f, radius * 0.16f, this.overlayPaint);
            this.ringPaint.setColor(Color.argb(115, 255, 255, 255));
            this.ringPaint.setStrokeWidth(Math.max(1.0f, radius * 0.012f));
            this.ringPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(this.roundRect, radius * 0.16f, radius * 0.16f, this.ringPaint);
        }
        if (hybrid) {
            this.ringPaint.setColor(withAlpha(color, Math.min(textAlpha, 180)));
            this.ringPaint.setStrokeWidth(Math.max(2.0f, radius * 0.025f));
            canvas.drawCircle(centerX, centerY, radius * 0.92f, this.ringPaint);
        }

        String time = timeText(now);
        this.clockPaint.setTypeface(this.digitalTypeface);
        this.clockPaint.setTextSize(Math.max(22.0f, radius * (this.config.showSeconds ? 0.52f : 0.60f)));
        this.clockPaint.setColor(withAlpha(color, textAlpha));
        this.clockPaint.setStyle(Paint.Style.FILL);
        if (neon) {
            this.clockPaint.setShadowLayer(radius * 0.12f, 0, 0, withAlpha(color, 190));
        } else {
            this.clockPaint.clearShadowLayer();
        }
        canvas.drawText(time, centerX, centerY + this.clockPaint.getTextSize() * 0.34f, this.clockPaint);
        this.clockPaint.clearShadowLayer();
        if (luxury) {
            this.ringPaint.setColor(withAlpha(Color.rgb(218, 175, 74), textAlpha));
            this.ringPaint.setStrokeWidth(Math.max(2.0f, radius * 0.018f));
            this.ringPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(centerX, centerY, radius * 0.86f, this.ringPaint);
        }
    }

    private String timeText(Calendar now) {
        int hour = this.config.is24Hour ? now.get(Calendar.HOUR_OF_DAY) : now.get(Calendar.HOUR);
        if (!this.config.is24Hour && hour == 0) {
            hour = 12;
        }
        StringBuilder builder = new StringBuilder(12);
        appendTwoDigits(builder, hour);
        builder.append(':');
        appendTwoDigits(builder, now.get(Calendar.MINUTE));
        if (this.config.showSeconds) {
            builder.append(':');
            appendTwoDigits(builder, now.get(Calendar.SECOND));
        }
        if (!this.config.is24Hour) {
            builder.append(now.get(Calendar.AM_PM) == Calendar.AM ? " AM" : " PM");
        }
        return builder.toString();
    }

    private static void appendTwoDigits(StringBuilder builder, int value) {
        if (value < 10) {
            builder.append('0');
        }
        builder.append(value);
    }

    private void drawNameAndDates(Canvas canvas, Calendar now, float centerX, float centerY, float radius) {
        float nameSize = Math.max(20.0f, Math.min(this.width, this.height) * 0.062f);
        float nameY = centerY - radius - nameSize * 0.55f;
        if (nameY < this.safeTop + nameSize) {
            nameY = centerY + radius + nameSize * 1.25f;
        }
        nameY = clamp(nameY, this.safeTop + nameSize, this.safeBottom - nameSize);
        this.namePaint.setTypeface(this.arabicTypeface);
        this.namePaint.setTextSize(nameSize);
        this.namePaint.setColor(withAlpha(this.config.clockColor, textAlpha));
        this.namePaint.setStyle(Paint.Style.FILL);
        canvas.drawText(AllahNameCatalog.getArabicName(this.config.nameId), centerX, nameY, this.namePaint);

        ensureDateStrings(now);
        float dateY = nameY + nameSize * 0.95f;
        if (dateY > this.safeBottom - nameSize * 0.35f) {
            dateY = nameY - nameSize * 0.32f;
        }
        this.datePaint.setTextSize(Math.max(13.0f, nameSize * 0.34f));
        this.datePaint.setColor(withAlpha(this.config.clockColor, Math.max(120, textAlpha - 30)));
        String date = dateText();
        if (!date.isEmpty()) {
            canvas.drawText(date, centerX, dateY, this.datePaint);
        }
    }

    private String dateText() {
        String gregorian = this.config.showDayName
                ? this.cachedDay + "  " + this.cachedGregorian : this.cachedGregorian;
        if (this.config.showHijriDate && this.config.showGregorianDate) {
            return this.cachedHijri + "  •  " + gregorian;
        }
        if (this.config.showHijriDate) {
            return this.cachedHijri;
        }
        if (this.config.showGregorianDate) {
            return gregorian;
        }
        return this.config.showDayName ? this.cachedDay : "";
    }

    private void ensureDateStrings(Calendar now) {
        String key = now.get(Calendar.ERA) + ":" + now.get(Calendar.YEAR) + ":"
                + now.get(Calendar.DAY_OF_YEAR) + ":" + this.config.hijriAdjustment;
        if (key.equals(this.cachedDateKey)) {
            return;
        }
        this.cachedDateKey = key;
        HijriDateProvider.HijriDate hijri = HijriDateProvider.from(now, this.config.hijriAdjustment);
        Locale locale = Locale.getDefault();
        this.cachedHijri = hijri.format(locale);
        this.cachedGregorian = new SimpleDateFormat("dd MMM yyyy", locale).format(new Date(now.getTimeInMillis()));
        this.cachedDay = new SimpleDateFormat("EEEE", locale).format(new Date(now.getTimeInMillis()));
    }

    private void buildThemeShader() {
        int theme = ClockStudioConfig.clamp(this.config.themeId, 1, THEME_COLORS.length) - 1;
        int[] colors = THEME_COLORS[theme];
        if (!this.config.imageBackground && !this.config.customBackground
                && this.config.backgroundColor != Color.BLACK && this.config.backgroundColor != 0) {
            int base = this.config.backgroundColor;
            colors = new int[]{base, Color.rgb(Math.min(255, Color.red(base) + 34),
                    Math.min(255, Color.green(base) + 34), Math.min(255, Color.blue(base) + 34))};
        }
        this.backgroundPaint.setShader(new LinearGradient(0, 0, this.width, this.height,
                colors[0], colors[1], Shader.TileMode.CLAMP));
        this.backgroundPaint.setColor(colors[0]);
    }

    private void loadBackground() {
        if (this.backgroundBitmap != null && !this.backgroundBitmap.isRecycled()) {
            this.backgroundBitmap.recycle();
        }
        this.backgroundBitmap = null;
        try {
            if (this.config.imageBackground && !this.config.backgroundPath.isEmpty()) {
                this.backgroundBitmap = BitmapFactory.decodeFile(this.config.backgroundPath);
            } else if (this.config.customBackground && this.config.backgroundResource != 0) {
                this.backgroundBitmap = BitmapFactory.decodeResource(this.context.getResources(),
                        this.config.backgroundResource);
            }
        } catch (RuntimeException ignored) {
            this.backgroundBitmap = null;
        }
    }

    private void loadAnalogArt() {
        this.analogFace = null;
        this.hourHand = null;
        this.minuteHand = null;
        this.secondHand = null;
        String id = this.config.clockStyleId;
        if (!(this.config.clockType == 0 || id.startsWith("analog") || id.contains("analog"))) {
            return;
        }
        try {
            List<Clocks> clocks = GetClocks.analogClocks();
            Clocks selected = clocks.get(0);
            for (Clocks clock : clocks) {
                if (id.equals(clock.getId())) {
                    selected = clock;
                    break;
                }
            }
            this.analogFace = this.context.getResources().getDrawable(selected.getBackroundImage());
            this.hourHand = this.context.getResources().getDrawable(selected.getHourHand());
            this.minuteHand = this.context.getResources().getDrawable(selected.getMinuteHand());
            this.secondHand = this.context.getResources().getDrawable(selected.getSecondHand());
        } catch (RuntimeException ignored) {
            this.analogFace = null;
            this.hourHand = null;
            this.minuteHand = null;
            this.secondHand = null;
        }
    }

    private void drawScaledDrawable(Canvas canvas, Drawable drawable, float centerX, float centerY,
                                    float diameter, int alpha) {
        int max = Math.max(1, Math.max(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()));
        float scale = diameter / max;
        int drawableWidth = Math.max(1, Math.round(drawable.getIntrinsicWidth() * scale));
        int drawableHeight = Math.max(1, Math.round(drawable.getIntrinsicHeight() * scale));
        int left = Math.round(centerX - drawableWidth / 2.0f);
        int top = Math.round(centerY - drawableHeight / 2.0f);
        drawable.setBounds(left, top, left + drawableWidth, top + drawableHeight);
        drawable.setAlpha(alpha);
        drawable.draw(canvas);
        drawable.setAlpha(255);
    }

    private Typeface loadTypeface(String asset, Typeface fallback) {
        try {
            return Typeface.createFromAsset(this.context.getAssets(), asset);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(ClockStudioConfig.clamp(alpha, 0, 255), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
