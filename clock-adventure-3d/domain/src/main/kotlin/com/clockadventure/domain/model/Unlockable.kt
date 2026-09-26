package com.clockadventure.domain.model

/** Visual style of the interactive analog clock. */
enum class ClockStyle(val label: LocalizedText) {
    CANDY(LocalizedText("Candy", "حلوى")),
    OCEAN(LocalizedText("Ocean", "المحيط")),
    SPACE(LocalizedText("Space", "الفضاء")),
    JUNGLE(LocalizedText("Jungle", "الغابة")),
    SUNSET(LocalizedText("Sunset", "الغروب")),
    GALAXY(LocalizedText("Galaxy", "المجرة"))
}

/** Background / colour theme of the whole app. */
enum class AppTheme(val label: LocalizedText) {
    SKY(LocalizedText("Sky", "السماء")),
    CANDY(LocalizedText("Candy", "حلوى")),
    FOREST(LocalizedText("Forest", "الغابة")),
    SPACE(LocalizedText("Space", "الفضاء")),
    DESERT(LocalizedText("Desert", "الصحراء"))
}

/** The friendly character that accompanies the child. */
enum class MascotId(val label: LocalizedText) {
    TICKY(LocalizedText("Ticky the Owl", "تيكي البومة")),
    ROBO(LocalizedText("Robo Tick", "روبو تيك")),
    KITTY(LocalizedText("Kitty Clock", "القطة تايم")),
    SPARKY(LocalizedText("Sparky Star", "سباركي النجمة"))
}

enum class UnlockableType { CLOCK_STYLE, THEME, CHARACTER }

/**
 * Something the child can buy with coins or earn with stars in the Rewards screen.
 * Items flagged [isDefault] are available from the very first launch.
 */
data class Unlockable(
    val id: String,
    val type: UnlockableType,
    val refId: String,
    val name: LocalizedText,
    val description: LocalizedText,
    val coinCost: Int,
    val requiredStars: Int,
    val isDefault: Boolean = false
)

data class UnlockState(val unlockable: Unlockable, val unlocked: Boolean)

object RewardCatalog {
    private fun clock(style: ClockStyle, coins: Int, stars: Int, isDefault: Boolean) = Unlockable(
        id = "clock_${style.name.lowercase()}",
        type = UnlockableType.CLOCK_STYLE,
        refId = style.name,
        name = style.label,
        description = LocalizedText(
            "A ${style.label.en.lowercase()} clock face",
            "وجه ساعة ${style.label.ar}"
        ),
        coinCost = coins,
        requiredStars = stars,
        isDefault = isDefault
    )

    private fun theme(theme: AppTheme, coins: Int, stars: Int, isDefault: Boolean) = Unlockable(
        id = "theme_${theme.name.lowercase()}",
        type = UnlockableType.THEME,
        refId = theme.name,
        name = theme.label,
        description = LocalizedText("${theme.label.en} background", "خلفية ${theme.label.ar}"),
        coinCost = coins,
        requiredStars = stars,
        isDefault = isDefault
    )

    private fun character(mascot: MascotId, coins: Int, stars: Int, isDefault: Boolean) = Unlockable(
        id = "char_${mascot.name.lowercase()}",
        type = UnlockableType.CHARACTER,
        refId = mascot.name,
        name = mascot.label,
        description = LocalizedText("Play together with ${mascot.label.en}", "اللعب مع ${mascot.label.ar}"),
        coinCost = coins,
        requiredStars = stars,
        isDefault = isDefault
    )

    val items: List<Unlockable> = listOf(
        clock(ClockStyle.CANDY, coins = 0, stars = 0, isDefault = true),
        clock(ClockStyle.OCEAN, coins = 40, stars = 6, isDefault = false),
        clock(ClockStyle.JUNGLE, coins = 80, stars = 14, isDefault = false),
        clock(ClockStyle.SUNSET, coins = 120, stars = 22, isDefault = false),
        clock(ClockStyle.SPACE, coins = 180, stars = 32, isDefault = false),
        clock(ClockStyle.GALAXY, coins = 250, stars = 45, isDefault = false),

        theme(AppTheme.SKY, coins = 0, stars = 0, isDefault = true),
        theme(AppTheme.CANDY, coins = 30, stars = 4, isDefault = false),
        theme(AppTheme.FOREST, coins = 70, stars = 12, isDefault = false),
        theme(AppTheme.DESERT, coins = 110, stars = 20, isDefault = false),
        theme(AppTheme.SPACE, coins = 160, stars = 30, isDefault = false),

        character(MascotId.TICKY, coins = 0, stars = 0, isDefault = true),
        character(MascotId.ROBO, coins = 50, stars = 8, isDefault = false),
        character(MascotId.KITTY, coins = 100, stars = 18, isDefault = false),
        character(MascotId.SPARKY, coins = 150, stars = 28, isDefault = false)
    )

    fun byId(id: String): Unlockable = items.first { it.id == id }

    fun forClockStyle(style: ClockStyle): Unlockable = items.first { it.type == UnlockableType.CLOCK_STYLE && it.refId == style.name }
    fun forTheme(theme: AppTheme): Unlockable = items.first { it.type == UnlockableType.THEME && it.refId == theme.name }
    fun forMascot(mascot: MascotId): Unlockable = items.first { it.type == UnlockableType.CHARACTER && it.refId == mascot.name }
}
