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
    onTertiary = Color.White
)

@Composable
fun ControleDeGastosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
