package com.familia.controledegastos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familia.controledegastos.data.AuthRepository
import com.familia.controledegastos.data.CategoriaRepository
import com.familia.controledegastos.data.OrcamentoRepository
import com.familia.controledegastos.data.RecorrenciaRepository
import com.familia.controledegastos.data.TransacaoRepository
import com.familia.controledegastos.data.CartaoRepository
import com.familia.controledegastos.model.Cartao
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.CategoriasPadrao
import com.familia.controledegastos.model.FormaPagamento
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.Usuario
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Transacao
import com.familia.controledegastos.model.primeiraMaiuscula
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ResumoMensal(
    val mes: YearMonth,
    val ganhosCentavos: Long,
    val gastosCentavos: Long
)

// Um dia da lista do Resumo: o cabeçalho com a data e o que
// aconteceu nele.
data class DiaDeLancamentos(
    val dia: LocalDate,
    val itens: List<Transacao>,
    // ganhos menos gastos DAQUELE dia (negativo = dia de gasto)
    val saldoCentavos: Long
)

// Quebra a lista do Resumo em dias. groupBy preserva a ordem em que os
// itens aparecem, então os dias saem na mesma ordem da lista que entrou
// (mais recentes primeiro, ou mais antigos).
// Fora da classe e sem Firebase: dá para testar direto.
fun agruparPorDia(
    transacoes: List<Transacao>,
    zona: ZoneId = ZoneId.systemDefault()
): List<DiaDeLancamentos> =
    transacoes
        .groupBy { it.data.toDate().toInstant().atZone(zona).toLocalDate() }
        .map { (dia, itens) ->
            DiaDeLancamentos(
                dia = dia,
                itens = itens,
                saldoCentavos = itens.sumOf {
                    if (it.tipo == TipoTransacao.GANHO) it.valorCentavos else -it.valorCentavos
                }
            )
        }

enum class OrdenacaoLista(val rotulo: String) {
    RECENTE("Mais recentes"),
    ANTIGO("Mais antigos"),
    MAIOR_VALOR("Maior valor"),
    MENOR_VALOR("Menor valor")
}

// Tudo que a tela de lançamento coleta, num pacote só.
data class DadosLancamento(
    val tipo: TipoTransacao,
    val valorCentavos: Long,
    val categoria: Categoria,
    val descricao: String,
    val dia: LocalDate,
    val recorrenciaId: String = "",
    val formaPagamento: FormaPagamento = FormaPagamento.DINHEIRO,
    val cartaoId: String = "",
    // > 1 divide a compra em N lançamentos mensais (só no crédito)
    val parcelas: Int = 1
)

class TransacoesViewModel(
    private val familiaId: String,
    private val uid: String,
    private val repo: TransacaoRepository = TransacaoRepository(),
    private val orcamentoRepo: OrcamentoRepository = OrcamentoRepository(),
    private val recorrenciaRepo: RecorrenciaRepository = RecorrenciaRepository(),
    private val cartaoRepo: CartaoRepository = CartaoRepository(),
    private val categoriaRepo: CategoriaRepository = CategoriaRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    var transacoes by mutableStateOf<List<Transacao>>(emptyList())
        private set
    var orcamentos by mutableStateOf<Map<String, Long>>(emptyMap())
        private set
    var categorias by mutableStateOf<List<Categoria>>(emptyList())
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
    // Filtros/ordenação da LISTA do Resumo (não mexem nos totais do mês)
    var ordenacaoLista by mutableStateOf(OrdenacaoLista.RECENTE)
        private set
    var filtroCategoriaId by mutableStateOf<String?>(null)
        private set
    var filtroTipoLista by mutableStateOf<TipoTransacao?>(null)
        private set
    var filtroForma by mutableStateOf<FormaPagamento?>(null)
        private set
    // quando não-nulo, restringe a um cartão específico (implica crédito)
    var filtroCartaoId by mutableStateOf<String?>(null)
        private set
    var busca by mutableStateOf("")
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
        escutas += viewModelScope.launch {
            categoriaRepo.observar(familiaId)
                .catch { erro = "Não foi possível carregar as categorias: ${it.message}" }
                .collect { categorias = it }
        }
        escutas += viewModelScope.launch {
            authRepo.observarMembros(familiaId)
                // sem perfis extras a tela ainda funciona; não vale travar por isso
                .catch { }
                .collect { membros = it }
        }
        viewModelScope.launch {
            // primeira vez da família: cria as categorias iniciais
            runCatching { categoriaRepo.semearSePreciso(familiaId) }
        }
    }

    // Resolve o ID guardado no lançamento para a categoria completa.
    private val categoriaPorId: Map<String, Categoria>
        get() = categorias.associateBy { it.id }

    fun resolverCategoria(id: String): Categoria =
        categoriaPorId[id] ?: CategoriasPadrao.removida.copy(nome = "Sem categoria")

    // Ativas (não arquivadas) para cada tipo de lançamento
    val categoriasParaGasto: List<Categoria>
        get() = categorias.filter { !it.arquivada && it.serveParaGasto() }

    val categoriasParaGanho: List<Categoria>
        get() = categorias.filter { !it.arquivada && it.serveParaGanho() }

    fun salvarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            try {
                categoriaRepo.salvar(familiaId, categoria)
            } catch (e: Exception) {
                erro = "Não foi possível salvar a categoria: ${e.message}"
            }
        }
    }

    fun arquivarCategoria(categoriaId: String, arquivada: Boolean) {
        viewModelScope.launch {
            try {
                categoriaRepo.arquivar(familiaId, categoriaId, arquivada)
            } catch (e: Exception) {
                erro = "Não foi possível arquivar a categoria: ${e.message}"
            }
        }
    }

    // Tira (ou devolve) a categoria da aba Limites, sem mexer nos
    // lançamentos — nem toda categoria precisa de um teto de gasto.
    fun ocultarCategoriaNosLimites(categoriaId: String, oculta: Boolean) {
        viewModelScope.launch {
            try {
                categoriaRepo.ocultarNosLimites(familiaId, categoriaId, oculta)
            } catch (e: Exception) {
                erro = "Não foi possível arquivar a categoria: ${e.message}"
            }
        }
    }

    // Quantos lançamentos e contas ainda apontam para essa categoria.
    // Zero = dá para apagar de vez sem deixar histórico órfão.
    fun usosDaCategoria(categoriaId: String): Int =
        transacoes.count { it.categoria == categoriaId } +
            recorrencias.count { it.categoria == categoriaId }

    fun removerCategoria(categoriaId: String) {
        viewModelScope.launch {
            try {
                categoriaRepo.remover(familiaId, categoriaId)
                // limite de uma categoria que não existe mais não serve para nada
                runCatching { orcamentoRepo.remover(familiaId, categoriaId) }
            } catch (e: Exception) {
                erro = "Não foi possível excluir a categoria: ${e.message}"
            }
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

    fun definirOrdenacao(ordem: OrdenacaoLista) {
        ordenacaoLista = ordem
    }

    fun definirFiltroCategoria(categoriaId: String?) {
        filtroCategoriaId = categoriaId
    }

    fun definirFiltroTipo(tipo: TipoTransacao?) {
        filtroTipoLista = tipo
    }

    // forma nula + cartão nulo = todas as formas
    fun definirFiltroForma(forma: FormaPagamento?, cartaoId: String? = null) {
        filtroForma = forma
        filtroCartaoId = cartaoId
    }

    fun definirBusca(texto: String) {
        busca = texto
    }

    // Lista já filtrada (categoria/tipo/forma/busca) e ordenada, para a aba Resumo.
    val transacoesExibidas: List<Transacao>
        get() = transacoesDoMes
            .filter { filtroCategoriaId == null || it.categoria == filtroCategoriaId }
            .filter { filtroTipoLista == null || it.tipo == filtroTipoLista }
            .filter { filtroForma == null || it.formaPagamento == filtroForma }
            .filter { filtroCartaoId == null || it.cartaoId == filtroCartaoId }
            .filter {
                busca.isBlank() ||
                    it.descricao.contains(busca, ignoreCase = true) ||
                    resolverCategoria(it.categoria).nome.contains(busca, ignoreCase = true)
            }
            .let { lista ->
                when (ordenacaoLista) {
                    OrdenacaoLista.RECENTE -> lista.sortedByDescending { it.data }
                    OrdenacaoLista.ANTIGO -> lista.sortedBy { it.data }
                    OrdenacaoLista.MAIOR_VALOR -> lista.sortedByDescending { it.valorCentavos }
                    OrdenacaoLista.MENOR_VALOR -> lista.sortedBy { it.valorCentavos }
                }
            }

    // Algum filtro da lista está ligado? (o filtro por pessoa não conta:
    // ele já muda o mês inteiro, inclusive Gráficos e Limites)
    val filtrosAtivos: Boolean
        get() = filtroCategoriaId != null || filtroTipoLista != null ||
            filtroForma != null || filtroCartaoId != null || busca.isNotBlank()

    // Separar por dia só faz sentido com a lista em ordem de data —
    // ordenando por valor os dias ficariam embaralhados.
    val separarPorDia: Boolean
        get() = ordenacaoLista == OrdenacaoLista.RECENTE || ordenacaoLista == OrdenacaoLista.ANTIGO

    val exibidasPorDia: List<DiaDeLancamentos>
        get() = agruparPorDia(transacoesExibidas)

    // Categorias que realmente aparecem no mês (para o filtro não listar vazio)
    val categoriasDoMes: List<Categoria>
        get() = transacoesDoMes.map { it.categoria }.distinct()
            .map { resolverCategoria(it) }.sortedBy { it.rotulo }

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
    fun definirOrcamento(categoriaId: String, limiteCentavos: Long) {
        viewModelScope.launch {
            try {
                if (limiteCentavos > 0) {
                    orcamentoRepo.definir(familiaId, categoriaId, limiteCentavos)
                } else {
                    orcamentoRepo.remover(familiaId, categoriaId)
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

    // Os totais do card seguem os MESMOS filtros da lista logo abaixo
    // dele. Sem isso a pessoa filtrava "só gastos no crédito" e o card
    // continuava mostrando o mês inteiro — parecia que a conta não fechava.
    val totalGanhosCentavos: Long
        get() = transacoesExibidas.filter { it.tipo == TipoTransacao.GANHO }.sumOf { it.valorCentavos }

    val totalGastosCentavos: Long
        get() = transacoesExibidas.filter { it.tipo == TipoTransacao.GASTO }.sumOf { it.valorCentavos }

    val saldoCentavos: Long
        get() = totalGanhosCentavos - totalGastosCentavos

    // Gastos do mês agrupados por categoria, do maior para o menor (donut).
    val gastosPorCategoria: List<Pair<Categoria, Long>>
        get() = transacoesDoMes
            .filter { it.tipo == TipoTransacao.GASTO }
            .groupBy { it.categoria }
            .map { (categoriaId, itens) -> resolverCategoria(categoriaId) to itens.sumOf { it.valorCentavos } }
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
                val n = if (dados.formaPagamento == FormaPagamento.CREDITO) {
                    dados.parcelas.coerceIn(1, 24)
                } else 1

                if (n == 1) {
                    repo.adicionar(
                        familiaId,
                        montarTransacao(
                            dados,
                            dados.valorCentavos,
                            dados.descricao.primeiraMaiuscula(),
                            dados.dia
                        )
                    )
                } else {
                    // o valor digitado é o TOTAL da compra; divide em N
                    // parcelas e a primeira absorve a sobra dos centavos
                    val parcela = dados.valorCentavos / n
                    val primeira = dados.valorCentavos - parcela * (n - 1)
                    val descBase = dados.descricao.primeiraMaiuscula()
                        .ifBlank { dados.categoria.rotulo }
                    repeat(n) { i ->
                        repo.adicionar(
                            familiaId,
                            montarTransacao(
                                dados,
                                if (i == 0) primeira else parcela,
                                "$descBase ${i + 1}/$n",
                                dados.dia.plusMonths(i.toLong())
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                erro = "Não foi possível salvar: ${e.message}"
            }
        }
    }

    private fun montarTransacao(
        dados: DadosLancamento,
        valorCentavos: Long,
        descricao: String,
        dia: LocalDate
    ) = Transacao(
        tipo = dados.tipo,
        valorCentavos = valorCentavos,
        categoria = dados.categoria.id,
        descricao = descricao,
        // meio-dia local: longe das bordas de fuso, o dia
        // nunca "escorrega" para o mês vizinho
        data = Timestamp(
            Date.from(dia.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
        ),
        criadoPor = uid,
        recorrenciaId = dados.recorrenciaId,
        formaPagamento = dados.formaPagamento,
        cartaoId = dados.cartaoId
    )

    // Fatura "aberta" do cartão: compras no crédito entre o fechamento
    // anterior e o próximo fechamento.
    fun faturaAtualCentavos(cartao: Cartao): Long {
        val hoje = LocalDate.now()
        fun fechamento(mes: YearMonth): LocalDate =
            mes.atDay(minOf(cartao.diaFechamento, mes.lengthOfMonth()))

        val fechamentoDesteMes = fechamento(YearMonth.from(hoje))
        val (inicio, fim) = if (hoje.isAfter(fechamentoDesteMes)) {
            fechamentoDesteMes.plusDays(1) to fechamento(YearMonth.from(hoje).plusMonths(1))
        } else {
            fechamento(YearMonth.from(hoje).minusMonths(1)).plusDays(1) to fechamentoDesteMes
        }

        return transacoes.filter {
            it.formaPagamento == FormaPagamento.CREDITO &&
                it.cartaoId == cartao.id &&
                it.tipo == TipoTransacao.GASTO &&
                !diaDaTransacao(it).isBefore(inicio) &&
                !diaDaTransacao(it).isAfter(fim)
        }.sumOf { it.valorCentavos }
    }

    private fun diaDaTransacao(t: Transacao): LocalDate =
        t.data.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

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
                        categoria = dados.categoria.id,
                        descricao = dados.descricao.primeiraMaiuscula(),
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

    // Backup em CSV de TODOS os lançamentos da família (todos os meses),
    // em ordem cronológica. Delimitador ';' e decimais com vírgula: é o
    // que o Excel em português abre direto. Gastos saem negativos, então
    // uma soma da coluna Valor já dá o saldo.
    fun gerarCsvExportacao(): String {
        val nomes = membros.associate { it.id to it.nome }
        val nomesCartoes = cartoes.associate { it.id to it.nome }
        val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val cabecalho = listOf(
            "Data", "Tipo", "Categoria", "Descrição", "Valor", "Forma", "Cartão", "Lançado por"
        )
        val linhas = transacoes
            .sortedBy { it.data }
            .map { t ->
                val ehGasto = t.tipo == TipoTransacao.GASTO
                val valorAssinado = if (ehGasto) -t.valorCentavos else t.valorCentavos
                listOf(
                    diaDaTransacao(t).format(formatoData),
                    if (ehGasto) "Gasto" else "Ganho",
                    resolverCategoria(t.categoria).nome,
                    t.descricao,
                    centavosParaTextoCsv(valorAssinado),
                    t.formaPagamento.rotulo,
                    if (t.formaPagamento == FormaPagamento.CREDITO) nomesCartoes[t.cartaoId].orEmpty() else "",
                    nomes[t.criadoPor].orEmpty()
                )
            }

        return (listOf(cabecalho) + linhas).joinToString("\r\n") { linha ->
            linha.joinToString(";") { campoCsv(it) }
        }
    }

    // Envolve em aspas (e dobra as internas) só quando o campo tem
    // caractere que quebraria o CSV.
    private fun campoCsv(valor: String): String =
        if (valor.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else {
            valor
        }

    // Centavos -> "1234,56" (vírgula decimal, sem R$ nem separador de
    // milhar, para o Excel reconhecer como número).
    private fun centavosParaTextoCsv(centavos: Long): String {
        val negativo = centavos < 0
        val abs = if (negativo) -centavos else centavos
        return (if (negativo) "-" else "") +
            "${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
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

    fun avisarNadaParaExportar() {
        erro = "Nenhum lançamento para exportar ainda."
    }

    fun limparErro() {
        erro = null
    }

    private fun mesDaTransacao(t: Transacao): YearMonth =
        YearMonth.from(t.data.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
}
