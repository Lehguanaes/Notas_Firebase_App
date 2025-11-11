package com.example.firebase.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Mapeamos suas cores para o tema escuro
private val DarkColorScheme = darkColorScheme(
    primary = primaryColor,         // Seu rosa principal
    secondary = detailsColor,       // Seu rosa de detalhes
    background = backgroundColor,   // Seu fundo preto
    surface = cardBackground,       // O fundo do seu Card
    onPrimary = textColor,          // Texto sobre o botão rosa (Branco)
    onSecondary = textColor,        // Texto sobre a cor de detalhe (Branco)
    onBackground = textColor,       // Texto sobre o fundo preto (Branco)
    onSurface = textColor,          // Texto sobre o fundo do Card (Branco)
    tertiary = backgroundColorNote, // Fundo das notas

    // Cores Específicas para o TextField
    surfaceVariant = cardBackground,  // Fundo do CustomDarkTextField
    onSurfaceVariant = labelColor     // Cor do label (Gray)
)

// 2. O tema claro pode ficar com os padrões,
//    já que seu app é focado no modo escuro.
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    /* Você pode customizar o tema claro aqui se quiser */
)

@Composable
fun FirebaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Deixe false para forçar suas cores
    content: @Composable () -> Unit
) {
    // 3. Usamos o DarkColorScheme que acabamos de criar
    val colorScheme = when {
        // Ignora o 'darkTheme' e força o seu tema escuro
        // Se quiser que o app respeite o modo do sistema,
        // mude (true) para (darkTheme)
        (true) -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Cor da barra de status
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}