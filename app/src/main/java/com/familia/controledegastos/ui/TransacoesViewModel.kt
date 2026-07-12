package com.familia.controledegastos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familia.controledegastos.data.TransacaoRepository
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Transacao
import com.google.firebase.Timestamp
import java.time.YearMonth
import java.time.ZoneId
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
    private val repo: TransacaoRepository = TransacaoRepository()
) : ViewModel() {

    var transacoes by mutableStateOf<List<Transacao>>(emptyList())
        private set
    var mesSelecionado by mutableStateOf(YearMonth.now())
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    private var escuta: Job? = null

    init {
        iniciarEscuta()
    }

    // Liga (ou religa) o ouvido na nuvem: qualquer mudança no banco
    // (deste celular ou do outro) atualiza a lista sozinha.
    // Um listener que recebe erro morre — por isso precisa ser religável.
    fun iniciarEscuta() {
        if (escuta?.isActive == true) return
        erro = null
        escuta = viewModelScope.launch {
            repo.observarTransacoes(familiaId)
                .catch { erro = "Não foi possível carregar: ${it.message}" }
                .collect { transacoes = it }
        }
    }

    // Desliga o ouvido ANTES do logout — um listener ativo sem login
    // é recusado pelo servidor (PERMISSION_DENIED).
    fun pararEscuta() {
        escuta?.cancel()
        escuta = null
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

    fun adicionar(tipo: TipoTransacao, valorCentavos: Long, categoria: Categoria, descricao: String) {
        viewModelScope.launch {
            try {
                repo.adicionar(
                    familiaId,
                    Transacao(
                        tipo = tipo,
                        valorCentavos = valorCentavos,
                        categoria = categoria,
                        descricao = descricao.trim(),
                        data = Timestamp.now(),
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
