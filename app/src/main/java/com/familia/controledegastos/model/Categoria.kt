package com.familia.controledegastos.model

import com.google.firebase.firestore.DocumentId

enum class TipoCategoria {
    GASTO,   // só aparece ao lançar um gasto
    GANHO,   // só aparece ao lançar um ganho
    AMBOS    // aparece nos dois
}

// Categoria personalizável da família. Guardada em familias/{id}/categorias.
// O id é fixo: renomear não quebra os lançamentos antigos (que apontam por id).
data class Categoria(
    @DocumentId val id: String = "",
    val nome: String = "",
    val corHex: String = "#90A4AE",
    val tipo: TipoCategoria = TipoCategoria.GASTO,
    // Arquivada de vez: some das opções de lançamento (o histórico
    // antigo continua mostrando o nome).
    val arquivada: Boolean = false,
    // Só some da aba Limites — continua valendo para lançar.
    // Serve para tirar da frente o que nunca vai ter limite.
    val ocultaNosLimites: Boolean = false
) {
    // .rotulo mantém compatibilidade com as telas que já usavam esse nome
    val rotulo: String get() = nome

    fun serveParaGasto() = tipo == TipoCategoria.GASTO || tipo == TipoCategoria.AMBOS
    fun serveParaGanho() = tipo == TipoCategoria.GANHO || tipo == TipoCategoria.AMBOS
}

// Categorias iniciais — semeadas na primeira vez com id fixo igual ao
// código antigo (MERCADO, LUZ...), então os lançamentos já existentes
// continuam batendo sem nenhuma migração.
object CategoriasPadrao {
    val lista: List<Categoria> = listOf(
        Categoria("MERCADO", "Mercado", "#EF6C00", TipoCategoria.GASTO),
        Categoria("LUZ", "Luz", "#F9A825", TipoCategoria.GASTO),
        Categoria("AGUA", "Água", "#29B6F6", TipoCategoria.GASTO),
        Categoria("INTERNET", "Internet", "#5C6BC0", TipoCategoria.GASTO),
        Categoria("GASOLINA", "Gasolina", "#8D6E63", TipoCategoria.GASTO),
        Categoria("TRANSPORTE", "Transporte", "#26A69A", TipoCategoria.GASTO),
        Categoria("SAUDE", "Saúde", "#EC407A", TipoCategoria.GASTO),
        Categoria("CASA", "Casa", "#7E57C2", TipoCategoria.GASTO),
        Categoria("LAZER", "Lazer", "#9CCC65", TipoCategoria.GASTO),
        Categoria("SALARIO", "Salário", "#2E7D32", TipoCategoria.GANHO),
        Categoria("OUTROS", "Outros", "#90A4AE", TipoCategoria.AMBOS)
    )

    // Paleta oferecida ao criar/editar uma categoria
    val paleta: List<String> = listOf(
        "#EF6C00", "#F9A825", "#29B6F6", "#5C6BC0", "#8D6E63", "#26A69A",
        "#EC407A", "#7E57C2", "#9CCC65", "#2E7D32", "#C62828", "#00897B",
        "#3949AB", "#00838F", "#6D4C41", "#90A4AE"
    )

    // Usada quando um lançamento aponta para uma categoria que sumiu.
    val removida = Categoria(id = "", nome = "Sem categoria", corHex = "#90A4AE", tipo = TipoCategoria.AMBOS)
}
