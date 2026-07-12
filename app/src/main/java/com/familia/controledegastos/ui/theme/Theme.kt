package com.familia.controledegastos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema fixo claro (pedido da casa: fundo claro, previsível para os pais —
// sem cor dinâmica do papel de parede, sem virar escuro sozinho).
private val EsquemaClaro = lightColorScheme(
    primary = PetroleoEscuro,
    onPrimary = Color.White,
    primaryContainer = PetroleoClaro,
    onPrimaryContainer = PetroleoProfundo,
    secondary = Petroleo,
    onSecondary = Color.White,
    secondaryContainer = PetroleoClaro,
    onSecondaryContainer = PetroleoProfundo,
    background = FundoClaro,
    surface = FundoClaro,
    surfaceVariant = PetroleoClaro,
    onSurfaceVariant = CinzaTexto,
    tertiary = Petroleo,
    onTertiary = Color.White,
    // Superfícies de cards/diálogos: sem sobrescrever, o Material 3
    // usa um padrão arroxeado que destoa da nossa paleta.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F4EF),
    surfaceContainer = Color(0xFFEEF0EA),
    surfaceContainerHigh = Color(0xFFE8EBE4),
    surfaceContainerHighest = Color(0xFFE2E6DF),
    outline = Color(0xFF74786F),
    outlineVariant = Color(0xFFC5CAC0)
)

@Composable
fun ControleDeGastosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
