package com.familia.controledegastos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.familia.controledegastos.notificacoes.LembreteVencimentoWorker
import com.familia.controledegastos.ui.AuthViewModel
import com.familia.controledegastos.ui.EstadoAuth
import com.familia.controledegastos.ui.telas.TelaCadastro
import com.familia.controledegastos.ui.telas.TelaFamilia
import com.familia.controledegastos.ui.telas.TelaLogin
import com.familia.controledegastos.ui.telas.TelaPrincipal
import com.familia.controledegastos.ui.theme.ControleDeGastosTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        agendarLembretes()
        pedirPermissaoDeNotificacao()
        setContent {
            ControleDeGastosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // Checagem diária de contas perto do vencimento, mesmo com o app
    // fechado. KEEP: se já está agendada, não duplica.
    private fun agendarLembretes() {
        val pedido = PeriodicWorkRequestBuilder<LembreteVencimentoWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "lembretes-vencimento",
            ExistingPeriodicWorkPolicy.KEEP,
            pedido
        )
    }

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}

// O estado do AuthViewModel decide qual tela aparece — sem biblioteca
// de navegação por enquanto: menos peças, mesma função.
@Composable
fun App(modifier: Modifier = Modifier, vm: AuthViewModel = viewModel()) {
    var mostrandoCadastro by remember { mutableStateOf(false) }

    when (val estado = vm.estado) {
        is EstadoAuth.Carregando -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is EstadoAuth.Deslogado -> {
            if (mostrandoCadastro) {
                TelaCadastro(
                    processando = vm.processando,
                    erro = vm.erro,
                    aoCadastrar = vm::cadastrar,
                    aoVoltarParaLogin = {
                        vm.limparErro()
                        mostrandoCadastro = false
                    },
                    modifier = modifier
                )
            } else {
                TelaLogin(
                    processando = vm.processando,
                    erro = vm.erro,
                    aoLogar = vm::login,
                    aoIrParaCadastro = {
                        vm.limparErro()
                        mostrandoCadastro = true
                    },
                    modifier = modifier
                )
            }
        }

        is EstadoAuth.SemFamilia -> {
            key(estado.usuario.id) {
                TelaFamilia(
                    processando = vm.processando,
                    erro = vm.erro,
                    aoCriarFamilia = vm::criarFamilia,
                    aoEntrarNaFamilia = vm::entrarNaFamilia,
                    modifier = modifier
                )
            }
        }

        is EstadoAuth.Pronto -> {
            key(estado.usuario.id) {
                TelaPrincipal(
                    usuario = estado.usuario,
                    aoSair = vm::sair,
                    aoAtualizarNome = vm::atualizarNome,
                    modifier = modifier
                )
            }
        }
    }
}

