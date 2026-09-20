package com.clock.livewallpaper.clock;

import com.clock.livewallpaper.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Central registry for every Stage 2 Clock Studio style. */
public final class ClockStyleRegistry {

    private static final List<ClockStyle> STYLES = createStyles();

    private ClockStyleRegistry() {
    }

    public static List<ClockStyle> getAll() {
        return STYLES;
    }

    public static ClockStyle defaultStyle() {
        return STYLES.get(0);
    }

    public static ClockStyle byId(String id) {
        if (id != null) {
            for (ClockStyle style : STYLES) {
                if (id.equals(style.getId())) {
                    return style;
                }
            }
        }
        return defaultStyle();
    }

    private static List<ClockStyle> createStyles() {
        List<ClockStyle> styles = new ArrayList<>();

        // Six reusable analog faces.
        styles.add(new ClockStyle("analog_minimal", R.string.clock_style_minimal_analog,
                R.string.clock_family_analog, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.ANALOG, 0xFFF4F0E6));
        styles.add(new ClockStyle("analog_classic", R.string.clock_style_classic_analog,
                R.string.clock_family_analog, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.ANALOG, 0xFFE6CFA0));
        styles.add(new ClockStyle("analog_luxury_gold", R.string.clock_style_luxury_gold_analog,
                R.string.clock_family_analog, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.ANALOG, 0xFFD8AF52));
        styles.add(new ClockStyle("analog_black_gold", R.string.clock_style_black_gold_analog,
                R.string.clock_family_analog, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.ANALOG, 0xFFE2B957));
        styles.add(new ClockStyle("analog_silver", R.string.clock_style_silver_analog,
                R.string.clock_family_analog, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.ANALOG, 0xFFC7D1DB));
        styles.add(new ClockStyle("analog_floating", R.string.clock_style_floating_hands_analog,
                R.string.clock_family_analog, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.ANALOG, 0xFFF0D38A));

        // Six professional digital faces.
        styles.add(new ClockStyle("digital_minimal_white", R.string.clock_style_minimal_white,
                R.string.clock_family_digital, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.DIGITAL, 0xFFF8F5EA));
        styles.add(new ClockStyle("digital_modern_bold", R.string.clock_style_modern_bold,
                R.string.clock_family_digital, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.DIGITAL, 0xFFBFE9E0));
        styles.add(new ClockStyle("digital_elegant_thin", R.string.clock_style_elegant_thin,
                R.string.clock_family_digital, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.DIGITAL, 0xFFE6D8BD));
        styles.add(new ClockStyle("digital_luxury_gold", R.string.clock_style_luxury_gold,
                R.string.clock_family_digital, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.DIGITAL, 0xFFE3B85B));
        styles.add(new ClockStyle("digital_led", R.string.clock_style_led,
                R.string.clock_family_digital, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.DIGITAL, 0xFFFF765C));
        styles.add(new ClockStyle("digital_seven_segment", R.string.clock_style_seven_segment,
                R.string.clock_family_digital, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.DIGITAL, 0xFF67D18D));

        // Four restrained neon styles: two analog and two digital variants.
        styles.add(new ClockStyle("neon_cyan", R.string.clock_style_neon_cyan,
                R.string.clock_family_neon, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.NEON, 0xFF57E8F2));
        styles.add(new ClockStyle("neon_blue", R.string.clock_style_neon_blue,
                R.string.clock_family_neon, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.NEON, 0xFF6EA8FF));
        styles.add(new ClockStyle("neon_green", R.string.clock_style_neon_green,
                R.string.clock_family_neon, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.NEON, 0xFF6CDE8B));
        styles.add(new ClockStyle("neon_gold", R.string.clock_style_neon_gold,
                R.string.clock_family_neon, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.NEON, 0xFFF1C75B));

        // Two translucent, performance-friendly glass panels. No real-time blur is used.
        styles.add(new ClockStyle("glass_dark", R.string.clock_style_dark_glass,
                R.string.clock_family_glass, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.GLASS, 0xFFBFD9D3));
        styles.add(new ClockStyle("glass_light", R.string.clock_style_light_glass,
                R.string.clock_family_glass, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.GLASS, 0xFF315B5A));

        // Six hybrid compositions: the editor supplies the live clock and date panel together.
        styles.add(new ClockStyle("hybrid_gold_analog", R.string.clock_style_hybrid_gold_analog,
                R.string.clock_family_hybrid, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.HYBRID, 0xFFE4B95F));
        styles.add(new ClockStyle("hybrid_minimal", R.string.clock_style_hybrid_minimal,
                R.string.clock_family_hybrid, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.HYBRID, 0xFFF5F1E7));
        styles.add(new ClockStyle("hybrid_analog_digital", R.string.clock_style_hybrid_analog_digital,
                R.string.clock_family_hybrid, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.HYBRID, 0xFFE0B45B, true));
        styles.add(new ClockStyle("hybrid_glass", R.string.clock_style_hybrid_glass,
                R.string.clock_family_hybrid, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.HYBRID, 0xFFB9D8D0));
        styles.add(new ClockStyle("hybrid_neon", R.string.clock_style_hybrid_neon,
                R.string.clock_family_hybrid, ClockStyle.Kind.DIGITAL,
                ClockStyle.Family.HYBRID, 0xFF5CE6ED));
        styles.add(new ClockStyle("hybrid_luxury", R.string.clock_style_hybrid_luxury,
                R.string.clock_family_hybrid, ClockStyle.Kind.ANALOG,
                ClockStyle.Family.HYBRID, 0xFFF0C86A));

        return Collections.unmodifiableList(styles);
    }
}
