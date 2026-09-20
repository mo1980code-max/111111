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

    private static final long FRAME_DELAY_MS = 200L;

    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (running) {
                postInvalidateOnAnimation();
                handler.postDelayed(this, FRAME_DELAY_MS);
            }
        }
    };

    private ClockStyle style = ClockStyleRegistry.defaultStyle();
    private boolean twentyFourHour;
    private boolean showSeconds = true;
    private boolean running;

    public DigitalClockView(Context context) {
        super(context);
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setStyle(@NonNull ClockStyle style) {
        this.style = style;
        invalidate();
    }

    public void setTwentyFourHour(boolean twentyFourHour) {
        this.twentyFourHour = twentyFourHour;
        invalidate();
    }

    public void setShowSeconds(boolean showSeconds) {
        this.showSeconds = showSeconds;
        invalidate();
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

    @Override
    protected void onDetachedFromWindow() {
        stopClock();
        super.onDetachedFromWindow();
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
        RectF panel = new RectF(left, top, right, bottom);

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
        Typeface face = typefaceForStyle();
        textPaint.clearShadowLayer();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(face);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(timeColor);
        textPaint.setLetterSpacing(letterSpacing());
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
            Calendar now = Calendar.getInstance();
            String meridiem = now.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            smallTextPaint.clearShadowLayer();
            smallTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
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

    @NonNull
    private String formattedTime() {
        Calendar now = Calendar.getInstance();
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

    private Typeface typefaceForStyle() {
        if ("digital_elegant_thin".equals(style.getId())) {
            return Typeface.create("sans-serif-light", Typeface.NORMAL);
        }
        if ("digital_led".equals(style.getId())
                || "digital_seven_segment".equals(style.getId())) {
            return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        }
        if ("digital_modern_bold".equals(style.getId())
                || "digital_luxury_gold".equals(style.getId())
                || style.usesNeonTreatment()) {
            return Typeface.create("sans-serif", Typeface.BOLD);
        }
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    private float letterSpacing() {
        if ("digital_elegant_thin".equals(style.getId())) {
            return 0.045f;
        }
        if ("digital_seven_segment".equals(style.getId())) {
            return 0.02f;
        }
        return 0f;
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
