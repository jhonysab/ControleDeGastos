package com.familia.controledegastos.ui.theme

import androidx.compose.ui.graphics.Color
import com.familia.controledegastos.model.Categoria

// Cada categoria tem uma cor fixa nos gráficos — a pessoa aprende
// "laranja = mercado" e nunca mais muda.
fun Categoria.cor(): Color = when (this) {
    Categoria.MERCADO -> Color(0xFFEF6C00)
    Categoria.LUZ -> Color(0xFFF9A825)
    Categoria.AGUA -> Color(0xFF29B6F6)
    Categoria.INTERNET -> Color(0xFF5C6BC0)
    Categoria.GASOLINA -> Color(0xFF8D6E63)
    Categoria.TRANSPORTE -> Color(0xFF26A69A)
    Categoria.SAUDE -> Color(0xFFEC407A)
    Categoria.CASA -> Color(0xFF7E57C2)
    Categoria.LAZER -> Color(0xFF9CCC65)
    Categoria.SALARIO -> VerdeGanho
    Categoria.OUTROS -> Color(0xFF90A4AE)
}
