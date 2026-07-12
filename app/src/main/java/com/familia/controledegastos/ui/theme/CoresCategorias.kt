package com.familia.controledegastos.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.familia.controledegastos.model.Categoria

// A cor agora vem do próprio dado da categoria (corHex). Se o hex
// estiver inválido por algum motivo, cai num cinza neutro.
fun Categoria.cor(): Color = corDoHex(corHex)

fun corDoHex(hex: String): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(Color(0xFF90A4AE))
