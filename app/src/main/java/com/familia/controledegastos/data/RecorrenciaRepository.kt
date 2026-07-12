package com.familia.controledegastos.data

import com.familia.controledegastos.model.Recorrencia
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RecorrenciaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun colecao(familiaId: String) =
        db.collection("familias")
            .document(familiaId)
            .collection("recorrencias")

    fun observar(familiaId: String): Flow<List<Recorrencia>> = callbackFlow {
        val inscricao = colecao(familiaId)
            .orderBy("diaVencimento")
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Recorrencia::class.java).orEmpty())
            }
        awaitClose { inscricao.remove() }
    }

    // Busca única (sem ouvinte) — usada pelo lembrete em segundo plano.
    suspend fun listar(familiaId: String): List<Recorrencia> =
        colecao(familiaId).get().await().toObjects(Recorrencia::class.java)

    // id em branco = conta nova; com id = edição da existente.
    suspend fun salvar(familiaId: String, recorrencia: Recorrencia) {
        if (recorrencia.id.isBlank()) {
            colecao(familiaId).add(recorrencia).await()
        } else {
            colecao(familiaId).document(recorrencia.id).set(recorrencia).await()
        }
    }

    suspend fun remover(familiaId: String, recorrenciaId: String) {
        colecao(familiaId).document(recorrenciaId).delete().await()
    }
}
