package com.clock.livewallpaper.quran;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline index of the 114 surahs of the Mushaf.
 *
 * <p>Everything the Surah Index screen and the reader header need is resolved here, in Java, with
 * no database round trip: names, ayah counts and the Makkah/Madinah classification. Ayah counts
 * and the page/Juz mapping are delegated to {@link QuranMetadata} so the numbers cannot drift apart
 * from the bundled {@code quran.ar.uthmani.db}.
 *
 * <p>Arabic names are the names used by the bundled Tanzil Uthmani text, so the list screen and the
 * reader header always agree with the database. See docs/licenses/Tanzil-Uthmani-CC-BY-3.0.txt.
 * Regenerate {@link #ARABIC_NAMES} with {@code python3 tools/build_quran_db.py} only if the source
 * text is ever re-pinned to a newer Tanzil release.
 */
public final class SurahIndex {

    /** Number of surahs in the Mushaf. */
    public static final int TOTAL_SURAHS = 114;

    /** Number of ayahs in the Mushaf under the Hafs numbering. */
    public static final int TOTAL_AYAHS = 6236;

    /** Arabic surah names, index 0 is surah 1 (Al-Fatihah). */
    public static final String[] ARABIC_NAMES = {
        "الفاتحة", "البقرة", "آل عمران",
        "النساء", "المائدة", "الأنعام",
        "الأعراف", "الأنفال", "التوبة",
        "يونس", "هود", "يوسف",
        "الرعد", "ابراهيم", "الحجر",
        "النحل", "الإسراء", "الكهف",
        "مريم", "طه", "الأنبياء",
        "الحج", "المؤمنون", "النور",
        "الفرقان", "الشعراء", "النمل",
        "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب",
        "سبإ", "فاطر", "يس",
        "الصافات", "ص", "الزمر",
        "غافر", "فصلت", "الشورى",
        "الزخرف", "الدخان", "الجاثية",
        "الأحقاف", "محمد", "الفتح",
        "الحجرات", "ق", "الذاريات",
        "الطور", "النجم", "القمر",
        "الرحمن", "الواقعة", "الحديد",
        "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون",
        "التغابن", "الطلاق", "التحريم",
        "الملك", "القلم", "الحاقة",
        "المعارج", "نوح", "الجن",
        "المزمل", "المدثر", "القيامة",
        "الانسان", "المرسلات", "النبإ",
        "النازعات", "عبس", "التكوير",
        "الإنفطار", "المطففين", "الإنشقاق",
        "البروج", "الطارق", "الأعلى",
        "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى",
        "الشرح", "التين", "العلق",
        "القدر", "البينة", "الزلزلة",
        "العاديات", "القارعة", "التكاثر",
        "العصر", "الهمزة", "الفيل",
        "قريش", "الماعون", "الكوثر",
        "الكافرون", "النصر", "المسد",
        "الإخلاص", "الفلق", "الناس",
    };

    /** Transliterated surah names, index 0 is surah 1 (Al-Fatihah). */
    public static final String[] ENGLISH_NAMES = {
        "Al-Fatihah", "Al-Baqarah", "Aal-Imran",
        "An-Nisa", "Al-Ma'idah", "Al-An'am",
        "Al-A'raf", "Al-Anfal", "At-Tawbah",
        "Yunus", "Hud", "Yusuf",
        "Ar-Ra'd", "Ibrahim", "Al-Hijr",
        "An-Nahl", "Al-Isra", "Al-Kahf",
        "Maryam", "Ta-Ha", "Al-Anbya",
        "Al-Hajj", "Al-Mu'minun", "An-Nur",
        "Al-Furqan", "Ash-Shu'ara", "An-Naml",
        "Al-Qasas", "Al-Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab",
        "Saba", "Fatir", "Ya-Sin",
        "As-Saffat", "Sad", "Az-Zumar",
        "Ghafir", "Fussilat", "Ash-Shura",
        "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah",
        "Al-Ahqaf", "Muhammad", "Al-Fath",
        "Al-Hujurat", "Qaf", "Adh-Dhariyat",
        "At-Tur", "An-Najm", "Al-Qamar",
        "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid",
        "Al-Mujadila", "Al-Hashr", "Al-Mumtahanah",
        "As-Saf", "Al-Jumu'ah", "Al-Munafiqun",
        "At-Taghabun", "At-Talaq", "At-Tahrim",
        "Al-Mulk", "Al-Qalam", "Al-Haqqah",
        "Al-Ma'arij", "Nuh", "Al-Jinn",
        "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah",
        "Al-Insan", "Al-Mursalat", "An-Naba",
        "An-Nazi'at", "Abasa", "At-Takwir",
        "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq",
        "Al-Buruj", "At-Tariq", "Al-A'la",
        "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Layl", "Ad-Duha",
        "Ash-Sharh", "At-Tin", "Al-Alaq",
        "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah",
        "Al-Adiyat", "Al-Qari'ah", "At-Takathur",
        "Al-Asr", "Al-Humazah", "Al-Fil",
        "Quraysh", "Al-Ma'un", "Al-Kawthar",
        "Al-Kafirun", "An-Nasr", "Al-Masad",
        "Al-Ikhlas", "Al-Falaq", "An-Nas",
    };

    /** English meanings of the surah names, index 0 is surah 1 (Al-Fatihah). */
    public static final String[] ENGLISH_MEANINGS = {
        "The Opening", "The Cow",
        "Family of Imran", "The Women",
        "The Table Spread", "The Cattle",
        "The Heights", "The Spoils of War",
        "The Repentance", "Jonah",
        "Hud", "Joseph",
        "The Thunder", "Abraham",
        "The Rocky Tract", "The Bee",
        "The Night Journey", "The Cave",
        "Mary", "Ta-Ha",
        "The Prophets", "The Pilgrimage",
        "The Believers", "The Light",
        "The Criterion", "The Poets",
        "The Ant", "The Stories",
        "The Spider", "The Byzantines",
        "Luqman", "The Prostration",
        "The Combined Forces", "Sheba",
        "Originator", "Ya Sin",
        "Those Who Set the Ranks", "The Letter Sad",
        "The Troops", "The Forgiver",
        "Explained in Detail", "The Consultation",
        "The Ornaments of Gold", "The Smoke",
        "The Crouching", "The Wind-Curved Sandhills",
        "Muhammad", "The Victory",
        "The Rooms", "The Letter Qaf",
        "The Winnowing Winds", "The Mount",
        "The Star", "The Moon",
        "The Beneficent", "The Inevitable",
        "The Iron", "The Pleading Woman",
        "The Exile", "She That Is to Be Examined",
        "The Ranks", "Friday, The Congregation",
        "The Hypocrites", "The Mutual Disillusion",
        "The Divorce", "The Prohibition",
        "The Sovereignty", "The Pen",
        "The Reality", "The Ascending Stairways",
        "Noah", "The Jinn",
        "The Enshrouded One", "The Cloaked One",
        "The Resurrection", "Man",
        "The Emissaries", "The Tidings",
        "Those Who Drag Forth", "He Frowned",
        "The Overthrowing", "The Cleaving",
        "The Defrauding", "The Sundering",
        "The Mansions of the Stars", "The Nightcomer",
        "The Most High", "The Overwhelming",
        "The Dawn", "The City",
        "The Sun", "The Night",
        "The Morning Hours", "The Relief",
        "The Fig", "The Clot",
        "The Power", "The Clear Proof",
        "The Earthquake", "The Courser",
        "The Calamity", "The Rivalry in Increase",
        "The Declining Day", "The Traducer",
        "The Elephant", "Quraysh",
        "The Small Kindnesses", "The Abundance",
        "The Disbelievers", "The Divine Support",
        "The Palm Fibre", "The Sincerity",
        "The Daybreak", "Mankind",
    };

    /**
     * Conventional Egyptian classification of where each surah was revealed; index 0 is surah 1.
     * {@code true} means Madinan, {@code false} means Makkan. Display only -- it never affects text.
     */
    private static final boolean[] MEDINAN_FLAGS = {
        false, true, true, true,
        true, false, false, true,
        true, false, false, false,
        true, false, false, false,
        false, false, false, false,
        false, true, false, true,
        false, false, false, false,
        false, false, false, false,
        true, false, false, false,
        false, false, false, false,
        false, false, false, false,
        false, false, true, true,
        true, false, false, false,
        false, false, true, false,
        true, true, true, true,
        true, true, true, true,
        true, true, false, false,
        false, false, false, false,
        false, false, false, true,
        false, false, false, false,
        false, false, false, false,
        false, false, false, false,
        false, false, false, false,
        false, false, false, false,
        false, true, true, false,
        false, false, false, false,
        false, false, false, false,
        false, true, false, false,
        false, false,
    };

    private static final Surah[] SURAHS = buildAll();

    private SurahIndex() { }

    private static Surah[] buildAll() {
        Surah[] built = new Surah[TOTAL_SURAHS];
        for (int id = 1; id <= TOTAL_SURAHS; id++) {
            built[id - 1] = new Surah(id);
        }
        return built;
    }

    /** @return {@code true} when {@code surahId} is a real surah, 1 to 114 inclusive. */
    public static boolean isValid(int surahId) {
        return surahId >= 1 && surahId <= TOTAL_SURAHS;
    }

    /**
     * @param surahId 1-based surah number
     * @return the surah descriptor
     * @throws IllegalArgumentException when the id is outside 1..114
     */
    public static Surah get(int surahId) {
        if (!isValid(surahId)) {
            throw new IllegalArgumentException("Surah must be between 1 and " + TOTAL_SURAHS
                    + ", got " + surahId);
        }
        return SURAHS[surahId - 1];
    }

    /** @return an unmodifiable list of all 114 surahs, in Mushaf order. */
    public static List<Surah> all() {
        List<Surah> list = new ArrayList<>(TOTAL_SURAHS);
        Collections.addAll(list, SURAHS);
        return Collections.unmodifiableList(list);
    }

    /** @return the Arabic name of a surah, or an empty string when the id is invalid. */
    public static String arabicName(int surahId) {
        return isValid(surahId) ? ARABIC_NAMES[surahId - 1] : "";
    }

    /** @return the transliterated name of a surah, or an empty string when the id is invalid. */
    public static String englishName(int surahId) {
        return isValid(surahId) ? ENGLISH_NAMES[surahId - 1] : "";
    }

    /**
     * Immutable descriptor for one surah. Page and Juz values come from {@link QuranMetadata}, so
     * they follow the standard 604-page Madani Mushaf with Hafs ayah numbering.
     */
    public static final class Surah {
        /** 1-based surah number. */
        public final int id;
        /** Arabic name, for example {@code البقرة}. */
        public final String arabicName;
        /** Transliterated name, for example {@code Al-Baqarah}. */
        public final String englishName;
        /** English meaning of the name, for example {@code The Cow}. */
        public final String englishMeaning;
        /** {@code true} when the surah is classified as Madinan. */
        public final boolean medinan;
        /** Number of ayahs in the surah. */
        public final int ayahCount;
        /** Juz containing ayah 1 of this surah. */
        public final int firstJuz;
        /** Juz containing the last ayah of this surah. */
        public final int lastJuz;
        /** Mushaf page holding ayah 1 of this surah. */
        public final int firstPage;
        /** Mushaf page holding the last ayah of this surah. */
        public final int lastPage;

        Surah(int id) {
            this.id = id;
            this.arabicName = ARABIC_NAMES[id - 1];
            this.englishName = ENGLISH_NAMES[id - 1];
            this.englishMeaning = ENGLISH_MEANINGS[id - 1];
            this.medinan = MEDINAN_FLAGS[id - 1];
            this.ayahCount = QuranMetadata.ayahCount(id);
            this.firstJuz = QuranMetadata.juzFor(id, 1);
            this.lastJuz = QuranMetadata.juzFor(id, ayahCount);
            this.firstPage = QuranMetadata.pageFor(id, 1);
            this.lastPage = QuranMetadata.pageFor(id, ayahCount);
        }

        /**
         * @return {@code true} when a Mushaf prints the basmallah above this surah's first ayah.
         *     Surah 1 carries it as ayah 1 itself and surah 9 has none, so neither gets a header.
         */
        public boolean hasBasmallahHeader() {
            return id != 1 && id != 9;
        }

        /** @return {@code true} when the surah spans more than one Mushaf page. */
        public boolean spansMultiplePages() {
            return firstPage != lastPage;
        }

        /** @return {@code true} when the surah spans more than one Juz. */
        public boolean spansMultipleJuz() {
            return firstJuz != lastJuz;
        }

        /** @return the surah number written with Arabic-Indic digits, for example {@code ٢}. */
        public String arabicNumber() {
            return QuranMetadata.arabicNumber(id);
        }

        @Override
        public String toString() {
            return id + ". " + englishName + " (" + arabicName + ")";
        }
    }
}
