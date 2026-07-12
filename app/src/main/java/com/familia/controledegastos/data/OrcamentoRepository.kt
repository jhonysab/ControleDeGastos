package com.familia.controledegastos.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Limites mensais por categoria, compartilhados pela família.
// O id do documento é o ID da categoria.
class OrcamentoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun colecao(familiaId: String) =
        db.collection("familias")
            .document(familiaId)
            .collection("orcamentos")

    fun observarOrcamentos(familiaId: String): Flow<Map<String, Long>> = callbackFlow {
        val inscricao = colecao(familiaId).addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                close(erro)
                return@addSnapshotListener
            }
            val mapa = snapshot?.documents.orEmpty().mapNotNull { doc ->
                val limite = doc.getLong("limiteCentavos") ?: return@mapNotNull null
                doc.id to limite
            }.toMap()
            trySend(mapa)
        }
        awaitClose { inscricao.remove() }
    }

    suspend fun definir(familiaId: String, categoriaId: String, limiteCentavos: Long) {
        colecao(familiaId).document(categoriaId)
            .set(mapOf("limiteCentavos" to limiteCentavos)).await()
    }

    suspend fun remover(familiaId: String, categoriaId: String) {
        colecao(familiaId).document(categoriaId).delete().await()
    }
}
