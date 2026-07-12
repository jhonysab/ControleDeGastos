package com.familia.controledegastos.data

import com.familia.controledegastos.model.Transacao
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Única classe do app que sabe que as transações moram no Firestore.
// As telas pedem dados a ela sem conhecer a nuvem.
class TransacaoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun colecao(familiaId: String) =
        db.collection("familias")
            .document(familiaId)
            .collection("transacoes")

    // Flow "vivo": o snapshot listener re-emite a lista inteira a cada
    // mudança no banco — inclusive quando o outro celular lança um gasto.
    fun observarTransacoes(familiaId: String): Flow<List<Transacao>> = callbackFlow {
        val inscricao = colecao(familiaId)
            .orderBy("data", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Transacao::class.java).orEmpty())
            }
        awaitClose { inscricao.remove() }
    }

    // Busca única (sem ouvinte) — usada pelo lembrete em segundo plano.
    suspend fun listarPeriodo(familiaId: String, de: Timestamp, ate: Timestamp): List<Transacao> =
        colecao(familiaId)
            .whereGreaterThanOrEqualTo("data", de)
            .whereLessThan("data", ate)
            .get().await()
            .toObjects(Transacao::class.java)

    suspend fun adicionar(familiaId: String, transacao: Transacao) {
        colecao(familiaId).add(transacao).await()
    }

    suspend fun remover(familiaId: String, transacaoId: String) {
        colecao(familiaId).document(transacaoId).delete().await()
    }
}
