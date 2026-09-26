package androidx.compose.material3
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Indication
import androidx.compose.ui.text.style.TextAlign

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(Float.NaN),
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(Float.NaN),
    textDecoration: androidx.compose.ui.text.style.TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(Float.NaN),
    overflow: Int = 0,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = TextStyle()
) = Unit

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null
) = Unit

object MaterialTheme {
    val colorScheme: ColorScheme get() = ColorScheme()
    val typography: Typography get() = Typography()
    val shapes: Shapes get() = Shapes()
}

@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = ColorScheme(),
    shapes: Shapes = Shapes(),
    typography: Typography = Typography(),
    content: @Composable () -> Unit
) = Unit

class ColorScheme {
    val primary: Color = Color.Unspecified
    val onPrimary: Color = Color.Unspecified
    val primaryContainer: Color = Color.Unspecified
    val onPrimaryContainer: Color = Color.Unspecified
    val secondary: Color = Color.Unspecified
    val onSecondary: Color = Color.Unspecified
    val secondaryContainer: Color = Color.Unspecified
    val onSecondaryContainer: Color = Color.Unspecified
    val tertiary: Color = Color.Unspecified
    val onTertiary: Color = Color.Unspecified
    val background: Color = Color.Unspecified
    val onBackground: Color = Color.Unspecified
    val surface: Color = Color.Unspecified
    val onSurface: Color = Color.Unspecified
    val surfaceVariant: Color = Color.Unspecified
    val onSurfaceVariant: Color = Color.Unspecified
    val error: Color = Color.Unspecified
    val onError: Color = Color.Unspecified
    val outline: Color = Color.Unspecified
    val surfaceTint: Color = Color.Unspecified
    val scrim: Color = Color.Unspecified
}

fun lightColorScheme(
    primary: Color = Color.Unspecified,
    onPrimary: Color = Color.Unspecified,
    primaryContainer: Color = Color.Unspecified,
    onPrimaryContainer: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    onSecondary: Color = Color.Unspecified,
    secondaryContainer: Color = Color.Unspecified,
    onSecondaryContainer: Color = Color.Unspecified,
    tertiary: Color = Color.Unspecified,
    onTertiary: Color = Color.Unspecified,
    tertiaryContainer: Color = Color.Unspecified,
    onTertiaryContainer: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    onBackground: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    onSurface: Color = Color.Unspecified,
    surfaceVariant: Color = Color.Unspecified,
    onSurfaceVariant: Color = Color.Unspecified,
    surfaceTint: Color = Color.Unspecified,
    error: Color = Color.Unspecified,
    onError: Color = Color.Unspecified,
    outline: Color = Color.Unspecified,
    scrim: Color = Color.Unspecified
): ColorScheme = ColorScheme()

fun darkColorScheme(
    primary: Color = Color.Unspecified,
    onPrimary: Color = Color.Unspecified,
    primaryContainer: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    onSecondary: Color = Color.Unspecified,
    secondaryContainer: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    onBackground: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    onSurface: Color = Color.Unspecified,
    error: Color = Color.Unspecified,
    onError: Color = Color.Unspecified
): ColorScheme = ColorScheme()

class Shapes(
    val extraSmall: Shape? = null,
    val small: Shape? = null,
    val medium: Shape? = null,
    val large: Shape? = null,
    val extraLarge: Shape? = null
)

class Typography(
    val displayLarge: TextStyle = TextStyle(),
    val displayMedium: TextStyle = TextStyle(),
    val displaySmall: TextStyle = TextStyle(),
    val headlineLarge: TextStyle = TextStyle(),
    val headlineMedium: TextStyle = TextStyle(),
    val headlineSmall: TextStyle = TextStyle(),
    val titleLarge: TextStyle = TextStyle(),
    val titleMedium: TextStyle = TextStyle(),
    val titleSmall: TextStyle = TextStyle(),
    val bodyLarge: TextStyle = TextStyle(),
    val bodyMedium: TextStyle = TextStyle(),
    val bodySmall: TextStyle = TextStyle(),
    val labelLarge: TextStyle = TextStyle(),
    val labelMedium: TextStyle = TextStyle(),
    val labelSmall: TextStyle = TextStyle()
)

class ButtonColors
class ButtonElevation

object ButtonDefaults {
    val ContentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()
    val shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(20)

    @Composable
    fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): ButtonColors = ButtonColors()

    @Composable
    fun buttonElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
        disabledElevation: Dp = 0.dp
    ): ButtonElevation = ButtonElevation()
}

object CardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): Any = Any()

    @Composable
    fun cardElevation(defaultElevation: Dp = 0.dp, pressedElevation: Dp = 0.dp): Any = Any()
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: Any? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) = Unit

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: Any? = null,
    content: @Composable () -> Unit
) = Unit

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12),
    colors: Any = CardDefaults.cardColors(),
    elevation: Any = CardDefaults.cardElevation(),
    border: Any? = null,
    content: @Composable () -> Unit
) = Unit

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: Any? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null
) = Unit

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: Any? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null
) = Unit

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null
) = Unit

@Composable
fun RadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) = Unit

@Composable
fun LinearProgressIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified, trackColor: Color = Color.Unspecified) = Unit

@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) = Unit

@Composable
fun ripple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified
): Indication = object : Indication {}

@Composable
fun Divider(modifier: Modifier = Modifier, color: Color = Color.Unspecified, thickness: Dp = 1.dp) = Unit
