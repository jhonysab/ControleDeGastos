package com.familia.controledegastos.data

import com.familia.controledegastos.model.Categoria
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Limites mensais por categoria, compartilhados pela família.
// Cada limite é um documento cujo id é o nome da categoria.
class OrcamentoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun colecao(familiaId: String) =
        db.collection("familias")
            .document(familiaId)
            .collection("orcamentos")

    fun observarOrcamentos(familiaId: String): Flow<Map<Categoria, Long>> = callbackFlow {
        val inscricao = colecao(familiaId).addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                close(erro)
                return@addSnapshotListener
            }
            val mapa = snapshot?.documents.orEmpty().mapNotNull { doc ->
                // Se um dia uma categoria sair do app, o doc órfão é ignorado.
                val categoria = runCatching { Categoria.valueOf(doc.id) }.getOrNull()
                    ?: return@mapNotNull null
                val limite = doc.getLong("limiteCentavos") ?: return@mapNotNull null
                categoria to limite
            }.toMap()
            trySend(mapa)
        }
        awaitClose { inscricao.remove() }
    }

    suspend fun definir(familiaId: String, categoria: Categoria, limiteCentavos: Long) {
        colecao(familiaId).document(categoria.name)
            .set(mapOf("limiteCentavos" to limiteCentavos)).await()
    }

    suspend fun remover(familiaId: String, categoria: Categoria) {
        colecao(familiaId).document(categoria.name).delete().await()
    }
}
