package com.familia.controledegastos.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familia.controledegastos.data.AuthRepository
import com.familia.controledegastos.data.RecorrenciaRepository
import com.familia.controledegastos.data.TransacaoRepository
import com.familia.controledegastos.model.TipoTransacao
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// Roda uma vez por dia (mesmo com o app fechado): procura contas
// recorrentes ainda não lançadas que vencem hoje ou amanhã e avisa.
class LembreteVencimentoWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        // Sem login, sem lembrete — e sem erro.
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        return try {
            val usuario = AuthRepository().carregarUsuario(uid) ?: return Result.success()
            if (usuario.familiaId.isBlank()) return Result.success()

            val hoje = LocalDate.now()
            val zona = ZoneId.systemDefault()
            val inicioDoMes = hoje.withDayOfMonth(1)
            val inicioDoProximo = inicioDoMes.plusMonths(1)

            val pagas = TransacaoRepository()
                .listarPeriodo(
                    usuario.familiaId,
                    Timestamp(Date.from(inicioDoMes.atStartOfDay(zona).toInstant())),
                    Timestamp(Date.from(inicioDoProximo.atStartOfDay(zona).toInstant()))
                )
                .mapNotNull { it.recorrenciaId.ifBlank { null } }
                .toSet()

            val mensagens = RecorrenciaRepository().listar(usuario.familiaId)
                .filter { it.tipo == TipoTransacao.GASTO && it.id !in pagas }
                .mapNotNull { conta ->
                    // dia 31 num mês de 30? vence no último dia do mês
                    val vencimento = hoje.withDayOfMonth(
                        minOf(conta.diaVencimento, hoje.lengthOfMonth())
                    )
                    when (vencimento) {
                        hoje -> "${conta.descricao} vence HOJE"
                        hoje.plusDays(1) -> "${conta.descricao} vence amanhã"
                        else -> null
                    }
                }

            Notificacoes.avisarVencimentos(applicationContext, mensagens)
            Result.success()
        } catch (e: Exception) {
            // sem internet agora? o WorkManager tenta de novo mais tarde
            Result.retry()
        }
    }
}
