package com.familia.controledegastos.model

import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

// "caixa" -> "Caixa". Sem isso a mesma coisa digitada de dois jeitos
// vira duas categorias diferentes na lista ("caixa" e "Caixa").
fun String.primeiraMaiuscula(): String =
    trim().replaceFirstChar { it.titlecase(PT_BR) }
