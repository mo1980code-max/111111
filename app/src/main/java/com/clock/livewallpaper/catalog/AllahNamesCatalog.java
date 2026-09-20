package com.clock.livewallpaper.catalog;

import com.clock.livewallpaper.model.AllahName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The single offline source of truth for the 99 Names of Allah shown by the app.
 *
 * <p>Keep the order and stable ids unchanged once released. Arabic is kept as plain Java text and
 * is rendered by Android, so no name is part of an image asset.</p>
 */
public final class AllahNamesCatalog {

    public static final int EXPECTED_COUNT = 99;

    /** Immutable, ordered catalog: item 0 is number 1 and item 98 is number 99. */
    public static final List<AllahName> ALL_NAMES = createCatalog();

    private AllahNamesCatalog() {
    }

    public static List<AllahName> getAll() {
        return ALL_NAMES;
    }

    public static AllahName byNumber(int number) {
        if (number < 1 || number > ALL_NAMES.size()) {
            return null;
        }
        return ALL_NAMES.get(number - 1);
    }

    public static AllahName byId(String id) {
        if (id == null) {
            return null;
        }
        for (AllahName name : ALL_NAMES) {
            if (id.equals(name.getId())) {
                return name;
            }
        }
        return null;
    }

    private static List<AllahName> createCatalog() {
        List<AllahName> names = new ArrayList<>(EXPECTED_COUNT);

        names.add(new AllahName(1, "allah_01_ar_rahman", "الرحمن", "Ar-Rahman"));
        names.add(new AllahName(2, "allah_02_ar_raheem", "الرحيم", "Ar-Raheem"));
        names.add(new AllahName(3, "allah_03_al_malik", "الملك", "Al-Malik"));
        names.add(new AllahName(4, "allah_04_al_quddus", "القدوس", "Al-Quddus"));
        names.add(new AllahName(5, "allah_05_as_salam", "السلام", "As-Salam"));
        names.add(new AllahName(6, "allah_06_al_mumin", "المؤمن", "Al-Mu'min"));
        names.add(new AllahName(7, "allah_07_al_muhaymin", "المهيمن", "Al-Muhaymin"));
        names.add(new AllahName(8, "allah_08_al_aziz", "العزيز", "Al-Aziz"));
        names.add(new AllahName(9, "allah_09_al_jabbar", "الجبار", "Al-Jabbar"));
        names.add(new AllahName(10, "allah_10_al_mutakabbir", "المتكبر", "Al-Mutakabbir"));
        names.add(new AllahName(11, "allah_11_al_khaliq", "الخالق", "Al-Khaliq"));
        names.add(new AllahName(12, "allah_12_al_bari", "البارئ", "Al-Bari'"));
        names.add(new AllahName(13, "allah_13_al_musawwir", "المصور", "Al-Musawwir"));
        names.add(new AllahName(14, "allah_14_al_ghaffar", "الغفار", "Al-Ghaffar"));
        names.add(new AllahName(15, "allah_15_al_qahhar", "القهار", "Al-Qahhar"));
        names.add(new AllahName(16, "allah_16_al_wahhab", "الوهاب", "Al-Wahhab"));
        names.add(new AllahName(17, "allah_17_ar_razzaq", "الرزاق", "Ar-Razzaq"));
        names.add(new AllahName(18, "allah_18_al_fattah", "الفتاح", "Al-Fattah"));
        names.add(new AllahName(19, "allah_19_al_alim", "العليم", "Al-Alim"));
        names.add(new AllahName(20, "allah_20_al_qabid", "القابض", "Al-Qabid"));
        names.add(new AllahName(21, "allah_21_al_basit", "الباسط", "Al-Basit"));
        names.add(new AllahName(22, "allah_22_al_khafid", "الخافض", "Al-Khafid"));
        names.add(new AllahName(23, "allah_23_ar_rafi", "الرافع", "Ar-Rafi'"));
        names.add(new AllahName(24, "allah_24_al_muizz", "المعز", "Al-Mu'izz"));
        names.add(new AllahName(25, "allah_25_al_mudhill", "المذل", "Al-Mudhill"));
        names.add(new AllahName(26, "allah_26_as_sami", "السميع", "As-Sami'"));
        names.add(new AllahName(27, "allah_27_al_basir", "البصير", "Al-Basir"));
        names.add(new AllahName(28, "allah_28_al_hakam", "الحكم", "Al-Hakam"));
        names.add(new AllahName(29, "allah_29_al_adl", "العدل", "Al-Adl"));
        names.add(new AllahName(30, "allah_30_al_latif", "اللطيف", "Al-Latif"));
        names.add(new AllahName(31, "allah_31_al_khabir", "الخبير", "Al-Khabir"));
        names.add(new AllahName(32, "allah_32_al_halim", "الحليم", "Al-Halim"));
        names.add(new AllahName(33, "allah_33_al_azim", "العظيم", "Al-Azim"));
        names.add(new AllahName(34, "allah_34_al_ghafur", "الغفور", "Al-Ghafur"));
        names.add(new AllahName(35, "allah_35_ash_shakur", "الشكور", "Ash-Shakur"));
        names.add(new AllahName(36, "allah_36_al_ali", "العلي", "Al-Ali"));
        names.add(new AllahName(37, "allah_37_al_kabir", "الكبير", "Al-Kabir"));
        names.add(new AllahName(38, "allah_38_al_hafiz", "الحفيظ", "Al-Hafiz"));
        names.add(new AllahName(39, "allah_39_al_muqit", "المقيت", "Al-Muqit"));
        names.add(new AllahName(40, "allah_40_al_hasib", "الحسيب", "Al-Hasib"));
        names.add(new AllahName(41, "allah_41_al_jalil", "الجليل", "Al-Jalil"));
        names.add(new AllahName(42, "allah_42_al_karim", "الكريم", "Al-Karim"));
        names.add(new AllahName(43, "allah_43_ar_raqib", "الرقيب", "Ar-Raqib"));
        names.add(new AllahName(44, "allah_44_al_mujib", "المجيب", "Al-Mujib"));
        names.add(new AllahName(45, "allah_45_al_wasi", "الواسع", "Al-Wasi'"));
        names.add(new AllahName(46, "allah_46_al_hakim", "الحكيم", "Al-Hakim"));
        names.add(new AllahName(47, "allah_47_al_wadud", "الودود", "Al-Wadud"));
        names.add(new AllahName(48, "allah_48_al_majid", "المجيد", "Al-Majid"));
        names.add(new AllahName(49, "allah_49_al_baith", "الباعث", "Al-Ba'ith"));
        names.add(new AllahName(50, "allah_50_ash_shahid", "الشهيد", "Ash-Shahid"));
        names.add(new AllahName(51, "allah_51_al_haqq", "الحق", "Al-Haqq"));
        names.add(new AllahName(52, "allah_52_al_wakil", "الوكيل", "Al-Wakil"));
        names.add(new AllahName(53, "allah_53_al_qawiyy", "القوي", "Al-Qawiyy"));
        names.add(new AllahName(54, "allah_54_al_matin", "المتين", "Al-Matin"));
        names.add(new AllahName(55, "allah_55_al_wali", "الولي", "Al-Waliyy"));
        names.add(new AllahName(56, "allah_56_al_hamid", "الحميد", "Al-Hamid"));
        names.add(new AllahName(57, "allah_57_al_muhsi", "المحصي", "Al-Muhsi"));
        names.add(new AllahName(58, "allah_58_al_mubdi", "المبدئ", "Al-Mubdi'"));
        names.add(new AllahName(59, "allah_59_al_muid", "المعيد", "Al-Mu'id"));
        names.add(new AllahName(60, "allah_60_al_muhyi", "المحيي", "Al-Muhyi"));
        names.add(new AllahName(61, "allah_61_al_mumit", "المميت", "Al-Mumit"));
        names.add(new AllahName(62, "allah_62_al_hayy", "الحي", "Al-Hayy"));
        names.add(new AllahName(63, "allah_63_al_qayyum", "القيوم", "Al-Qayyum"));
        names.add(new AllahName(64, "allah_64_al_wajid", "الواجد", "Al-Wajid"));
        names.add(new AllahName(65, "allah_65_al_majid_alt", "الماجد", "Al-Majid"));
        names.add(new AllahName(66, "allah_66_al_wahid", "الواحد", "Al-Wahid"));
        names.add(new AllahName(67, "allah_67_al_ahad", "الأحد", "Al-Ahad"));
        names.add(new AllahName(68, "allah_68_as_samad", "الصمد", "As-Samad"));
        names.add(new AllahName(69, "allah_69_al_qadir", "القادر", "Al-Qadir"));
        names.add(new AllahName(70, "allah_70_al_muqtadir", "المقتدر", "Al-Muqtadir"));
        names.add(new AllahName(71, "allah_71_al_muqaddim", "المقدم", "Al-Muqaddim"));
        names.add(new AllahName(72, "allah_72_al_muakhkhir", "المؤخر", "Al-Mu'akhkhir"));
        names.add(new AllahName(73, "allah_73_al_awwal", "الأول", "Al-Awwal"));
        names.add(new AllahName(74, "allah_74_al_akhir", "الآخر", "Al-Akhir"));
        names.add(new AllahName(75, "allah_75_az_zahir", "الظاهر", "Az-Zahir"));
        names.add(new AllahName(76, "allah_76_al_batin", "الباطن", "Al-Batin"));
        names.add(new AllahName(77, "allah_77_al_wali_alt", "الوالي", "Al-Wali"));
        names.add(new AllahName(78, "allah_78_al_mutaali", "المتعالي", "Al-Muta'ali"));
        names.add(new AllahName(79, "allah_79_al_barr", "البر", "Al-Barr"));
        names.add(new AllahName(80, "allah_80_at_tawwab", "التواب", "At-Tawwab"));
        names.add(new AllahName(81, "allah_81_al_muntaqim", "المنتقم", "Al-Muntaqim"));
        names.add(new AllahName(82, "allah_82_al_afuww", "العفو", "Al-Afuww"));
        names.add(new AllahName(83, "allah_83_ar_rauf", "الرؤوف", "Ar-Ra'uf"));
        names.add(new AllahName(84, "allah_84_malik_al_mulk", "مالك الملك", "Malik-ul-Mulk"));
        names.add(new AllahName(85, "allah_85_dhul_jalali_wal_ikram", "ذو الجلال والإكرام", "Dhul-Jalali wal-Ikram"));
        names.add(new AllahName(86, "allah_86_al_muqsit", "المقسط", "Al-Muqsit"));
        names.add(new AllahName(87, "allah_87_al_jami", "الجامع", "Al-Jami'"));
        names.add(new AllahName(88, "allah_88_al_ghani", "الغني", "Al-Ghani"));
        names.add(new AllahName(89, "allah_89_al_mughni", "المغني", "Al-Mughni"));
        names.add(new AllahName(90, "allah_90_al_mani", "المانع", "Al-Mani'"));
        names.add(new AllahName(91, "allah_91_ad_darr", "الضار", "Ad-Darr"));
        names.add(new AllahName(92, "allah_92_an_nafi", "النافع", "An-Nafi'"));
        names.add(new AllahName(93, "allah_93_an_nur", "النور", "An-Nur"));
        names.add(new AllahName(94, "allah_94_al_hadi", "الهادي", "Al-Hadi"));
        names.add(new AllahName(95, "allah_95_al_badi", "البديع", "Al-Badi'"));
        names.add(new AllahName(96, "allah_96_al_baqi", "الباقي", "Al-Baqi"));
        names.add(new AllahName(97, "allah_97_al_warith", "الوارث", "Al-Warith"));
        names.add(new AllahName(98, "allah_98_ar_rashid", "الرشيد", "Ar-Rashid"));
        names.add(new AllahName(99, "allah_99_as_sabur", "الصبور", "As-Sabur"));

        if (names.size() != EXPECTED_COUNT) {
            throw new IllegalStateException("The Names of Allah catalog must contain exactly 99 items");
        }
        for (int index = 0; index < names.size(); index++) {
            if (names.get(index).getNumber() != index + 1) {
                throw new IllegalStateException("The Names of Allah catalog numbers must be consecutive");
            }
        }
        return Collections.unmodifiableList(names);
    }
}
