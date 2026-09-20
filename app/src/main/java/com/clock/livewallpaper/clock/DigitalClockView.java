package com.clock.livewallpaper.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/** A reusable Canvas-rendered live digital clock with multiple premium treatments. */
public final class DigitalClockView extends View {

    private static final long SMOOTH_FRAME_DELAY_MS = 250L;
    private static final long MIN_FRAME_DELAY_MS = 250L;

    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private final Calendar time = Calendar.getInstance();
    private final Typeface meridiemTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (running) {
                postInvalidateOnAnimation();
                handler.postDelayed(this, nextFrameDelayMs());
            }
        }
    };

    private ClockStyle style = ClockStyleRegistry.defaultStyle();
    private int accentColor;
    private boolean hasAccentColor;
    private boolean twentyFourHour;
    private boolean showSeconds = true;
    private boolean running;
    private Typeface clockTypeface;
    private float clockLetterSpacing;

    public DigitalClockView(Context context) {
        super(context);
        setWillNotDraw(false);
        textPaint.setSubpixelText(true);
        smallTextPaint.setSubpixelText(true);
        panelPaint.setDither(true);
        updateStyleTypography();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setStyle(@NonNull ClockStyle style) {
        this.style = style;
        updateStyleTypography();
        invalidate();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        this.hasAccentColor = true;
        invalidate();
    }

    public void setTwentyFourHour(boolean twentyFourHour) {
        this.twentyFourHour = twentyFourHour;
        invalidate();
    }

    public void setShowSeconds(boolean showSeconds) {
        this.showSeconds = showSeconds;
        if (running) {
            handler.removeCallbacks(ticker);
            ticker.run();
        } else {
            invalidate();
        }
    }

    public void startClock() {
        if (running) {
            return;
        }
        running = true;
        handler.removeCallbacks(ticker);
        ticker.run();
    }

    public void stopClock() {
        running = false;
        handler.removeCallbacks(ticker);
    }

    /** Digital text only needs the next visible time boundary, never a free-running animation loop. */
    private long nextFrameDelayMs() {
        if (showSeconds) {
            return SMOOTH_FRAME_DELAY_MS;
        }
        Calendar now = Calendar.getInstance();
        long untilMinute = 60_000L - now.get(Calendar.SECOND) * 1_000L
                - now.get(Calendar.MILLISECOND) + 40L;
        return Math.max(MIN_FRAME_DELAY_MS, untilMinute);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopClock();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) {
            stopClock();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            stopClock();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        boolean neon = style.usesNeonTreatment();
        boolean glass = style.usesGlassTreatment();
        int accent = style.getPreviewColor();
        float left = getWidth() * 0.035f;
        float top = getHeight() * 0.12f;
        float right = getWidth() * 0.965f;
        float bottom = getHeight() * 0.88f;
        panel.set(left, top, right, bottom);

        panelPaint.clearShadowLayer();
        panelPaint.setStyle(Paint.Style.FILL);
        if (glass) {
            panelPaint.setColor("glass_light".equals(style.getId())
                    ? Color.argb(150, 242, 247, 244)
                    : Color.argb(145, 7, 15, 21));
            panelPaint.setShadowLayer(12f, 0f, 5f, Color.argb(90, 0, 0, 0));
            canvas.drawRoundRect(panel, 22f, 22f, panelPaint);
            panelPaint.clearShadowLayer();
            panelPaint.setStyle(Paint.Style.STROKE);
            panelPaint.setStrokeWidth(1.5f);
            panelPaint.setColor("glass_light".equals(style.getId())
                    ? Color.argb(180, 255, 255, 255)
                    : Color.argb(190, 214, 235, 226));
            canvas.drawRoundRect(panel, 22f, 22f, panelPaint);
        } else if (!"digital_minimal_white".equals(style.getId())) {
            int panelColor;
            if (neon) {
                panelColor = Color.argb(92, 2, 9, 19);
            } else if ("digital_luxury_gold".equals(style.getId())) {
                panelColor = Color.argb(188, 27, 19, 10);
            } else if ("digital_led".equals(style.getId())
                    || "digital_seven_segment".equals(style.getId())) {
                panelColor = Color.argb(232, 2, 8, 11);
            } else {
                panelColor = Color.argb(125, 8, 22, 32);
            }
            panelPaint.setColor(panelColor);
            canvas.drawRoundRect(panel, 18f, 18f, panelPaint);
        }

        String time = formattedTime();
        int timeColor = timeColor(accent);
        textPaint.clearShadowLayer();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(clockTypeface);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(timeColor);
        textPaint.setLetterSpacing(clockLetterSpacing);
        float availableWidth = panel.width() * 0.90f;
        float textSize = Math.min(getHeight() * 0.40f, getWidth() * 0.22f);
        textPaint.setTextSize(textSize);
        while (textPaint.measureText(time) > availableWidth && textSize > 12f) {
            textSize -= 1f;
            textPaint.setTextSize(textSize);
        }
        if (neon || "digital_luxury_gold".equals(style.getId())) {
            textPaint.setShadowLayer(14f, 0f, 0f, timeColor);
        }
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(time, getWidth() / 2f, baseline, textPaint);

        if (!twentyFourHour) {
            Calendar now = currentTime();
            String meridiem = now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            smallTextPaint.clearShadowLayer();
            smallTextPaint.setTypeface(meridiemTypeface);
            smallTextPaint.setTextAlign(Paint.Align.CENTER);
            smallTextPaint.setTextSize(Math.max(11f, getHeight() * 0.085f));
            smallTextPaint.setColor(Color.argb(215, Color.red(timeColor), Color.green(timeColor), Color.blue(timeColor)));
            canvas.drawText(meridiem, getWidth() / 2f, bottom - getHeight() * 0.13f, smallTextPaint);
        }

        if ("digital_led".equals(style.getId())) {
            drawLedBaseline(canvas, panel, timeColor);
        } else if ("digital_seven_segment".equals(style.getId())) {
            drawSegmentDots(canvas, panel, timeColor);
        }
    }

    private Calendar currentTime() {
        time.setTimeInMillis(System.currentTimeMillis());
        return time;
    }

    @NonNull
    private String formattedTime() {
        Calendar now = currentTime();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (!twentyFourHour) {
            hour %= 12;
            if (hour == 0) {
                hour = 12;
            }
        }
        if (showSeconds) {
            return String.format(Locale.US, "%02d:%02d:%02d", hour,
                    now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        }
        return String.format(Locale.US, "%02d:%02d", hour, now.get(Calendar.MINUTE));
    }

    private int timeColor(int accent) {
        if (hasAccentColor) {
            return accentColor;
        }
        if (style.usesNeonTreatment()) {
            return accent;
        }
        if ("digital_luxury_gold".equals(style.getId())) {
            return Color.rgb(242, 204, 116);
        }
        if ("digital_led".equals(style.getId())) {
            return Color.rgb(255, 112, 87);
        }
        if ("digital_seven_segment".equals(style.getId())) {
            return Color.rgb(106, 221, 143);
        }
        if ("glass_light".equals(style.getId())) {
            return Color.rgb(39, 75, 75);
        }
        if ("digital_elegant_thin".equals(style.getId())) {
            return Color.rgb(236, 227, 211);
        }
        return Color.rgb(248, 246, 238);
    }

    /** Style typography is static for the lifetime of this view; do not allocate it per frame. */
    private void updateStyleTypography() {
        if ("digital_elegant_thin".equals(style.getId())) {
            clockTypeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
            clockLetterSpacing = 0.045f;
        } else if ("digital_led".equals(style.getId())
                || "digital_seven_segment".equals(style.getId())) {
            clockTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
            clockLetterSpacing = "digital_seven_segment".equals(style.getId()) ? 0.02f : 0f;
        } else if ("digital_modern_bold".equals(style.getId())
                || "digital_luxury_gold".equals(style.getId())
                || style.usesNeonTreatment()) {
            clockTypeface = Typeface.create("sans-serif", Typeface.BOLD);
            clockLetterSpacing = 0f;
        } else {
            clockTypeface = Typeface.create("sans-serif", Typeface.NORMAL);
            clockLetterSpacing = 0f;
        }
    }

    private void drawLedBaseline(Canvas canvas, RectF panel, int color) {
        panelPaint.clearShadowLayer();
        panelPaint.setStyle(Paint.Style.STROKE);
        panelPaint.setStrokeWidth(1f);
        panelPaint.setColor(Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawRoundRect(panel.left + 14f, panel.bottom - 18f,
                panel.right - 14f, panel.bottom - 16f, 1f, 1f, panelPaint);
    }

    private void drawSegmentDots(Canvas canvas, RectF panel, int color) {
        panelPaint.clearShadowLayer();
        panelPaint.setStyle(Paint.Style.FILL);
        panelPaint.setColor(Color.argb(110, Color.red(color), Color.green(color), Color.blue(color)));
        float y = panel.top + 15f;
        for (int i = 0; i < 17; i++) {
            canvas.drawCircle(panel.left + 15f + i * 7f, y, 1.2f, panelPaint);
        }
    }
}
