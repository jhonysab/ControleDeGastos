package com.familia.controledegastos.data

import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.CategoriasPadrao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CategoriaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun colecao(familiaId: String) =
        db.collection("familias")
            .document(familiaId)
            .collection("categorias")

    fun observar(familiaId: String): Flow<List<Categoria>> = callbackFlow {
        val inscricao = colecao(familiaId)
            .orderBy("nome")
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Categoria::class.java).orEmpty())
            }
        awaitClose { inscricao.remove() }
    }

    // Semeia as categorias iniciais só se a família ainda não tem nenhuma.
    // Ids fixos + set = idempotente (dois celulares semeando não duplicam).
    suspend fun semearSePreciso(familiaId: String) {
        val existentes = colecao(familiaId).limit(1).get().await()
        if (!existentes.isEmpty) return
        CategoriasPadrao.lista.forEach { categoria ->
            colecao(familiaId).document(categoria.id).set(categoria).await()
        }
    }

    // id em branco = nova (Firestore gera o id); com id = edição.
    suspend fun salvar(familiaId: String, categoria: Categoria) {
        if (categoria.id.isBlank()) {
            colecao(familiaId).add(categoria.copy(id = "")).await()
        } else {
            colecao(familiaId).document(categoria.id).set(categoria).await()
        }
    }

    suspend fun arquivar(familiaId: String, categoriaId: String, arquivada: Boolean) {
        colecao(familiaId).document(categoriaId).update("arquivada", arquivada).await()
    }
}
