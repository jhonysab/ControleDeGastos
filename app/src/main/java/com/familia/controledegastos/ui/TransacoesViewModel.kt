package com.familia.controledegastos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familia.controledegastos.data.AuthRepository
import com.familia.controledegastos.data.OrcamentoRepository
import com.familia.controledegastos.data.RecorrenciaRepository
import com.familia.controledegastos.data.TransacaoRepository
import com.familia.controledegastos.data.CartaoRepository
import com.familia.controledegastos.model.Cartao
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.FormaPagamento
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.Usuario
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

// Tudo que a tela de lançamento coleta, num pacote só.
data class DadosLancamento(
    val tipo: TipoTransacao,
    val valorCentavos: Long,
    val categoria: Categoria,
    val descricao: String,
    val dia: LocalDate,
    val recorrenciaId: String = "",
    val formaPagamento: FormaPagamento = FormaPagamento.DINHEIRO,
    val cartaoId: String = ""
)

class TransacoesViewModel(
    private val familiaId: String,
    private val uid: String,
    private val repo: TransacaoRepository = TransacaoRepository(),
    private val orcamentoRepo: OrcamentoRepository = OrcamentoRepository(),
    private val recorrenciaRepo: RecorrenciaRepository = RecorrenciaRepository(),
    private val cartaoRepo: CartaoRepository = CartaoRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    var transacoes by mutableStateOf<List<Transacao>>(emptyList())
        private set
    var orcamentos by mutableStateOf<Map<Categoria, Long>>(emptyMap())
        private set
    var recorrencias by mutableStateOf<List<Recorrencia>>(emptyList())
        private set
    var membros by mutableStateOf<List<Usuario>>(emptyList())
        private set
    var cartoes by mutableStateOf<List<Cartao>>(emptyList())
        private set

    // null = família toda; uid = só o que aquela pessoa lançou
    var filtroUid by mutableStateOf<String?>(null)
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
        escutas += viewModelScope.launch {
            recorrenciaRepo.observar(familiaId)
                .catch { erro = "Não foi possível carregar as contas: ${it.message}" }
                .collect { recorrencias = it }
        }
        escutas += viewModelScope.launch {
            cartaoRepo.observar(familiaId)
                .catch { erro = "Não foi possível carregar os cartões: ${it.message}" }
                .collect { cartoes = it }
        }
        viewModelScope.launch {
            // nomes mudam raramente: uma busca por sessão basta
            runCatching { membros = authRepo.carregarMembros(familiaId) }
        }
    }

    fun salvarCartao(cartao: Cartao) {
        viewModelScope.launch {
            try {
                cartaoRepo.salvar(familiaId, cartao)
            } catch (e: Exception) {
                erro = "Não foi possível salvar o cartão: ${e.message}"
            }
        }
    }

    fun removerCartao(cartaoId: String) {
        viewModelScope.launch {
            try {
                cartaoRepo.remover(familiaId, cartaoId)
            } catch (e: Exception) {
                erro = "Não foi possível excluir o cartão: ${e.message}"
            }
        }
    }

    fun definirFiltro(uid: String?) {
        filtroUid = uid
    }

    // Ids das recorrências que já viraram lançamento no mês selecionado.
    // De propósito NÃO respeita o filtro por pessoa: a conta de luz
    // está paga, não importa quem pagou.
    val recorrenciasPagasNoMes: Set<String>
        get() = transacoes
            .filter { mesDaTransacao(it) == mesSelecionado }
            .mapNotNull { it.recorrenciaId.ifBlank { null } }
            .toSet()

    fun salvarRecorrencia(recorrencia: Recorrencia) {
        viewModelScope.launch {
            try {
                recorrenciaRepo.salvar(familiaId, recorrencia)
            } catch (e: Exception) {
                erro = "Não foi possível salvar a conta: ${e.message}"
            }
        }
    }

    fun removerRecorrencia(recorrenciaId: String) {
        viewModelScope.launch {
            try {
                recorrenciaRepo.remover(familiaId, recorrenciaId)
            } catch (e: Exception) {
                erro = "Não foi possível excluir a conta: ${e.message}"
            }
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
        get() = transacoes.filter {
            mesDaTransacao(it) == mesSelecionado &&
                (filtroUid == null || it.criadoPor == filtroUid)
        }

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
            val doMes = transacoes.filter {
                mesDaTransacao(it) == mes &&
                    (filtroUid == null || it.criadoPor == filtroUid)
            }
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

    fun adicionar(dados: DadosLancamento) {
        viewModelScope.launch {
            try {
                repo.adicionar(
                    familiaId,
                    Transacao(
                        tipo = dados.tipo,
                        valorCentavos = dados.valorCentavos,
                        categoria = dados.categoria,
                        descricao = dados.descricao.trim(),
                        // meio-dia local: longe das bordas de fuso, o dia
                        // nunca "escorrega" para o mês vizinho
                        data = Timestamp(
                            Date.from(dados.dia.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
                        ),
                        criadoPor = uid,
                        recorrenciaId = dados.recorrenciaId,
                        formaPagamento = dados.formaPagamento,
                        cartaoId = dados.cartaoId
                    )
                )
            } catch (e: Exception) {
                erro = "Não foi possível salvar: ${e.message}"
            }
        }
    }

    // Edição: preserva quem criou e o vínculo com a recorrência,
    // troca só o que a tela permite mudar.
    fun atualizar(original: Transacao, dados: DadosLancamento) {
        viewModelScope.launch {
            try {
                repo.atualizar(
                    familiaId,
                    original.copy(
                        tipo = dados.tipo,
                        valorCentavos = dados.valorCentavos,
                        categoria = dados.categoria,
                        descricao = dados.descricao.trim(),
                        data = Timestamp(
                            Date.from(dados.dia.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
                        ),
                        formaPagamento = dados.formaPagamento,
                        cartaoId = dados.cartaoId
                    )
                )
            } catch (e: Exception) {
                erro = "Não foi possível salvar a alteração: ${e.message}"
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
