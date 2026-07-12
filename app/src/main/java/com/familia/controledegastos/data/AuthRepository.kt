package com.familia.controledegastos.data

import com.familia.controledegastos.model.Familia
import com.familia.controledegastos.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

// Cuida de login/cadastro (Firebase Auth) e da ponte com as
// coleções usuarios e familias no Firestore.
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val uidAtual: String?
        get() = auth.currentUser?.uid

    suspend fun login(email: String, senha: String): Usuario {
        val resultado = auth.signInWithEmailAndPassword(email, senha).await()
        val uid = resultado.user!!.uid
        return carregarUsuario(uid)
            ?: throw IllegalStateException("Conta sem cadastro completo. Crie a conta de novo.")
    }

    suspend fun cadastrar(nome: String, email: String, senha: String): Usuario {
        val resultado = auth.createUserWithEmailAndPassword(email, senha).await()
        val uid = resultado.user!!.uid
        val usuario = Usuario(id = uid, nome = nome, familiaId = "")
        db.collection("usuarios").document(uid).set(usuario).await()
        return usuario
    }

    suspend fun carregarUsuario(uid: String): Usuario? =
        db.collection("usuarios").document(uid).get().await()
            .toObject(Usuario::class.java)

    suspend fun atualizarNome(uid: String, novoNome: String) {
        db.collection("usuarios").document(uid).update("nome", novoNome).await()
    }

    // Todos os perfis da família — usado no filtro "quem gastou o quê".
    suspend fun carregarMembros(familiaId: String): List<Usuario> =
        db.collection("usuarios")
            .whereEqualTo("familiaId", familiaId)
            .get().await()
            .toObjects(Usuario::class.java)

    suspend fun criarFamilia(nomeFamilia: String, uid: String): String {
        val ref = db.collection("familias")
            .add(Familia(nome = nomeFamilia, membros = listOf(uid)))
            .await()
        db.collection("usuarios").document(uid).update("familiaId", ref.id).await()
        return ref.id
    }

    // O "código da família" é o próprio id do documento no Firestore.
    // Não dá pra LER a família antes de ser membro (as regras proíbem),
    // então tentamos entrar direto e traduzimos a recusa.
    suspend fun entrarNaFamilia(codigoDigitado: String, uid: String): String {
        val codigo = codigoDigitado.trim()
        try {
            db.collection("familias").document(codigo)
                .update("membros", FieldValue.arrayUnion(uid)).await()
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.NOT_FOUND,
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    throw IllegalArgumentException("Família não encontrada. Confira o código.")
                else -> throw e
            }
        }
        db.collection("usuarios").document(uid).update("familiaId", codigo).await()
        return codigo
    }

    fun sair() {
        auth.signOut()
    }
}
