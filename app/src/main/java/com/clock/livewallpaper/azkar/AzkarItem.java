package com.clock.livewallpaper.azkar;

import androidx.annotation.NonNull;

/**
 * One Azkar: the imported content plus the live counter state.
 *
 * <p>Content fields ({@code id}, {@code category}, {@code order}, {@code arabicText},
 * {@code repeatCount}, {@code virtue}) come verbatim from {@code assets/azkar.json} and never
 * change at runtime.
 *
 * <h2>Counter contract (session-only countdown)</h2>
 * <ul>
 *   <li>كل ذِكر يبدأ عدّاده من عدده الأصلي: {@code remaining = repeatCount} (مثلاً 3).</li>
 *   <li>كل ضغطة على زر العدّاد تنقص واحداً: {@code remaining--} عبر {@link #countDown()}.</li>
 *   <li>الاكتمال عند الوصول للصفر: {@link #isCompleted()} مشتقة دائماً ({@code remaining == 0})
 *       ولا تُخزَّن أبداً.</li>
 *   <li>العدّاد لا يُحفظ في أي تخزين: عند إغلاق التطبيق وإعادة فتحه تُبنى عناصر جديدة من
 *       {@link AzkarRepository#items} فيعود كل عدّاد لعدده الأصلي تلقائياً.</li>
 *   <li>لا يعيد العدّاد نفسه إلا بإعادة فتح الشاشة — لا زر إعادة، ولا تصفير تلقائي.</li>
 * </ul>
 *
 * <p>Every item carries its own {@code repeatCount} from the source (1, 3, 4, 7, 10, 100, ...);
 * the counter always starts from that number, never from a shared constant.
 */
public final class AzkarItem {

    private final int id;
    private final AzkarCategory category;
    private final int order;
    private final String arabicText;
    private final int repeatCount;
    private final String virtue;

    /**
     * المتبقي من هذا الذِكر. يبدأ دائماً من {@link #repeatCount} عند إنشاء العنصر
     * (أي عند كل فتح للشاشة)، ويعيش في الذاكرة فقط — لا يُقرأ ولا يُكتب في التخزين.
     */
    private int remaining;

    public AzkarItem(int id,
                     @NonNull AzkarCategory category,
                     int order,
                     @NonNull String arabicText,
                     int repeatCount,
                     @NonNull String virtue) {
        this.id = id;
        this.category = category;
        this.order = order;
        this.arabicText = arabicText;
        this.repeatCount = Math.max(1, repeatCount);
        this.virtue = virtue;
        // أهم سطر في العقد: البداية دائماً من العدد الأصلي، وليست من أي قيمة محفوظة.
        this.remaining = this.repeatCount;
    }

    /** @return the 1-based Azkar number within its category, as numbered by the source */
    public int id() {
        return id;
    }

    /** @return the section this item belongs to */
    @NonNull
    public AzkarCategory category() {
        return category;
    }

    /** @return the display order, matching the source order */
    public int order() {
        return order;
    }

    /** @return the full Arabic dhikr text, verbatim from the source */
    @NonNull
    public String arabicText() {
        return arabicText;
    }

    /** @return the exact required repetitions for THIS item, from the source */
    public int repeatCount() {
        return repeatCount;
    }

    /** @return the virtue/reward line from the source, or {@code ""} when the source gives none */
    @NonNull
    public String virtue() {
        return virtue;
    }

    /** @return taps left, always within {@code [0, repeatCount]} */
    public int remaining() {
        return remaining;
    }

    /** @return {@code true} exactly when the countdown reached zero */
    public boolean isCompleted() {
        return remaining == 0;
    }

    /**
     * Counts one tap down. Never goes below zero; taps on a completed item change nothing.
     *
     * @return {@code true} if the tap changed the count, {@code false} if already complete
     */
    public boolean countDown() {
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        return true;
    }
}
