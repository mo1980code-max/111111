package com.clock.livewallpaper.clock;

import android.content.Context;

import androidx.annotation.NonNull;

import com.clock.livewallpaper.unlock.UnlockStore;

/**
 * Stable identifiers for the Names of Allah used by Clock Studio and the wallpaper renderer.
 *
 * <p>The first nine entries follow the existing free-content rule.  Other entries are only accepted
 * by the wallpaper when their permanent {@link UnlockStore} record exists; a crafted intent or a bad
 * preference can therefore never turn a locked name into wallpaper content.
 */
public final class AllahNameCatalog {
    private static final String[] ARABIC_NAMES = {
            "الرَّحْمَن", "الرَّحِيم", "الْمَلِك", "الْقُدُّوس", "السَّلَام", "الْمُؤْمِن",
            "الْمُهَيْمِن", "الْعَزِيز", "الْجَبَّار", "الْمُتَكَبِّر", "الْخَالِق", "الْبَارِئ",
            "الْمُصَوِّر", "الْغَفَّار", "الْقَهَّار", "الْوَهَّاب", "الرَّزَّاق", "الْفَتَّاح",
            "الْعَلِيم", "الْقَابِض", "الْبَاسِط", "الْخَافِض", "الرَّافِع", "الْمُعِزّ",
            "الْمُذِلّ", "السَّمِيع", "الْبَصِير", "الْحَكَم", "الْعَدْل", "اللَّطِيف",
            "الْخَبِير", "الْحَلِيم", "الْعَظِيم", "الْغَفُور", "الشَّكُور", "الْعَلِيّ",
            "الْكَبِير", "الْحَفِيظ", "الْمُقِيت", "الْحَسِيب", "الْجَلِيل", "الْكَرِيم",
            "الرَّقِيب", "الْمُجِيب", "الْوَاسِع", "الْحَكِيم", "الْوَدُود", "الْمَجِيد",
            "الْبَاعِث", "الشَّهِيد", "الْحَقّ", "الْوَكِيل", "الْقَوِيّ", "الْمَتِين",
            "الْوَلِيّ", "الْحَمِيد", "الْمُحْصِي", "الْمُبْدِئ", "الْمُعِيد", "الْمُحْيِي",
            "الْمُمِيت", "الْحَيّ", "الْقَيُّوم", "الْوَاجِد", "الْمَاجِد", "الْوَاحِد",
            "الْأَحَد", "الصَّمَد", "الْقَادِر", "الْمُقْتَدِر", "الْمُقَدِّم", "الْمُؤَخِّر",
            "الْأَوَّل", "الْآخِر", "الظَّاهِر", "الْبَاطِن", "الْوَالِي", "الْمُتَعَالِي",
            "الْبَرّ", "التَّوَّاب", "الْمُنْتَقِم", "الْعَفُوّ", "الرَّؤُوف", "مَالِكُ الْمُلْك",
            "ذُو الْجَلَالِ وَالْإِكْرَام", "الْمُقْسِط", "الْجَامِع", "الْغَنِيّ", "الْمُغْنِي",
            "الْمَانِع", "الضَّارّ", "النَّافِع", "النُّور", "الْهَادِي", "الْبَدِيع",
            "الْبَاقِي", "الْوَارِث", "الرَّشِيد", "الصَّبُور"
    };

    private AllahNameCatalog() {
    }

    public static int count() {
        return ARABIC_NAMES.length;
    }

    public static boolean isValid(int id) {
        return id >= 1 && id <= ARABIC_NAMES.length;
    }

    public static boolean isFree(int id) {
        return isValid(id) && id <= 9;
    }

    public static int safeId(int id) {
        return isValid(id) ? id : ClockStudioConfig.DEFAULT_NAME_ID;
    }

    @NonNull
    public static String getArabicName(int id) {
        return ARABIC_NAMES[safeId(id) - 1];
    }

    @NonNull
    public static String unlockKey(int id) {
        return "name:" + safeId(id);
    }

    /** Returns a safe, unlocked name for use by a non-UI caller such as WallpaperService. */
    public static int usableId(Context context, int requestedId) {
        int id = safeId(requestedId);
        if (isFree(id) || UnlockStore.get(context).isUnlocked(unlockKey(id))) {
            return id;
        }
        return ClockStudioConfig.DEFAULT_NAME_ID;
    }

    public static boolean isUsable(Context context, int id) {
        int safe = safeId(id);
        return isFree(safe) || UnlockStore.get(context).isUnlocked(unlockKey(safe));
    }
}
