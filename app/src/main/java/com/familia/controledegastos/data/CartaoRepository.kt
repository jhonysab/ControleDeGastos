package com.familia.controledegastos.data

import com.familia.controledegastos.model.Cartao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CartaoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun colecao(familiaId: String) =
        db.collection("familias")
            .document(familiaId)
            .collection("cartoes")

    fun observar(familiaId: String): Flow<List<Cartao>> = callbackFlow {
        val inscricao = colecao(familiaId)
            .orderBy("nome")
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Cartao::class.java).orEmpty())
            }
        awaitClose { inscricao.remove() }
    }

    suspend fun salvar(familiaId: String, cartao: Cartao) {
        if (cartao.id.isBlank()) {
            colecao(familiaId).add(cartao).await()
        } else {
            colecao(familiaId).document(cartao.id).set(cartao).await()
        }
    }

    suspend fun remover(familiaId: String, cartaoId: String) {
        colecao(familiaId).document(cartaoId).delete().await()
    }
}
