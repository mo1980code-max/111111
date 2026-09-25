package com.clockadventure.domain.model

/** Icon drawn by the UI for a routine activity (Canvas art, no raster assets). */
enum class RoutineIcon { WAKE_UP, BREAKFAST, SCHOOL, LUNCH, PLAY, DINNER, PRAYER, BED }

/** A moment of the day the child recognises from real life. */
data class RoutineItem(
    val id: String,
    val icon: RoutineIcon,
    val name: LocalizedText,
    val time: ClockTime,
    /** Sentence used by the "real life challenges": "School starts at 7:30." */
    val sentence: LocalizedText
)

object RoutineCatalog {
    private fun item(
        id: String,
        icon: RoutineIcon,
        enName: String,
        arName: String,
        hour: Int,
        minute: Int,
        enSentence: String,
        arSentence: String
    ) = RoutineItem(
        id = id,
        icon = icon,
        name = LocalizedText(enName, arName),
        time = ClockTime(hour, minute),
        sentence = LocalizedText(enSentence, arSentence)
    )

    val items: List<RoutineItem> = listOf(
        item("wake_up", RoutineIcon.WAKE_UP, "Wake up", "الاستيقاظ", 7, 0,
            "I wake up at 7:00 in the morning.", "أستيقظ في السابعة صباحاً."),
        item("breakfast", RoutineIcon.BREAKFAST, "Breakfast", "الفطور", 7, 30,
            "Breakfast is at half past seven.", "وجبة الفطور في السابعة والنصف."),
        item("school", RoutineIcon.SCHOOL, "School starts", "المدرسة", 8, 0,
            "School starts at 8:00.", "تبدأ المدرسة في الثامنة."),
        item("lunch", RoutineIcon.LUNCH, "Lunch", "الغداء", 12, 30,
            "Lunch is at half past twelve.", "وجبة الغداء في الثانية عشرة والنصف."),
        item("play", RoutineIcon.PLAY, "Playtime", "وقت اللعب", 4, 30,
            "Playtime is at half past four.", "وقت اللعب في الرابعة والنصف."),
        item("prayer", RoutineIcon.PRAYER, "Prayer time", "وقت الصلاة", 6, 15,
            "Prayer time is at quarter past six.", "وقت الصلاة في السادسة والربع."),
        item("dinner", RoutineIcon.DINNER, "Dinner", "العشاء", 7, 30,
            "Dinner is at half past seven.", "وجبة العشاء في السابعة والنصف."),
        item("bed", RoutineIcon.BED, "Bedtime", "وقت النوم", 8, 30,
            "Bedtime is at half past eight.", "وقت النوم في الثامنة والنصف.")
    )

    fun byId(id: String): RoutineItem = items.first { it.id == id }
}
