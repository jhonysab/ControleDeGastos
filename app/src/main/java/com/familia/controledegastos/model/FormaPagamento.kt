package com.familia.controledegastos.model

enum class FormaPagamento(val rotulo: String) {
    DINHEIRO("Dinheiro"),
    PIX("Pix"),
    DEBITO("Débito"),
    CREDITO("Crédito")
}
