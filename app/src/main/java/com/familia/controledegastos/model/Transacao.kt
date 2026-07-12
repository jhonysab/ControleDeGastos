package com.familia.controledegastos.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.text.NumberFormat
import java.util.Locale

enum class TipoTransacao {
    GASTO,
    GANHO
}

// Todos os campos têm valor padrão porque o Firestore exige um
// construtor vazio para transformar documentos de volta em objetos.
data class Transacao(
    @DocumentId val id: String = "",
    val tipo: TipoTransacao = TipoTransacao.GASTO,
    val valorCentavos: Long = 0L,
    val categoria: Categoria = Categoria.OUTROS,
    val descricao: String = "",
    val data: Timestamp = Timestamp.now(),
    val criadoPor: String = "",
    // Se nasceu de uma conta recorrente, guarda o id dela —
    // é assim que a aba Contas sabe que o mês está pago.
    val recorrenciaId: String = ""
) {
    fun valorFormatado(): String = formatarCentavos(valorCentavos)
}

// Dinheiro é guardado em centavos (inteiro, matemática exata);
// a vírgula só existe na hora de mostrar na tela.
fun formatarCentavos(centavos: Long): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        .format(centavos / 100.0)
