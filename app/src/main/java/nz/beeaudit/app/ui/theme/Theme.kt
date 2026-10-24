package nz.beeaudit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF315C3A),
    onPrimary = Color.White,
    secondary = Color(0xFFF0B429),
    onSecondary = Color(0xFF1F2A24),
    tertiary = Color(0xFF006C7A),
    surface = Color(0xFFFFFBF2),
    surfaceVariant = Color(0xFFE6E0D2),
    onSurface = Color(0xFF1F2A24),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD49E),
    onPrimary = Color(0xFF15351C),
    secondary = Color(0xFFE5C15A),
    onSecondary = Color(0xFF342B00),
    tertiary = Color(0xFF79D3E2),
    surface = Color(0xFF171D18),
    surfaceVariant = Color(0xFF3F493F),
    onSurface = Color(0xFFE7F0E5),
)

@Composable
fun BeeAuditTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
