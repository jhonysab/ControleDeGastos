package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familia.controledegastos.model.FormaPagamento
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Transacao
import com.familia.controledegastos.model.Usuario
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.ui.TransacoesViewModel
import com.familia.controledegastos.ui.theme.VerdeGanho
import com.familia.controledegastos.ui.theme.VermelhoGasto
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    var formRecorrencia by remember { mutableStateOf<Recorrencia?>(null) }
    var mostrandoAjustes by remember { mutableStateOf(false) }
    var mostrandoPerfil by remember { mutableStateOf(false) }
    var telaCartoes by remember { mutableStateOf(false) }
    var transacaoParaExcluir by remember { mutableStateOf<Transacao?>(null) }
    var abaSelecionada by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val escopo = rememberCoroutineScope()

    // Toda vez que a tela (re)aparece — inclusive num novo login —
    // garante que o ouvinte da nuvem está vivo e o erro antigo, limpo.
    LaunchedEffect(Unit) {
        vm.iniciarEscuta()
    }

    if (lancando) {
        val prefill = recorrenciaParaLancar
        TelaNovaTransacao(
            cartoes = vm.cartoes,
            aoSalvar = { dados ->
                vm.adicionar(dados)
                lancando = false
                recorrenciaParaLancar = null
            },
            aoCancelar = {
                lancando = false
                recorrenciaParaLancar = null
            },
            tipoInicial = prefill?.tipo ?: TipoTransacao.GASTO,
            valorInicialCentavos = prefill?.valorEsperadoCentavos ?: 0L,
            categoriaInicial = prefill?.categoria,
            descricaoInicial = prefill?.descricao ?: "",
            recorrenciaId = prefill?.id ?: "",
            modifier = modifier
        )
        return
    }

    formRecorrencia?.let { recorrencia ->
        TelaNovaRecorrencia(
            recorrencia = recorrencia,
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
            aoSalvar = vm::salvarCartao,
            aoRemover = { vm.removerCartao(it.id) },
            aoVoltar = { telaCartoes = false },
            modifier = modifier
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
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
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    selected = false,
                    onClick = {
                        escopo.launch { drawerState.close() }
                        telaCartoes = true
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

            TabRow(selectedTabIndex = abaSelecionada) {
                Tab(
                    selected = abaSelecionada == 0,
                    onClick = { abaSelecionada = 0 },
                    text = { Text(text = "Resumo", fontSize = 16.sp) }
                )
                Tab(
                    selected = abaSelecionada == 1,
                    onClick = { abaSelecionada = 1 },
                    text = { Text(text = "Gráficos", fontSize = 16.sp) }
                )
                Tab(
                    selected = abaSelecionada == 2,
                    onClick = { abaSelecionada = 2 },
                    text = { Text(text = "Limites", fontSize = 16.sp) }
                )
                Tab(
                    selected = abaSelecionada == 3,
                    onClick = { abaSelecionada = 3 },
                    text = { Text(text = "Contas", fontSize = 16.sp) }
                )
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
                    gastosPorCategoria = vm.gastosPorCategoria.toMap(),
                    orcamentos = vm.orcamentos,
                    aoDefinir = vm::definirOrcamento
                )
            } else if (abaSelecionada == 3) {
                AbaContas(
                    recorrencias = vm.recorrencias,
                    pagasNoMes = vm.recorrenciasPagasNoMes,
                    aoLancar = { recorrencia ->
                        recorrenciaParaLancar = recorrencia
                        lancando = true
                    },
                    aoEditar = { formRecorrencia = it },
                    aoRemover = { vm.removerRecorrencia(it.id) },
                    aoCriarNova = { formRecorrencia = Recorrencia() }
                )
            } else {

            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
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
                    ResumoColuna(
                        rotulo = "Saldo",
                        valorCentavos = vm.saldoCentavos,
                        cor = if (vm.saldoCentavos >= 0) VerdeGanho else VermelhoGasto,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (vm.transacoesDoMes.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nenhum lançamento em ${nomeDoMes(vm.mesSelecionado)}.\n\nToque em Lançar para registrar o primeiro.",
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val nomes = vm.membros.associate { it.id to it.nome }
                val nomesCartoes = vm.cartoes.associate { it.id to it.nome }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(vm.transacoesDoMes, key = { it.id }) { transacao ->
                        ItemTransacao(
                            transacao = transacao,
                            // só faz sentido dizer quem lançou vendo "Todos"
                            criadorNome = if (vm.membros.size > 1 && vm.filtroUid == null) {
                                nomes[transacao.criadoPor]
                            } else null,
                            formaTexto = if (transacao.formaPagamento == FormaPagamento.CREDITO) {
                                listOfNotNull("Crédito", nomesCartoes[transacao.cartaoId])
                                    .joinToString(" ")
                            } else {
                                transacao.formaPagamento.rotulo
                            },
                            aoSegurar = { transacaoParaExcluir = transacao }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
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
                    text = "${transacao.descricao.ifBlank { transacao.categoria.rotulo }} — ${transacao.valorFormatado()}",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemTransacao(
    transacao: Transacao,
    criadorNome: String?,
    formaTexto: String?,
    aoSegurar: () -> Unit
) {
    val ehGasto = transacao.tipo == TipoTransacao.GASTO
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = aoSegurar)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transacao.descricao.ifBlank { transacao.categoria.rotulo },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = listOfNotNull(
                    transacao.categoria.rotulo,
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = cor
        )
    }
}

private fun nomeDoMes(mes: YearMonth): String {
    val ptBr = Locale.forLanguageTag("pt-BR")
    val texto = mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBr))
    return texto.replaceFirstChar { it.titlecase(ptBr) }
}

private fun dataCurta(transacao: Transacao): String =
    SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR")).format(transacao.data.toDate())
