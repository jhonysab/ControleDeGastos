package com.familia.controledegastos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familia.controledegastos.data.OrcamentoRepository
import com.familia.controledegastos.data.TransacaoRepository
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Transacao
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ResumoMensal(
    val mes: YearMonth,
    val ganhosCentavos: Long,
    val gastosCentavos: Long
)

class TransacoesViewModel(
    private val familiaId: String,
    private val uid: String,
    private val repo: TransacaoRepository = TransacaoRepository(),
    private val orcamentoRepo: OrcamentoRepository = OrcamentoRepository()
) : ViewModel() {

    var transacoes by mutableStateOf<List<Transacao>>(emptyList())
        private set
    var orcamentos by mutableStateOf<Map<Categoria, Long>>(emptyMap())
        private set
    var mesSelecionado by mutableStateOf(YearMonth.now())
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    private val escutas = mutableListOf<Job>()

    init {
        iniciarEscuta()
    }

    // Liga (ou religa) os ouvidos na nuvem: qualquer mudança no banco
    // (deste celular ou do outro) atualiza a tela sozinha.
    // Um listener que recebe erro morre — por isso precisa ser religável.
    fun iniciarEscuta() {
        if (escutas.any { it.isActive }) return
        erro = null
        escutas += viewModelScope.launch {
            repo.observarTransacoes(familiaId)
                .catch { erro = "Não foi possível carregar: ${it.message}" }
                .collect { transacoes = it }
        }
        escutas += viewModelScope.launch {
            orcamentoRepo.observarOrcamentos(familiaId)
                .catch { erro = "Não foi possível carregar os limites: ${it.message}" }
                .collect { orcamentos = it }
        }
    }

    // Desliga os ouvidos ANTES do logout — um listener ativo sem login
    // é recusado pelo servidor (PERMISSION_DENIED).
    fun pararEscuta() {
        escutas.forEach { it.cancel() }
        escutas.clear()
    }

    // Limite zero (ou negativo) significa "remover o limite".
    fun definirOrcamento(categoria: Categoria, limiteCentavos: Long) {
        viewModelScope.launch {
            try {
                if (limiteCentavos > 0) {
                    orcamentoRepo.definir(familiaId, categoria, limiteCentavos)
                } else {
                    orcamentoRepo.remover(familiaId, categoria)
                }
            } catch (e: Exception) {
                erro = "Não foi possível salvar o limite: ${e.message}"
            }
        }
    }

    val transacoesDoMes: List<Transacao>
        get() = transacoes.filter { mesDaTransacao(it) == mesSelecionado }

    val totalGanhosCentavos: Long
        get() = transacoesDoMes.filter { it.tipo == TipoTransacao.GANHO }.sumOf { it.valorCentavos }

    val totalGastosCentavos: Long
        get() = transacoesDoMes.filter { it.tipo == TipoTransacao.GASTO }.sumOf { it.valorCentavos }

    val saldoCentavos: Long
        get() = totalGanhosCentavos - totalGastosCentavos

    // Gastos do mês agrupados por categoria, do maior para o menor (donut).
    val gastosPorCategoria: List<Pair<Categoria, Long>>
        get() = transacoesDoMes
            .filter { it.tipo == TipoTransacao.GASTO }
            .groupBy { it.categoria }
            .map { (categoria, itens) -> categoria to itens.sumOf { it.valorCentavos } }
            .sortedByDescending { it.second }

    // Ganhos x gastos dos últimos [quantos] meses, terminando no mês
    // selecionado (gráfico de barras).
    fun resumoUltimosMeses(quantos: Int = 6): List<ResumoMensal> =
        (quantos - 1 downTo 0).map { atras ->
            val mes = mesSelecionado.minusMonths(atras.toLong())
            val doMes = transacoes.filter { mesDaTransacao(it) == mes }
            ResumoMensal(
                mes = mes,
                ganhosCentavos = doMes.filter { it.tipo == TipoTransacao.GANHO }.sumOf { it.valorCentavos },
                gastosCentavos = doMes.filter { it.tipo == TipoTransacao.GASTO }.sumOf { it.valorCentavos }
            )
        }

    fun mesAnterior() {
        mesSelecionado = mesSelecionado.minusMonths(1)
    }

    fun proximoMes() {
        mesSelecionado = mesSelecionado.plusMonths(1)
    }

    fun adicionar(
        tipo: TipoTransacao,
        valorCentavos: Long,
        categoria: Categoria,
        descricao: String,
        dia: LocalDate
    ) {
        viewModelScope.launch {
            try {
                repo.adicionar(
                    familiaId,
                    Transacao(
                        tipo = tipo,
                        valorCentavos = valorCentavos,
                        categoria = categoria,
                        descricao = descricao.trim(),
                        // meio-dia local: longe das bordas de fuso, o dia
                        // nunca "escorrega" para o mês vizinho
                        data = Timestamp(
                            Date.from(dia.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
                        ),
                        criadoPor = uid
                    )
                )
            } catch (e: Exception) {
                erro = "Não foi possível salvar: ${e.message}"
            }
        }
    }

    fun remover(transacaoId: String) {
        viewModelScope.launch {
            try {
                repo.remover(familiaId, transacaoId)
            } catch (e: Exception) {
                erro = "Não foi possível excluir: ${e.message}"
            }
        }
    }

    fun limparErro() {
        erro = null
    }

    private fun mesDaTransacao(t: Transacao): YearMonth =
        YearMonth.from(t.data.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
}
