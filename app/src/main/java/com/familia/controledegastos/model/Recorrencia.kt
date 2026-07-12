package com.familia.controledegastos.model

import com.google.firebase.firestore.DocumentId

// Conta que se repete todo mês (luz, água, internet, salário...).
// É um "modelo": o valor real é confirmado na hora de lançar,
// porque conta de luz varia — aqui fica só o esperado.
data class Recorrencia(
    @DocumentId val id: String = "",
    val descricao: String = "",
    val tipo: TipoTransacao = TipoTransacao.GASTO,
    val categoria: Categoria = Categoria.OUTROS,
    val valorEsperadoCentavos: Long = 0L,
    val diaVencimento: Int = 1
)
