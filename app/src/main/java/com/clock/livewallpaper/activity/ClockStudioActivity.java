package com.clock.livewallpaper.activity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.adapter.ClockStyleAdapter;
import com.clock.livewallpaper.catalog.AllahNamesCatalog;
import com.clock.livewallpaper.clock.AnalogClockView;
import com.clock.livewallpaper.clock.ClockStyle;
import com.clock.livewallpaper.clock.ClockStyleRegistry;
import com.clock.livewallpaper.clock.DigitalClockView;
import com.clock.livewallpaper.date.HijriDate;
import com.clock.livewallpaper.date.HijriDateFormatter;
import com.clock.livewallpaper.date.HijriDateProvider;
import com.clock.livewallpaper.model.AllahName;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * One reusable live clock editor for every Name of Allah.
 *
 * <p>The Arabic name owns an upper composition region, the live clock owns a middle region, and
 * the date panel owns the bottom region. Changing styles, dates, or sizes can never cover, rotate,
 * or animate the religious text.</p>
 */
public final class ClockStudioActivity extends AppCompatActivity {

    public static final String EXTRA_NAME_ID = "clock_studio_name_id";
    public static final String EXTRA_NAME_NUMBER = "clock_studio_name_number";

    private static final String PREFS_NAME = "names_allah_clock_studio";
    private static final String KEY_STYLE = "style_id";
    private static final String KEY_POSITION = "clock_position";
    private static final String KEY_SIZE = "clock_size";
    private static final String KEY_24_HOUR = "twenty_four_hour";
    private static final String KEY_SECONDS = "show_seconds";
    private static final String KEY_SHOW_HIJRI = "show_hijri_date";
    private static final String KEY_SHOW_GREGORIAN = "show_gregorian_date";
    private static final String KEY_SHOW_DAY = "show_day_name";
    private static final String KEY_HIJRI_ADJUSTMENT = "hijri_date_adjustment";

    private static final int POSITION_TOP = 0;
    private static final int POSITION_CENTER = 1;
    private static final int POSITION_BOTTOM = 2;
    private static final int SIZE_SMALL = 0;
    private static final int SIZE_MEDIUM = 1;
    private static final int SIZE_LARGE = 2;
    private static final int MIN_HIJRI_ADJUSTMENT = -2;
    private static final int MAX_HIJRI_ADJUSTMENT = 2;

    private final List<ClockStyle> styles = ClockStyleRegistry.getAll();
    private final Handler dateHandler = new Handler(Looper.getMainLooper());
    private final Runnable dateRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (resumed) {
                refreshDates();
                scheduleNextDateRefresh();
            }
        }
    };

    private SharedPreferences preferences;
    private AllahName selectedName;
    private ClockStyle activeStyle;
    private ClockStyleAdapter styleAdapter;

    private FrameLayout preview;
    private FrameLayout clockArea;
    private FrameLayout dateArea;
    private LinearLayout datePanel;
    private View backdrop;
    private View activeClock;
    private View secondaryClock;
    private LinearLayout controls;
    private LinearLayout toolbar;
    private TextView positionButton;
    private TextView sizeButton;
    private TextView formatButton;
    private TextView secondsButton;
    private TextView hijriButton;
    private TextView gregorianButton;
    private TextView dayButton;
    private TextView adjustmentButton;
    private TextView dayText;
    private TextView hijriText;
    private TextView gregorianText;
    private View dateDivider;

    private int clockPosition;
    private int clockSize;
    private int hijriAdjustment;
    private boolean twentyFourHour;
    private boolean showSeconds;
    private boolean showHijri;
    private boolean showGregorian;
    private boolean showDay;
    private boolean fullScreen;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_studio);

        selectedName = readSelectedName();
        if (selectedName == null) {
            finish();
            return;
        }

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        activeStyle = ClockStyleRegistry.byId(
                preferences.getString(KEY_STYLE, ClockStyleRegistry.defaultStyle().getId()));
        clockPosition = clamp(preferences.getInt(KEY_POSITION, POSITION_CENTER), POSITION_TOP, POSITION_BOTTOM);
        clockSize = clamp(preferences.getInt(KEY_SIZE, SIZE_MEDIUM), SIZE_SMALL, SIZE_LARGE);
        twentyFourHour = preferences.contains(KEY_24_HOUR)
                ? preferences.getBoolean(KEY_24_HOUR, false)
                : DateFormat.is24HourFormat(this);
        showSeconds = preferences.getBoolean(KEY_SECONDS, true);
        showHijri = preferences.getBoolean(KEY_SHOW_HIJRI, true);
        showGregorian = preferences.getBoolean(KEY_SHOW_GREGORIAN, false);
        showDay = preferences.getBoolean(KEY_SHOW_DAY, false);
        hijriAdjustment = clamp(preferences.getInt(KEY_HIJRI_ADJUSTMENT, 0),
                MIN_HIJRI_ADJUSTMENT, MAX_HIJRI_ADJUSTMENT);

        bindViews();
        bindName();
        bindOptions();
        bindStyleSelector();
        updateDateVisibility();
        rebuildClock(false);
        refreshDates();
    }

    private void bindViews() {
        preview = findViewById(R.id.clock_studio_preview);
        clockArea = findViewById(R.id.clock_studio_clock_area);
        dateArea = findViewById(R.id.clock_studio_date_area);
        datePanel = findViewById(R.id.clock_studio_date_panel);
        backdrop = findViewById(R.id.clock_studio_theme_backdrop);
        controls = findViewById(R.id.clock_studio_controls);
        toolbar = findViewById(R.id.clock_studio_toolbar);
        positionButton = findViewById(R.id.clock_studio_position_button);
        sizeButton = findViewById(R.id.clock_studio_size_button);
        formatButton = findViewById(R.id.clock_studio_format_button);
        secondsButton = findViewById(R.id.clock_studio_seconds_button);
        hijriButton = findViewById(R.id.clock_studio_hijri_button);
        gregorianButton = findViewById(R.id.clock_studio_gregorian_button);
        dayButton = findViewById(R.id.clock_studio_day_button);
        adjustmentButton = findViewById(R.id.clock_studio_adjustment_button);
        dayText = findViewById(R.id.clock_studio_day);
        hijriText = findViewById(R.id.clock_studio_hijri);
        gregorianText = findViewById(R.id.clock_studio_gregorian);
        dateDivider = findViewById(R.id.clock_studio_date_divider);

        ImageView back = findViewById(R.id.clock_studio_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fullScreen) {
                    setFullScreen(false);
                }
            }
        });
        clockArea.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateClockLayout();
            }
        });
    }

    private void bindName() {
        TextView number = findViewById(R.id.clock_studio_name_number);
        TextView name = findViewById(R.id.clock_studio_name);
        TextView transliteration = findViewById(R.id.clock_studio_name_transliteration);
        number.setText(getString(R.string.allah_name_number, selectedName.getNumber()));
        name.setText(selectedName.getArabicName());
        transliteration.setText(selectedName.getTransliteration());
        try {
            Typeface arabicTypeface = Typeface.createFromAsset(getAssets(), "fonts/cairo_regular.ttf");
            name.setTypeface(Typeface.create(arabicTypeface, Typeface.BOLD));
            hijriText.setTypeface(Typeface.create(arabicTypeface, Typeface.BOLD));
        } catch (RuntimeException ignored) {
            // The platform Arabic face is a safe fallback if the bundled face is unavailable.
        }
        backdrop.setBackgroundResource(themeForName(selectedName.getNumber()));
    }

    private void bindOptions() {
        positionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clockPosition = (clockPosition + 1) % 3;
                saveSettings();
                updateOptionLabels();
                updateClockLayout();
            }
        });
        sizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clockSize = (clockSize + 1) % 3;
                saveSettings();
                updateOptionLabels();
                updateClockLayout();
            }
        });
        formatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twentyFourHour = !twentyFourHour;
                saveSettings();
                updateOptionLabels();
                applyClockSettings();
            }
        });
        secondsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSeconds = !showSeconds;
                saveSettings();
                updateOptionLabels();
                applyClockSettings();
            }
        });
        hijriButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHijri = !showHijri;
                saveSettings();
                updateOptionLabels();
                updateDateVisibility();
                refreshDates();
                scheduleNextDateRefresh();
            }
        });
        gregorianButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGregorian = !showGregorian;
                saveSettings();
                updateOptionLabels();
                updateDateVisibility();
                refreshDates();
                scheduleNextDateRefresh();
            }
        });
        dayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDay = !showDay;
                saveSettings();
                updateOptionLabels();
                updateDateVisibility();
                refreshDates();
                scheduleNextDateRefresh();
            }
        });
        adjustmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hijriAdjustment++;
                if (hijriAdjustment > MAX_HIJRI_ADJUSTMENT) {
                    hijriAdjustment = MIN_HIJRI_ADJUSTMENT;
                }
                saveSettings();
                updateOptionLabels();
                refreshDates();
            }
        });
        findViewById(R.id.clock_studio_fullscreen_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFullScreen(true);
            }
        });
        updateOptionLabels();
    }

    private void bindStyleSelector() {
        RecyclerView styleRecycler = findViewById(R.id.clock_studio_styles);
        styleRecycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        styleAdapter = new ClockStyleAdapter(styles, activeStyle.getId(),
                new ClockStyleAdapter.OnStyleClickListener() {
                    @Override
                    public void onStyleSelected(@NonNull ClockStyle style) {
                        selectStyle(style);
                    }
                });
        styleRecycler.setAdapter(styleAdapter);
        int selectedPosition = styles.indexOf(activeStyle);
        if (selectedPosition >= 0) {
            styleRecycler.scrollToPosition(selectedPosition);
        }
    }

    private void selectStyle(@NonNull ClockStyle style) {
        if (style.getId().equals(activeStyle.getId())) {
            return;
        }
        activeStyle = style;
        saveSettings();
        styleAdapter.setSelectedId(style.getId());
        applyDateStyle();
        rebuildClock(true);
    }

    private void rebuildClock(boolean animate) {
        stopActiveClock();
        if (activeClock != null) {
            clockArea.removeView(activeClock);
        }
        if (secondaryClock != null) {
            clockArea.removeView(secondaryClock);
            secondaryClock = null;
        }

        if (activeStyle.isAnalog()) {
            AnalogClockView analog = new AnalogClockView(this);
            analog.setStyle(activeStyle);
            analog.setShowSeconds(showSeconds);
            activeClock = analog;
        } else {
            DigitalClockView digital = new DigitalClockView(this);
            digital.setStyle(activeStyle);
            digital.setTwentyFourHour(twentyFourHour);
            digital.setShowSeconds(showSeconds);
            activeClock = digital;
        }

        FrameLayout.LayoutParams initial = new FrameLayout.LayoutParams(dp(120), dp(120));
        initial.gravity = Gravity.CENTER;
        clockArea.addView(activeClock, initial);

        if (activeStyle.hasSecondaryDigitalClock()) {
            DigitalClockView secondary = new DigitalClockView(this);
            secondary.setStyle(ClockStyleRegistry.byId("digital_elegant_thin"));
            secondary.setTwentyFourHour(twentyFourHour);
            secondary.setShowSeconds(showSeconds);
            secondaryClock = secondary;
            FrameLayout.LayoutParams secondaryInitial = new FrameLayout.LayoutParams(dp(160), dp(50));
            secondaryInitial.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            clockArea.addView(secondaryClock, secondaryInitial);
        }

        activeClock.setAlpha(animate ? 0f : 1f);
        activeClock.setScaleX(animate ? 0.94f : 1f);
        activeClock.setScaleY(animate ? 0.94f : 1f);
        if (animate) {
            activeClock.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220L).start();
        }
        applyDateStyle();
        preview.post(new Runnable() {
            @Override
            public void run() {
                updateClockLayout();
                if (resumed) {
                    startActiveClock();
                }
            }
        });
    }

    private void applyClockSettings() {
        if (activeClock instanceof AnalogClockView) {
            ((AnalogClockView) activeClock).setShowSeconds(showSeconds);
        } else if (activeClock instanceof DigitalClockView) {
            DigitalClockView digital = (DigitalClockView) activeClock;
            digital.setTwentyFourHour(twentyFourHour);
            digital.setShowSeconds(showSeconds);
        }
        if (secondaryClock instanceof DigitalClockView) {
            DigitalClockView digital = (DigitalClockView) secondaryClock;
            digital.setTwentyFourHour(twentyFourHour);
            digital.setShowSeconds(showSeconds);
        }
    }

    private void updateClockLayout() {
        if (activeClock == null || clockArea.getWidth() <= 0 || clockArea.getHeight() <= 0) {
            return;
        }
        int availableWidth = Math.max(1, clockArea.getWidth() - dp(22));
        int availableHeight = Math.max(1, clockArea.getHeight() - dp(18));
        int secondaryHeight = secondaryClock == null ? 0 : dp(48);
        int primaryHeight = Math.max(1, availableHeight - secondaryHeight - (secondaryClock == null ? 0 : dp(5)));
        float sizeFactor = clockSize == SIZE_SMALL ? 0.58f
                : clockSize == SIZE_LARGE ? 0.94f : 0.76f;
        FrameLayout.LayoutParams params;
        if (activeStyle.isAnalog()) {
            int side = Math.max(1, Math.round(Math.min(availableWidth, primaryHeight) * sizeFactor));
            params = new FrameLayout.LayoutParams(side, side);
        } else {
            int height = Math.max(dp(58), Math.round(primaryHeight * sizeFactor * 0.48f));
            height = Math.min(height, primaryHeight);
            int width = Math.min(availableWidth, Math.max(dp(180), Math.round(height * 2.55f)));
            params = new FrameLayout.LayoutParams(width, height);
        }
        params.gravity = Gravity.CENTER_HORIZONTAL;
        int margin = dp(9);
        if (clockPosition == POSITION_TOP) {
            params.gravity |= Gravity.TOP;
            params.topMargin = margin;
        } else if (clockPosition == POSITION_BOTTOM) {
            params.gravity |= Gravity.BOTTOM;
            params.bottomMargin = margin + secondaryHeight;
        } else {
            params.gravity |= Gravity.CENTER_VERTICAL;
            if (secondaryClock != null) {
                params.bottomMargin = secondaryHeight / 2;
            }
        }
        clockArea.updateViewLayout(activeClock, params);

        if (secondaryClock != null) {
            int secondaryWidth = Math.min(availableWidth, Math.max(dp(160), dp(48) * 3));
            FrameLayout.LayoutParams secondaryParams = new FrameLayout.LayoutParams(
                    secondaryWidth, dp(48));
            secondaryParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            secondaryParams.bottomMargin = dp(2);
            clockArea.updateViewLayout(secondaryClock, secondaryParams);
        }
    }

    private void updateOptionLabels() {
        String position;
        if (clockPosition == POSITION_TOP) {
            position = getString(R.string.clock_studio_top);
        } else if (clockPosition == POSITION_BOTTOM) {
            position = getString(R.string.clock_studio_bottom);
        } else {
            position = getString(R.string.clock_studio_center);
        }
        String size;
        if (clockSize == SIZE_SMALL) {
            size = getString(R.string.clock_studio_small);
        } else if (clockSize == SIZE_LARGE) {
            size = getString(R.string.clock_studio_large);
        } else {
            size = getString(R.string.clock_studio_medium);
        }
        positionButton.setText(getString(R.string.clock_studio_position_value, position));
        sizeButton.setText(getString(R.string.clock_studio_size_value, size));
        formatButton.setText(twentyFourHour
                ? R.string.clock_studio_format_24 : R.string.clock_studio_format_12);
        secondsButton.setText(showSeconds
                ? R.string.clock_studio_seconds_on : R.string.clock_studio_seconds_off);
        hijriButton.setText(showHijri
                ? R.string.clock_studio_hijri_on : R.string.clock_studio_hijri_off);
        gregorianButton.setText(showGregorian
                ? R.string.clock_studio_gregorian_on : R.string.clock_studio_gregorian_off);
        dayButton.setText(showDay ? R.string.clock_studio_day_on : R.string.clock_studio_day_off);
        adjustmentButton.setText(getString(R.string.clock_studio_adjustment_value, hijriAdjustment));
    }

    private void updateDateVisibility() {
        boolean anyDate = showHijri || showGregorian || showDay;
        dateArea.setVisibility(anyDate ? View.VISIBLE : View.GONE);
        hijriText.setVisibility(showHijri ? View.VISIBLE : View.GONE);
        gregorianText.setVisibility(showGregorian ? View.VISIBLE : View.GONE);
        dayText.setVisibility(showDay ? View.VISIBLE : View.GONE);
        dateDivider.setVisibility(showHijri && showGregorian ? View.VISIBLE : View.GONE);

        LinearLayout namePanel = findViewById(R.id.clock_studio_name_panel);
        LinearLayout.LayoutParams nameParams = (LinearLayout.LayoutParams) namePanel.getLayoutParams();
        LinearLayout.LayoutParams clockParams = (LinearLayout.LayoutParams) clockArea.getLayoutParams();
        LinearLayout.LayoutParams dateParams = (LinearLayout.LayoutParams) dateArea.getLayoutParams();
        if (anyDate) {
            nameParams.weight = 0.30f;
            clockParams.weight = 0.45f;
            dateParams.weight = 0.25f;
        } else {
            nameParams.weight = 0.36f;
            clockParams.weight = 0.64f;
            dateParams.weight = 0f;
        }
        namePanel.setLayoutParams(nameParams);
        clockArea.setLayoutParams(clockParams);
        dateArea.setLayoutParams(dateParams);
        applyDateStyle();
        preview.requestLayout();
    }

    /** Updates date text only on open, at midnight, or when a date setting changes. */
    private void refreshDates() {
        Date now = Calendar.getInstance().getTime();
        if (showHijri) {
            HijriDate hijriDate = HijriDateProvider.today(hijriAdjustment);
            hijriText.setText(HijriDateFormatter.formatHijri(this, hijriDate));
        }
        if (showGregorian) {
            gregorianText.setText(HijriDateFormatter.formatGregorian(this, now));
        }
        if (showDay) {
            dayText.setText(HijriDateFormatter.formatDay(this, now));
        }
    }

    private void scheduleNextDateRefresh() {
        dateHandler.removeCallbacks(dateRefreshRunnable);
        if (!resumed || !(showHijri || showGregorian || showDay)) {
            return;
        }
        Calendar nextMidnight = Calendar.getInstance();
        nextMidnight.add(Calendar.DAY_OF_MONTH, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);
        long delay = Math.max(1000L, nextMidnight.getTimeInMillis() - System.currentTimeMillis() + 250L);
        dateHandler.postDelayed(dateRefreshRunnable, delay);
    }

    private void applyDateStyle() {
        if (datePanel == null) {
            return;
        }
        boolean neon = activeStyle != null && activeStyle.usesNeonTreatment();
        boolean glass = activeStyle != null && activeStyle.usesGlassTreatment();
        boolean gold = activeStyle != null && ("hybrid_gold_analog".equals(activeStyle.getId())
                || "hybrid_luxury".equals(activeStyle.getId())
                || "analog_luxury_gold".equals(activeStyle.getId())
                || "digital_luxury_gold".equals(activeStyle.getId()));

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));
        if (glass) {
            boolean light = "glass_light".equals(activeStyle.getId());
            background.setColor(light ? Color.argb(150, 242, 247, 244)
                    : Color.argb(145, 7, 15, 21));
            background.setStroke(dp(1), light ? Color.argb(190, 255, 255, 255)
                    : Color.argb(190, 214, 235, 226));
        } else if (neon) {
            background.setColor(Color.argb(74, 2, 9, 19));
            background.setStroke(dp(1), activeStyle.getPreviewColor());
        } else if (gold) {
            background.setColor(Color.argb(115, 33, 24, 10));
            background.setStroke(dp(1), Color.rgb(216, 177, 90));
        } else {
            background.setColor(Color.argb(80, 8, 24, 32));
            background.setStroke(dp(1), Color.argb(130, 122, 163, 157));
        }
        datePanel.setBackground(background);
        datePanel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        int primary = neon ? activeStyle.getPreviewColor()
                : gold ? Color.rgb(242, 212, 141) : Color.rgb(255, 248, 231);
        int secondary = glass && "glass_light".equals(activeStyle.getId())
                ? Color.rgb(39, 75, 75) : Color.rgb(214, 201, 149);
        setDateTextStyle(dayText, gold || neon ? primary : Color.rgb(242, 212, 141), neon);
        setDateTextStyle(hijriText, primary, neon);
        setDateTextStyle(gregorianText, secondary, neon);
    }

    private void setDateTextStyle(TextView textView, int color, boolean glow) {
        textView.setTextColor(color);
        textView.getPaint().clearShadowLayer();
        if (glow) {
            textView.getPaint().setShadowLayer(dp(7), 0f, 0f, color);
        }
    }

    private void setFullScreen(boolean enabled) {
        fullScreen = enabled;
        controls.setVisibility(enabled ? View.GONE : View.VISIBLE);
        toolbar.setVisibility(enabled ? View.GONE : View.VISIBLE);
        if (enabled) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        preview.post(new Runnable() {
            @Override
            public void run() {
                updateClockLayout();
            }
        });
    }

    private AllahName readSelectedName() {
        String id = getIntent().getStringExtra(EXTRA_NAME_ID);
        AllahName name = AllahNamesCatalog.byId(id);
        if (name != null) {
            return name;
        }
        int number = getIntent().getIntExtra(EXTRA_NAME_NUMBER, 1);
        return AllahNamesCatalog.byNumber(number);
    }

    private void startActiveClock() {
        if (activeClock instanceof AnalogClockView) {
            ((AnalogClockView) activeClock).startClock();
        } else if (activeClock instanceof DigitalClockView) {
            ((DigitalClockView) activeClock).startClock();
        }
        if (secondaryClock instanceof DigitalClockView) {
            ((DigitalClockView) secondaryClock).startClock();
        }
    }

    private void stopActiveClock() {
        if (activeClock instanceof AnalogClockView) {
            ((AnalogClockView) activeClock).stopClock();
        } else if (activeClock instanceof DigitalClockView) {
            ((DigitalClockView) activeClock).stopClock();
        }
        if (secondaryClock instanceof DigitalClockView) {
            ((DigitalClockView) secondaryClock).stopClock();
        }
    }

    private void saveSettings() {
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putString(KEY_STYLE, activeStyle.getId())
                .putInt(KEY_POSITION, clockPosition)
                .putInt(KEY_SIZE, clockSize)
                .putBoolean(KEY_24_HOUR, twentyFourHour)
                .putBoolean(KEY_SECONDS, showSeconds)
                .putBoolean(KEY_SHOW_HIJRI, showHijri)
                .putBoolean(KEY_SHOW_GREGORIAN, showGregorian)
                .putBoolean(KEY_SHOW_DAY, showDay)
                .putInt(KEY_HIJRI_ADJUSTMENT, hijriAdjustment)
                .apply();
    }

    private int themeForName(int number) {
        switch ((number - 1) % 6) {
            case 1:
                return R.drawable.bg_allah_theme_emerald;
            case 2:
                return R.drawable.bg_allah_theme_black_gold;
            case 3:
                return R.drawable.bg_allah_theme_sapphire;
            case 4:
                return R.drawable.bg_allah_theme_forest;
            case 5:
                return R.drawable.bg_allah_theme_plum;
            default:
                return R.drawable.bg_allah_theme_navy;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        startActiveClock();
        refreshDates();
        scheduleNextDateRefresh();
    }

    @Override
    protected void onPause() {
        resumed = false;
        stopActiveClock();
        dateHandler.removeCallbacks(dateRefreshRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopActiveClock();
        dateHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (fullScreen) {
            setFullScreen(false);
        } else {
            super.onBackPressed();
        }
    }
}
