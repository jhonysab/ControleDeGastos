package com.familia.controledegastos.model

// Categorias fixas da v1. Se um dia os pais quiserem criar as próprias,
// isso migra para uma coleção no Firestore (Fase 5).
enum class Categoria(val rotulo: String) {
    MERCADO("Mercado"),
    LUZ("Luz"),
    AGUA("Água"),
    INTERNET("Internet"),
    GASOLINA("Gasolina"),
    TRANSPORTE("Transporte"),
    SAUDE("Saúde"),
    CASA("Casa"),
    LAZER("Lazer"),
    SALARIO("Salário"),
    OUTROS("Outros")
}
