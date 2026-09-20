package com.clock.livewallpaper.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Calendar;

/**
 * A single reusable, Canvas-based live analog clock.
 *
 * <p>Only the Clock Studio creates this view. It uses one lightweight ticker and stops it when the
 * host pauses or detaches, so the 99-name gallery never owns 99 ticking clocks.</p>
 */
public final class AnalogClockView extends View {

    private static final long SMOOTH_FRAME_DELAY_MS = 100L;
    private static final long MIN_FRAME_DELAY_MS = 250L;
    private static final String[] CLASSIC_NUMBERS = {
            "12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar time = Calendar.getInstance();
    private final Typeface classicTypeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
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
    private boolean showSeconds = true;
    private boolean running;

    public AnalogClockView(Context context) {
        super(context);
        setWillNotDraw(false);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        textPaint.setSubpixelText(true);
        paint.setDither(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setStyle(@NonNull ClockStyle style) {
        this.style = style;
        invalidate();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        this.hasAccentColor = true;
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

    /** Seconds need a smooth sweep; without seconds the next redraw is scheduled at the next minute. */
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
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(width, height) * 0.43f;
        boolean neon = style.usesNeonTreatment();
        boolean floating = "analog_floating".equals(style.getId());
        boolean luxury = "analog_luxury_gold".equals(style.getId())
                || "hybrid_gold_analog".equals(style.getId())
                || "hybrid_luxury".equals(style.getId());
        int accent = hasAccentColor ? accentColor : style.getPreviewColor();

        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.TRANSPARENT);
        if (!floating) {
            int dialColor;
            if ("analog_classic".equals(style.getId())) {
                dialColor = Color.rgb(244, 236, 216);
            } else if ("analog_silver".equals(style.getId())) {
                dialColor = Color.rgb(218, 225, 231);
            } else if ("analog_minimal".equals(style.getId())) {
                dialColor = Color.argb(150, 7, 21, 34);
            } else {
                dialColor = Color.argb(215, 5, 8, 12);
            }
            paint.setColor(dialColor);
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        if (neon) {
            drawGlowRing(canvas, centerX, centerY, radius, accent);
        } else if (floating) {
            drawRing(canvas, centerX, centerY, radius, Color.argb(100, 242, 212, 141), 1.5f);
        } else if (luxury) {
            // Layered gold rings provide a warm, restrained glow without animation or strobing.
            drawGlowRing(canvas, centerX, centerY, radius, Color.rgb(216, 175, 82));
            drawRing(canvas, centerX, centerY, radius * 0.91f,
                    Color.argb(150, 242, 212, 141), 1.5f);
        } else if ("analog_black_gold".equals(style.getId())) {
            drawRing(canvas, centerX, centerY, radius, Color.rgb(225, 183, 89), 3f);
            drawRing(canvas, centerX, centerY, radius * 0.92f,
                    Color.argb(150, 225, 183, 89), 1f);
        } else if ("analog_silver".equals(style.getId())) {
            drawRing(canvas, centerX, centerY, radius, Color.rgb(245, 248, 250), 3f);
            drawRing(canvas, centerX, centerY, radius * 0.93f,
                    Color.rgb(142, 155, 167), 1f);
        } else {
            drawRing(canvas, centerX, centerY, radius,
                    Color.argb(210, 244, 240, 230), 1.7f);
        }

        drawMarkers(canvas, centerX, centerY, radius, accent, floating, neon);
        drawHands(canvas, centerX, centerY, radius, accent, neon);
    }

    private void drawMarkers(Canvas canvas, float cx, float cy, float radius,
                             int accent, boolean floating, boolean neon) {
        String id = style.getId();
        int markerColor;
        if (hasAccentColor) {
            markerColor = accent;
        } else if (neon || "analog_luxury_gold".equals(id) || "analog_black_gold".equals(id)
                || "hybrid_gold_analog".equals(id) || "hybrid_luxury".equals(id)
                || floating) {
            markerColor = accent;
        } else if ("analog_silver".equals(id)) {
            markerColor = Color.rgb(76, 89, 101);
        } else if ("analog_classic".equals(id)) {
            markerColor = Color.rgb(65, 54, 39);
        } else {
            markerColor = Color.rgb(244, 240, 230);
        }

        int markerCount = "analog_minimal".equals(id) ? 4 : 12;
        for (int index = 0; index < markerCount; index++) {
            int hour = markerCount == 4 ? index * 3 : index;
            double angle = Math.toRadians(hour * 30d - 90d);
            float outer = radius * 0.83f;
            float inner = radius * (hour % 3 == 0 ? 0.72f : 0.77f);
            float startX = cx + (float) Math.cos(angle) * inner;
            float startY = cy + (float) Math.sin(angle) * inner;
            float endX = cx + (float) Math.cos(angle) * outer;
            float endY = cy + (float) Math.sin(angle) * outer;
            paint.clearShadowLayer();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(hour % 3 == 0 ? 3.2f : 1.6f);
            paint.setColor(markerColor);
            if (neon) {
                paint.setShadowLayer(7f, 0f, 0f, markerColor);
            }
            canvas.drawLine(startX, startY, endX, endY, paint);
        }

        if ("analog_classic".equals(id)) {
            textPaint.setColor(Color.rgb(65, 54, 39));
            textPaint.setTextSize(radius * 0.16f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(classicTypeface);
            for (int index = 0; index < 12; index++) {
                double angle = Math.toRadians(index * 30d - 90d);
                float x = cx + (float) Math.cos(angle) * radius * 0.61f;
                float y = cy + (float) Math.sin(angle) * radius * 0.61f
                        - (textPaint.ascent() + textPaint.descent()) / 2f;
                canvas.drawText(CLASSIC_NUMBERS[index], x, y, textPaint);
            }
        }
    }

    private void drawHands(Canvas canvas, float cx, float cy, float radius,
                           int accent, boolean neon) {
        time.setTimeInMillis(System.currentTimeMillis());
        Calendar now = time;
        float milliseconds = now.get(Calendar.MILLISECOND) / 1000f;
        float second = now.get(Calendar.SECOND) + milliseconds;
        float minute = now.get(Calendar.MINUTE) + second / 60f;
        float hour = (now.get(Calendar.HOUR) % 12) + minute / 60f;

        int handColor;
        if (hasAccentColor) {
            handColor = accent;
        } else if (neon) {
            handColor = accent;
        } else if ("analog_classic".equals(style.getId())) {
            handColor = Color.rgb(42, 49, 54);
        } else if ("analog_silver".equals(style.getId())) {
            handColor = Color.rgb(48, 60, 72);
        } else if ("analog_minimal".equals(style.getId())) {
            handColor = Color.rgb(246, 242, 231);
        } else {
            handColor = Color.rgb(231, 191, 96);
        }

        drawHand(canvas, cx, cy, radius * 0.50f, radius * 0.09f, handColor, hour * 30f, radius * 0.08f, neon);
        drawHand(canvas, cx, cy, radius * 0.72f, radius * 0.055f, handColor, minute * 6f, radius * 0.10f, neon);
        if (showSeconds) {
            int secondColor = neon ? accent : Color.rgb(188, 77, 66);
            drawHand(canvas, cx, cy, radius * 0.82f, radius * 0.018f,
                    secondColor, second * 6f, radius * 0.16f, neon);
        }

        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(handColor);
        canvas.drawCircle(cx, cy, radius * 0.065f, paint);
        paint.setColor(showSeconds ? (neon ? accent : Color.rgb(188, 77, 66)) : handColor);
        canvas.drawCircle(cx, cy, radius * 0.025f, paint);
    }

    private void drawHand(Canvas canvas, float cx, float cy, float length, float width,
                          int color, float degrees, float tail, boolean glow) {
        double angle = Math.toRadians(degrees - 90d);
        float tipX = cx + (float) Math.cos(angle) * length;
        float tipY = cy + (float) Math.sin(angle) * length;
        float tailX = cx - (float) Math.cos(angle) * tail;
        float tailY = cy - (float) Math.sin(angle) * tail;
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        if (glow) {
            paint.setShadowLayer(10f, 0f, 0f, color);
        }
        canvas.drawLine(tailX, tailY, tipX, tipY, paint);
    }

    private void drawRing(Canvas canvas, float cx, float cy, float radius, int color, float width) {
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    private void drawGlowRing(Canvas canvas, float cx, float cy, float radius, int color) {
        paint.setStyle(Paint.Style.STROKE);
        float glowWidth = Math.max(2f, radius * 0.035f);
        float blur = Math.max(6f, radius * 0.055f);
        paint.setStrokeWidth(glowWidth);
        paint.setColor(Color.argb(48, Color.red(color), Color.green(color), Color.blue(color)));
        paint.setShadowLayer(blur, 0f, 0f, color);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.clearShadowLayer();
        paint.setStrokeWidth(Math.max(1.5f, radius * 0.012f));
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radius, paint);
    }
}
