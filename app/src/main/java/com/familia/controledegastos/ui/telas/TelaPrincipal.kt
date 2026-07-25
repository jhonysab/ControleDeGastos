package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familia.controledegastos.R
import com.familia.controledegastos.export.ExportacaoCsv
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.FormaPagamento
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Transacao
import com.familia.controledegastos.model.Usuario
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.ui.OrdenacaoLista
import com.familia.controledegastos.ui.TransacoesViewModel
import com.familia.controledegastos.ui.theme.VerdeGanho
import com.familia.controledegastos.ui.theme.cor
import com.familia.controledegastos.ui.theme.VermelhoGasto
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun TelaPrincipal(
    usuario: Usuario,
    aoSair: () -> Unit,
    aoAtualizarNome: (String) -> Unit,
    modifier: Modifier = Modifier,
    // key: se trocar de usuário ou de família, o ViewModel antigo é descartado
    vm: TransacoesViewModel = viewModel(key = "transacoes-${usuario.id}-${usuario.familiaId}") {
        TransacoesViewModel(familiaId = usuario.familiaId, uid = usuario.id)
    }
) {
    var lancando by remember { mutableStateOf(false) }
    var recorrenciaParaLancar by remember { mutableStateOf<Recorrencia?>(null) }
    var transacaoParaEditar by remember { mutableStateOf<Transacao?>(null) }
    var formRecorrencia by remember { mutableStateOf<Recorrencia?>(null) }
    var mostrandoAjustes by remember { mutableStateOf(false) }
    var mostrandoPerfil by remember { mutableStateOf(false) }
    var telaCartoes by remember { mutableStateOf(false) }
    var telaCategorias by remember { mutableStateOf(false) }
    var transacaoParaExcluir by remember { mutableStateOf<Transacao?>(null) }
    var abaSelecionada by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val escopo = rememberCoroutineScope()
    val contexto = LocalContext.current

    // Toda vez que a tela (re)aparece — inclusive num novo login —
    // garante que o ouvinte da nuvem está vivo e o erro antigo, limpo.
    LaunchedEffect(Unit) {
        vm.iniciarEscuta()
    }

    val editando = transacaoParaEditar
    if (lancando || editando != null) {
        val prefill = recorrenciaParaLancar
        val categoriaIdInicial = editando?.categoria ?: prefill?.categoria
        // Editando: mantém a data do lançamento.
        // Lançando uma conta recorrente: cai no MÊS que está sendo visto,
        // no dia do vencimento — não em "hoje" — para bater com o status
        // "Lançada / Lançar" da aba Contas.
        val dataInicialForm = when {
            editando != null -> editando.data.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            prefill != null -> vm.mesSelecionado.atDay(
                minOf(prefill.diaVencimento, vm.mesSelecionado.lengthOfMonth())
            )
            else -> LocalDate.now()
        }
        TelaNovaTransacao(
            cartoes = vm.cartoes,
            categoriasGasto = vm.categoriasParaGasto,
            categoriasGanho = vm.categoriasParaGanho,
            aoSalvar = { dados ->
                if (editando != null) vm.atualizar(editando, dados) else vm.adicionar(dados)
                lancando = false
                recorrenciaParaLancar = null
                transacaoParaEditar = null
            },
            aoCancelar = {
                lancando = false
                recorrenciaParaLancar = null
                transacaoParaEditar = null
            },
            tipoInicial = editando?.tipo ?: prefill?.tipo ?: TipoTransacao.GASTO,
            valorInicialCentavos = editando?.valorCentavos ?: prefill?.valorEsperadoCentavos ?: 0L,
            categoriaInicial = categoriaIdInicial?.let { vm.resolverCategoria(it) },
            descricaoInicial = editando?.descricao ?: prefill?.descricao ?: "",
            recorrenciaId = prefill?.id ?: "",
            dataInicial = dataInicialForm,
            formaPagamentoInicial = editando?.formaPagamento ?: FormaPagamento.DINHEIRO,
            cartaoIdInicial = editando?.cartaoId ?: "",
            editando = editando != null,
            modifier = modifier
        )
        return
    }

    formRecorrencia?.let { recorrencia ->
        TelaNovaRecorrencia(
            recorrencia = recorrencia,
            categoriasGasto = vm.categoriasParaGasto,
            categoriasGanho = vm.categoriasParaGanho,
            aoSalvar = {
                vm.salvarRecorrencia(it)
                formRecorrencia = null
            },
            aoCancelar = { formRecorrencia = null },
            modifier = modifier
        )
        return
    }

    if (telaCartoes) {
        TelaCartoes(
            cartoes = vm.cartoes,
            faturaDe = vm::faturaAtualCentavos,
            aoSalvar = vm::salvarCartao,
            aoRemover = { vm.removerCartao(it.id) },
            aoVoltar = { telaCartoes = false },
            modifier = modifier
        )
        return
    }

    if (telaCategorias) {
        TelaCategorias(
            categorias = vm.categorias,
            aoSalvar = vm::salvarCategoria,
            aoArquivar = vm::arquivarCategoria,
            aoExcluir = vm::removerCategoria,
            usosDaCategoria = vm::usosDaCategoria,
            aoVoltar = { telaCategorias = false },
            modifier = modifier
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = usuario.nome,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Unicka Finanças",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text(text = "Meu perfil", fontSize = 16.sp) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    selected = false,
                    onClick = {
                        escopo.launch { drawerState.close() }
                        mostrandoPerfil = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(text = "Meus cartões", fontSize = 16.sp) },
                    icon = { Icon(painter = painterResource(R.drawable.ic_cartao), contentDescription = null) },
                    selected = false,
                    onClick = {
                        escopo.launch { drawerState.close() }
                        telaCartoes = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(text = "Categorias", fontSize = 16.sp) },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    selected = false,
                    onClick = {
                        escopo.launch { drawerState.close() }
                        telaCategorias = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(text = "Família e convite", fontSize = 16.sp) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    selected = false,
                    onClick = {
                        escopo.launch { drawerState.close() }
                        mostrandoAjustes = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(text = "Exportar / Backup", fontSize = 16.sp) },
                    icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    selected = false,
                    onClick = {
                        escopo.launch { drawerState.close() }
                        if (vm.transacoes.isEmpty()) {
                            vm.avisarNadaParaExportar()
                        } else {
                            ExportacaoCsv.compartilhar(
                                contexto,
                                vm.gerarCsvExportacao(),
                                "unicka-financas-${LocalDate.now()}.csv"
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(text = "Sair da conta", fontSize = 16.sp, color = VermelhoGasto) },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = VermelhoGasto
                        )
                    },
                    selected = false,
                    onClick = {
                        vm.pararEscuta()
                        aoSair()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = vm::mesAnterior) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mês anterior",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = nomeDoMes(vm.mesSelecionado),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::proximoMes) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Próximo mês",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { escopo.launch { drawerState.open() } }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // A aba encolhe a fonte até caber em vez de quebrar a palavra:
            // em celular com fonte grande, "Gráficos" pulava as últimas
            // letras para a linha de baixo.
            TabRow(selectedTabIndex = abaSelecionada) {
                ABAS.forEachIndexed { indice, titulo ->
                    Tab(
                        selected = abaSelecionada == indice,
                        onClick = { abaSelecionada = indice },
                        text = {
                            BasicText(
                                text = titulo,
                                style = LocalTextStyle.current.copy(color = LocalContentColor.current),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 11.sp,
                                    maxFontSize = 16.sp
                                )
                            )
                        }
                    )
                }
            }

            // Filtro por pessoa (some na aba Contas: o status lá é da família)
            if (vm.membros.size > 1 && abaSelecionada != 3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    FilterChip(
                        selected = vm.filtroUid == null,
                        onClick = { vm.definirFiltro(null) },
                        label = { Text(text = "Todos", fontSize = 15.sp) }
                    )
                    vm.membros.forEach { membro ->
                        FilterChip(
                            selected = vm.filtroUid == membro.id,
                            onClick = { vm.definirFiltro(membro.id) },
                            label = { Text(text = membro.nome, fontSize = 15.sp) }
                        )
                    }
                }
            }

            vm.erro?.let { mensagem ->
                Text(
                    text = mensagem,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (abaSelecionada == 1) {
                AbaGraficos(
                    gastosPorCategoria = vm.gastosPorCategoria,
                    resumoMeses = vm.resumoUltimosMeses()
                )
            } else if (abaSelecionada == 2) {
                AbaOrcamento(
                    categorias = vm.categoriasParaGasto,
                    gastoPorCategoriaId = vm.gastosPorCategoria.associate { it.first.id to it.second },
                    orcamentos = vm.orcamentos,
                    aoDefinir = vm::definirOrcamento,
                    aoArquivar = vm::ocultarCategoriaNosLimites
                )
            } else if (abaSelecionada == 3) {
                AbaContas(
                    recorrencias = vm.recorrencias,
                    pagasNoMes = vm.recorrenciasPagasNoMes,
                    corDaCategoria = { id -> vm.resolverCategoria(id).cor() },
                    aoLancar = { recorrencia ->
                        recorrenciaParaLancar = recorrencia
                        lancando = true
                    },
                    aoEditar = { formRecorrencia = it },
                    aoRemover = { vm.removerRecorrencia(it.id) },
                    aoCriarNova = { formRecorrencia = Recorrencia() }
                )
            } else {

            // Aba Resumo: o saldo, a busca e os filtros ficam DENTRO da lista,
            // então rolam junto — ao descer saem de vista e liberam a tela,
            // ao voltar ao topo reaparecem.
            val exibidas = vm.transacoesExibidas
            val nomes = vm.membros.associate { it.id to it.nome }
            val nomesCartoes = vm.cartoes.associate { it.id to it.nome }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // respiro no fim para o botão "Lançar" não tampar o último item
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    CartaoResumo(vm)
                }

                if (vm.transacoesDoMes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum lançamento em ${nomeDoMes(vm.mesSelecionado)}.\n\nToque em Lançar para registrar o primeiro.",
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item { ControlesLista(vm) }

                    if (exibidas.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum lançamento com esses filtros.",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (vm.separarPorDia) {
                        // Um cabeçalho por dia, com o que entrou/saiu
                        // naquele dia — fica bem mais fácil de ler do
                        // que uma lista corrida do mês inteiro.
                        vm.exibidasPorDia.forEach { grupo ->
                            item(key = "dia-${grupo.dia}") {
                                CabecalhoDoDia(
                                    dia = grupo.dia,
                                    saldoCentavos = grupo.saldoCentavos
                                )
                            }
                            items(grupo.itens, key = { it.id }) { transacao ->
                                LinhaDaLista(
                                    vm = vm,
                                    transacao = transacao,
                                    nomes = nomes,
                                    nomesCartoes = nomesCartoes,
                                    aoTocar = { transacaoParaEditar = transacao },
                                    aoSegurar = { transacaoParaExcluir = transacao }
                                )
                            }
                        }
                    } else {
                        // Ordenado por valor: agrupar por dia embaralharia tudo.
                        items(exibidas, key = { it.id }) { transacao ->
                            LinhaDaLista(
                                vm = vm,
                                transacao = transacao,
                                nomes = nomes,
                                nomesCartoes = nomesCartoes,
                                aoTocar = { transacaoParaEditar = transacao },
                                aoSegurar = { transacaoParaExcluir = transacao }
                            )
                        }
                    }
                }
            }
            } // fim da aba Resumo
        }

        ExtendedFloatingActionButton(
            onClick = { lancando = true },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(text = "Lançar", fontSize = 18.sp) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
    }

    if (mostrandoPerfil) {
        var nome by remember { mutableStateOf(usuario.nome) }
        AlertDialog(
            onDismissRequest = { mostrandoPerfil = false },
            title = { Text("Meu perfil") },
            text = {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Seu nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        aoAtualizarNome(nome)
                        mostrandoPerfil = false
                    },
                    enabled = nome.isNotBlank()
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoPerfil = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrandoAjustes) {
        AlertDialog(
            onDismissRequest = { mostrandoAjustes = false },
            title = { Text("Família e convite") },
            text = {
                Column {
                    Text(
                        text = "Código para convidar outra pessoa (toque e segure para copiar):",
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = usuario.familiaId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrandoAjustes = false }) { Text("Fechar") }
            }
        )
    }

    transacaoParaExcluir?.let { transacao ->
        AlertDialog(
            onDismissRequest = { transacaoParaExcluir = null },
            title = { Text("Excluir lançamento?") },
            text = {
                Text(
                    text = "${transacao.descricao.ifBlank { vm.resolverCategoria(transacao.categoria).rotulo }} — ${transacao.valorFormatado()}",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.remover(transacao.id)
                    transacaoParaExcluir = null
                }) { Text("Excluir", color = VermelhoGasto) }
            },
            dismissButton = {
                TextButton(onClick = { transacaoParaExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

// Busca + linha de ordenação e filtros da lista do Resumo.
// A linha de chips rola na horizontal para caber em telas estreitas.
@Composable
private fun ControlesLista(vm: TransacoesViewModel) {
    Column {
        OutlinedTextField(
            value = vm.busca,
            onValueChange = { vm.definirBusca(it) },
            label = { Text("Buscar por descrição ou categoria") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (vm.busca.isNotEmpty()) {
                    IconButton(onClick = { vm.definirBusca("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Ordenar
            MenuSuspenso(
                texto = "Ordenar: ${vm.ordenacaoLista.rotulo}",
                opcoes = OrdenacaoLista.entries.map { it.rotulo to { vm.definirOrdenacao(it) } }
            )
            // Filtrar por categoria (só as que aparecem no mês)
            val nomeFiltroCategoria = vm.filtroCategoriaId
                ?.let { vm.resolverCategoria(it).rotulo } ?: "Todas"
            MenuSuspenso(
                texto = "Categoria: $nomeFiltroCategoria",
                selecionadoDestaque = vm.filtroCategoriaId != null,
                opcoes = buildList {
                    add("Todas" to { vm.definirFiltroCategoria(null) })
                    vm.categoriasDoMes.forEach { cat ->
                        add(cat.rotulo to { vm.definirFiltroCategoria(cat.id) })
                    }
                }
            )
            // Filtrar por tipo
            val rotuloTipo = when (vm.filtroTipoLista) {
                TipoTransacao.GASTO -> "Só gastos"
                TipoTransacao.GANHO -> "Só ganhos"
                null -> "Tudo"
            }
            MenuSuspenso(
                texto = rotuloTipo,
                selecionadoDestaque = vm.filtroTipoLista != null,
                opcoes = listOf(
                    "Tudo" to { vm.definirFiltroTipo(null) },
                    "Só gastos" to { vm.definirFiltroTipo(TipoTransacao.GASTO) },
                    "Só ganhos" to { vm.definirFiltroTipo(TipoTransacao.GANHO) }
                )
            )
            // Filtrar por forma de pagamento / cartão específico
            val rotuloForma = when {
                vm.filtroCartaoId != null ->
                    vm.cartoes.firstOrNull { it.id == vm.filtroCartaoId }?.nome ?: "Cartão"
                vm.filtroForma != null -> vm.filtroForma!!.rotulo
                else -> "Forma: todas"
            }
            MenuSuspenso(
                texto = rotuloForma,
                selecionadoDestaque = vm.filtroForma != null || vm.filtroCartaoId != null,
                opcoes = buildList {
                    add("Todas as formas" to { vm.definirFiltroForma(null) })
                    FormaPagamento.entries.forEach { forma ->
                        add(forma.rotulo to { vm.definirFiltroForma(forma) })
                    }
                    vm.cartoes.forEach { cartao ->
                        add("Cartão: ${cartao.nome}" to {
                            vm.definirFiltroForma(FormaPagamento.CREDITO, cartao.id)
                        })
                    }
                }
            )
        }
    }
}

@Composable
private fun MenuSuspenso(
    texto: String,
    opcoes: List<Pair<String, () -> Unit>>,
    selecionadoDestaque: Boolean = false
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selecionadoDestaque,
            onClick = { aberto = true },
            label = { Text(text = texto, fontSize = 14.sp) }
        )
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (rotulo, acao) ->
                DropdownMenuItem(
                    text = { Text(rotulo) },
                    onClick = {
                        acao()
                        aberto = false
                    }
                )
            }
        }
    }
}

// Uma linha da lista do Resumo (com o traço separador embaixo).
// Existe para os dois modos — agrupado por dia e lista corrida —
// usarem exatamente o mesmo item.
@Composable
private fun LinhaDaLista(
    vm: TransacoesViewModel,
    transacao: Transacao,
    nomes: Map<String, String>,
    nomesCartoes: Map<String, String>,
    aoTocar: () -> Unit,
    aoSegurar: () -> Unit
) {
    ItemTransacao(
        transacao = transacao,
        categoria = vm.resolverCategoria(transacao.categoria),
        // só faz sentido dizer quem lançou vendo "Todos"
        criadorNome = if (vm.membros.size > 1 && vm.filtroUid == null) {
            nomes[transacao.criadoPor]
        } else null,
        formaTexto = if (transacao.formaPagamento == FormaPagamento.CREDITO) {
            listOfNotNull("Crédito", nomesCartoes[transacao.cartaoId]).joinToString(" ")
        } else {
            transacao.formaPagamento.rotulo
        },
        aoTocar = aoTocar,
        aoSegurar = aoSegurar
    )
    HorizontalDivider(thickness = 0.5.dp)
}

// Faixa que separa um dia do outro na lista.
@Composable
private fun CabecalhoDoDia(dia: LocalDate, saldoCentavos: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = rotuloDoDia(dia),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = (if (saldoCentavos < 0) "− " else "+ ") + formatarCentavos(abs(saldoCentavos)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = if (saldoCentavos < 0) VermelhoGasto else VerdeGanho
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemTransacao(
    transacao: Transacao,
    categoria: Categoria,
    criadorNome: String?,
    formaTexto: String?,
    aoTocar: () -> Unit,
    aoSegurar: () -> Unit
) {
    val ehGasto = transacao.tipo == TipoTransacao.GASTO
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = aoTocar, onLongClick = aoSegurar)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transacao.descricao.ifBlank { categoria.rotulo },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = listOfNotNull(
                    categoria.rotulo,
                    dataCurta(transacao),
                    criadorNome,
                    formaTexto
                ).joinToString(" • "),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = (if (ehGasto) "− " else "+ ") + transacao.valorFormatado(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (ehGasto) VermelhoGasto else VerdeGanho
        )
    }
}

// Card do topo da aba Resumo: Saldo em destaque + Entradas/Saídas.
@Composable
private fun CartaoResumo(vm: TransacoesViewModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Saldo em destaque: é o número que os pais mais olham.
            // Com filtro ligado o card fala do filtro, não do mês —
            // e o rótulo avisa isso para ninguém se perder.
            Text(
                text = if (vm.filtrosAtivos) "Saldo do que está filtrado" else "Saldo do mês",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatarCentavos(vm.saldoCentavos),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = if (vm.saldoCentavos >= 0) VerdeGanho else VermelhoGasto
            )
            if (vm.filtrosAtivos) {
                Text(
                    text = "${vm.transacoesExibidas.size} de ${vm.transacoesDoMes.size} lançamentos do mês",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ResumoColuna(
                    rotulo = "Entradas",
                    valorCentavos = vm.totalGanhosCentavos,
                    cor = VerdeGanho,
                    modifier = Modifier.weight(1f)
                )
                ResumoColuna(
                    rotulo = "Saídas",
                    valorCentavos = vm.totalGastosCentavos,
                    cor = VermelhoGasto,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ResumoColuna(
    rotulo: String,
    valorCentavos: Long,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = rotulo, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = formatarCentavos(valorCentavos),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = cor
        )
    }
}

private val ABAS = listOf("Resumo", "Gráficos", "Limites", "Contas")

private fun nomeDoMes(mes: YearMonth): String {
    val ptBr = Locale.forLanguageTag("pt-BR")
    val texto = mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBr))
    return texto.replaceFirstChar { it.titlecase(ptBr) }
}

// "Hoje, 24/07" / "Ontem, 23/07" / "Segunda-feira, 21/07"
private fun rotuloDoDia(dia: LocalDate): String {
    val ptBr = Locale.forLanguageTag("pt-BR")
    val curta = dia.format(DateTimeFormatter.ofPattern("dd/MM", ptBr))
    val hoje = LocalDate.now()
    return when (dia) {
        hoje -> "Hoje, $curta"
        hoje.minusDays(1) -> "Ontem, $curta"
        else -> {
            val semana = dia.format(DateTimeFormatter.ofPattern("EEEE", ptBr))
                .replaceFirstChar { it.titlecase(ptBr) }
            "$semana, $curta"
        }
    }
}

private fun dataCurta(transacao: Transacao): String =
    SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR")).format(transacao.data.toDate())
