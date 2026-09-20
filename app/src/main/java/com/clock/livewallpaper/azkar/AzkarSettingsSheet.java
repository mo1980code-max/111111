package com.clock.livewallpaper.azkar;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.clock.livewallpaper.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

/**
 * The Azkar display-settings bottom sheet: dhikr text size (A− / A+), Arabic font family
 * (Default / Amiri / Cairo / Tajawal) and the Azkar-section night mode.
 *
 * <p>Every control applies instantly and persists immediately in {@link AzkarFontStore}, so
 * the choice survives closing the app. The host screen repaints through
 * {@link Listener#onDisplayChanged()} — counters are never touched, so changing any
 * setting cannot reset a count. The sheet itself follows the active palette (day/night)
 * and is repainted at once when the night switch flips.
 */
public final class AzkarSettingsSheet {

    /** The host repaints its chrome and cards from the store; called on every change. */
    public interface Listener {
        void onDisplayChanged();
    }

    private AzkarSettingsSheet() {
    }

    /** Shows the sheet; safe to call from either Azkar screen. */
    public static void show(@NonNull AppCompatActivity activity, @NonNull Listener listener) {
        AzkarFontStore store = AzkarFontStore.get(activity);
        View sheet = LayoutInflater.from(activity)
                .inflate(R.layout.azkar_settings_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(sheet);

        TextView sizeValue = sheet.findViewById(R.id.azkar_font_size_value);
        RadioGroup fontGroup = sheet.findViewById(R.id.azkar_font_group);
        RadioButton fontDefault = sheet.findViewById(R.id.azkar_font_default);
        RadioButton fontAmiri = sheet.findViewById(R.id.azkar_font_amiri);
        RadioButton fontCairo = sheet.findViewById(R.id.azkar_font_cairo);
        RadioButton fontTajawal = sheet.findViewById(R.id.azkar_font_tajawal);
        SwitchCompat nightSwitch = sheet.findViewById(R.id.azkar_night_switch);

        paintSheet(activity, sheet, store.isNightMode());
        paintSize(sizeValue, store.getSp());
        checkFamily(fontGroup, fontDefault, fontAmiri, fontCairo, fontTajawal,
                store.getFontFamily());
        nightSwitch.setChecked(store.isNightMode());

        sheet.findViewById(R.id.azkar_font_minus).setOnClickListener(v -> {
            float next = AzkarFontStore.clamp(store.getSp() - AzkarFontStore.STEP_SP);
            store.setSp(next);
            paintSize(sizeValue, next);
            listener.onDisplayChanged();
        });
        sheet.findViewById(R.id.azkar_font_plus).setOnClickListener(v -> {
            float next = AzkarFontStore.clamp(store.getSp() + AzkarFontStore.STEP_SP);
            store.setSp(next);
            paintSize(sizeValue, next);
            listener.onDisplayChanged();
        });

        fontGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.azkar_font_default) {
                store.setFontFamily(AzkarFontStore.FAMILY_DEFAULT);
            } else if (checkedId == R.id.azkar_font_cairo) {
                store.setFontFamily(AzkarFontStore.FAMILY_CAIRO);
            } else if (checkedId == R.id.azkar_font_tajawal) {
                store.setFontFamily(AzkarFontStore.FAMILY_TAJAWAL);
            } else {
                store.setFontFamily(AzkarFontStore.FAMILY_AMIRI);
            }
            listener.onDisplayChanged();
        });

        nightSwitch.setOnCheckedChangeListener((button, checked) -> {
            store.setNightMode(checked);
            paintSheet(activity, sheet, checked);
            listener.onDisplayChanged();
        });

        dialog.show();
    }

    /** Shows the current size (e.g. "20") between the A− / A+ buttons. */
    private static void paintSize(@NonNull TextView sizeValue, float sp) {
        sizeValue.setText(String.format(Locale.US, "%d", Math.round(sp)));
    }

    private static void checkFamily(@NonNull RadioGroup group,
                                    @NonNull RadioButton fontDefault,
                                    @NonNull RadioButton fontAmiri,
                                    @NonNull RadioButton fontCairo,
                                    @NonNull RadioButton fontTajawal,
                                    @NonNull String family) {
        if (AzkarFontStore.FAMILY_DEFAULT.equals(family)) {
            group.check(fontDefault.getId());
        } else if (AzkarFontStore.FAMILY_CAIRO.equals(family)) {
            group.check(fontCairo.getId());
        } else if (AzkarFontStore.FAMILY_TAJAWAL.equals(family)) {
            group.check(fontTajawal.getId());
        } else {
            group.check(fontAmiri.getId());
        }
    }

    /** Paints the sheet chrome in the active palette (day or night). */
    private static void paintSheet(@NonNull AppCompatActivity activity, @NonNull View sheet,
                                   boolean night) {
        sheet.findViewById(R.id.azkar_sheet_root).setBackgroundResource(night
                ? R.drawable.bg_azkar_sheet_night
                : R.drawable.bg_azkar_sheet);
        int title = ContextCompat.getColor(activity,
                night ? R.color.azkar_night_text : R.color.azkar_text);
        int muted = ContextCompat.getColor(activity,
                night ? R.color.azkar_night_muted : R.color.azkar_muted);
        int accent = ContextCompat.getColor(activity,
                night ? R.color.azkar_night_primary : R.color.azkar_primary);
        setTextColor(sheet, R.id.azkar_sheet_title, title);
        setTextColor(sheet, R.id.azkar_sheet_size_label, muted);
        setTextColor(sheet, R.id.azkar_font_size_value, title);
        setTextColor(sheet, R.id.azkar_font_minus, accent);
        setTextColor(sheet, R.id.azkar_font_plus, accent);
        setTextColor(sheet, R.id.azkar_sheet_font_label, muted);
        setTextColor(sheet, R.id.azkar_font_default, title);
        setTextColor(sheet, R.id.azkar_font_amiri, title);
        setTextColor(sheet, R.id.azkar_font_cairo, title);
        setTextColor(sheet, R.id.azkar_font_tajawal, title);
        setTextColor(sheet, R.id.azkar_night_label, title);
        // Tints the switch track/thumb accent so it stays visible on both palettes.
        SwitchCompat nightSwitch = sheet.findViewById(R.id.azkar_night_switch);
        nightSwitch.setTextColor(title);
        nightSwitch.setThumbTintList(ColorStateList.valueOf(accent));
    }

    private static void setTextColor(@NonNull View sheet, int viewId, int color) {
        ((TextView) sheet.findViewById(viewId)).setTextColor(color);
    }
}
