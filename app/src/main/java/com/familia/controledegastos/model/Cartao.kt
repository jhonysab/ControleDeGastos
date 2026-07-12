package com.familia.controledegastos.model

import com.google.firebase.firestore.DocumentId

// Cartão de crédito da família (ex: "Nubank do Pai").
// O vencimento é informativo — decisão de produto: a compra conta
// no MÊS DA COMPRA, não no mês da fatura.
data class Cartao(
    @DocumentId val id: String = "",
    val nome: String = "",
    val diaVencimento: Int = 1
)
