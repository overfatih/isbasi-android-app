package com.profplay.isbasi.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = IsbasiGreenSecondary, // Gece modunda biraz daha açık yeşil daha iyi okunur
    secondary = IsbasiGreenTertiary,
    tertiary = IsbasiGreenPrimary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IsbasiGreenPrimary, // Ana butonlar bu renk olacak
    secondary = IsbasiGreenSecondary,
    tertiary = IsbasiGreenTertiary,
    background = IsbasiBackground,
    surface = IsbasiSurface,
    onPrimary = IsbasiOnPrimary, // Buton üzerindeki yazı rengi (Beyaz)
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = IsbasiOnBackground,
    onSurface = IsbasiOnBackground,
)

@Composable
fun IsbasiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color Android 12+ için duvar kağıdına göre renk değiştirir.
    // Marka rengimizi korumak için bunu 'false' yapmanı öneririm.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Durum Çubuğu (Status Bar) Rengini Ayarlama
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar'ı da yeşil yapalım, bütünlük sağlar
            window.statusBarColor = colorScheme.primary.toArgb()
            // İkonlar beyaz olsun (Çünkü zemin koyu yeşil)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}