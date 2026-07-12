package com.familia.controledegastos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familia.controledegastos.data.AuthRepository
import com.familia.controledegastos.model.Usuario
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

// Em qual "momento" da jornada o usuário está — a MainActivity
// escolhe qual tela mostrar olhando só para isso.
sealed interface EstadoAuth {
    data object Carregando : EstadoAuth
    data object Deslogado : EstadoAuth
    data class SemFamilia(val usuario: Usuario) : EstadoAuth
    data class Pronto(val usuario: Usuario) : EstadoAuth
}

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    var estado by mutableStateOf<EstadoAuth>(EstadoAuth.Carregando)
        private set
    var processando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    init {
        verificarSessao()
    }

    // Se a pessoa já logou antes, o Firebase lembra — vai direto pra dentro.
    private fun verificarSessao() {
        val uid = repo.uidAtual
        if (uid == null) {
            estado = EstadoAuth.Deslogado
            return
        }
        viewModelScope.launch {
            estado = try {
                val usuario = repo.carregarUsuario(uid)
                when {
                    usuario == null -> {
                        repo.sair()
                        EstadoAuth.Deslogado
                    }
                    usuario.familiaId.isBlank() -> EstadoAuth.SemFamilia(usuario)
                    else -> EstadoAuth.Pronto(usuario)
                }
            } catch (e: Exception) {
                erro = traduzErro(e)
                EstadoAuth.Deslogado
            }
        }
    }

    fun login(email: String, senha: String) {
        if (email.isBlank() || senha.isBlank()) {
            erro = "Preencha e-mail e senha."
            return
        }
        executar {
            val usuario = repo.login(email.trim(), senha)
            estado = if (usuario.familiaId.isBlank()) {
                EstadoAuth.SemFamilia(usuario)
            } else {
                EstadoAuth.Pronto(usuario)
            }
        }
    }

    fun cadastrar(nome: String, email: String, senha: String) {
        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            erro = "Preencha todos os campos."
            return
        }
        executar {
            estado = EstadoAuth.SemFamilia(repo.cadastrar(nome.trim(), email.trim(), senha))
        }
    }

    fun criarFamilia(nomeFamilia: String) {
        val usuario = (estado as? EstadoAuth.SemFamilia)?.usuario ?: return
        if (nomeFamilia.isBlank()) {
            erro = "Dê um nome para a família."
            return
        }
        executar {
            val id = repo.criarFamilia(nomeFamilia.trim(), usuario.id)
            estado = EstadoAuth.Pronto(usuario.copy(familiaId = id))
        }
    }

    fun entrarNaFamilia(codigo: String) {
        val usuario = (estado as? EstadoAuth.SemFamilia)?.usuario ?: return
        if (codigo.isBlank()) {
            erro = "Digite o código da família."
            return
        }
        executar {
            val id = repo.entrarNaFamilia(codigo, usuario.id)
            estado = EstadoAuth.Pronto(usuario.copy(familiaId = id))
        }
    }

    fun atualizarNome(novoNome: String) {
        val usuario = (estado as? EstadoAuth.Pronto)?.usuario ?: return
        if (novoNome.isBlank()) {
            erro = "Digite um nome."
            return
        }
        executar {
            repo.atualizarNome(usuario.id, novoNome.trim())
            estado = EstadoAuth.Pronto(usuario.copy(nome = novoNome.trim()))
        }
    }

    fun sair() {
        repo.sair()
        estado = EstadoAuth.Deslogado
    }

    fun limparErro() {
        erro = null
    }

    private fun executar(bloco: suspend () -> Unit) {
        viewModelScope.launch {
            processando = true
            erro = null
            try {
                bloco()
            } catch (e: Exception) {
                erro = traduzErro(e)
            }
            processando = false
        }
    }

    // Erros do Firebase vêm em inglês técnico; os pais merecem português claro.
    private fun traduzErro(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
        is FirebaseAuthInvalidUserException -> "Não existe conta com esse e-mail."
        is FirebaseAuthUserCollisionException -> "Já existe uma conta com esse e-mail."
        is FirebaseAuthWeakPasswordException -> "A senha precisa ter pelo menos 6 caracteres."
        is FirebaseNetworkException -> "Sem internet. Verifique a conexão e tente de novo."
        is IllegalArgumentException, is IllegalStateException ->
            e.message ?: "Aconteceu um erro inesperado."
        else -> e.message ?: "Aconteceu um erro inesperado."
    }
}
