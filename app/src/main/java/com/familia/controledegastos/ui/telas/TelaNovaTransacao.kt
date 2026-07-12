package com.familia.controledegastos.ui.telas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Cartao
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.FormaPagamento
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.ui.DadosLancamento
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaNovaTransacao(
    cartoes: List<Cartao>,
    aoSalvar: (DadosLancamento) -> Unit,
    aoCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    // Pré-preenchimento quando o lançamento nasce de uma conta recorrente
    tipoInicial: TipoTransacao = TipoTransacao.GASTO,
    valorInicialCentavos: Long = 0L,
    categoriaInicial: Categoria? = null,
    descricaoInicial: String = "",
    recorrenciaId: String = ""
) {
    var tipo by remember { mutableStateOf(tipoInicial) }
    var valorCentavos by remember { mutableLongStateOf(valorInicialCentavos) }
    var categoria by remember { mutableStateOf(categoriaInicial) }
    var descricao by remember { mutableStateOf(descricaoInicial) }
    var dataSelecionada by remember { mutableStateOf(LocalDate.now()) }
    var mostrandoCalendario by remember { mutableStateOf(false) }
    var formaPagamento by remember { mutableStateOf(FormaPagamento.DINHEIRO) }
    var cartaoId by remember { mutableStateOf("") }

    BackHandler(onBack = aoCancelar)

    val categoriasVisiveis = if (tipo == TipoTransacao.GANHO) {
        listOf(Categoria.SALARIO, Categoria.OUTROS)
    } else {
        Categoria.entries.filter { it != Categoria.SALARIO }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = "Novo lançamento", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = tipo == TipoTransacao.GASTO,
                onClick = {
                    tipo = TipoTransacao.GASTO
                    if (categoria == Categoria.SALARIO) categoria = null
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(text = "Gasto", fontSize = 17.sp)
            }
            SegmentedButton(
                selected = tipo == TipoTransacao.GANHO,
                onClick = {
                    tipo = TipoTransacao.GANHO
                    if (categoria != null && categoria != Categoria.SALARIO && categoria != Categoria.OUTROS) {
                        categoria = null
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(text = "Ganho", fontSize = 17.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        CampoValorMonetario(
            valorInicialCentavos = valorInicialCentavos,
            aoMudar = { valorCentavos = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Categoria", fontSize = 16.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categoriasVisiveis.forEach { c ->
                FilterChip(
                    selected = categoria == c,
                    onClick = { categoria = c },
                    label = { Text(text = c.rotulo, fontSize = 15.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Data do lançamento: hoje por padrão, mas dá pra registrar
        // aquela conta paga semana passada.
        OutlinedButton(
            onClick = { mostrandoCalendario = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (dataSelecionada == LocalDate.now()) {
                    "Hoje, ${dataSelecionada.format(FORMATO_DATA)}"
                } else {
                    dataSelecionada.format(FORMATO_DATA)
                },
                fontSize = 17.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (tipo == TipoTransacao.GASTO) "Forma de pagamento" else "Como recebeu?",
            fontSize = 16.sp
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormaPagamento.entries.forEach { forma ->
                FilterChip(
                    selected = formaPagamento == forma,
                    onClick = {
                        formaPagamento = forma
                        if (forma != FormaPagamento.CREDITO) cartaoId = ""
                    },
                    label = { Text(text = forma.rotulo, fontSize = 15.sp) }
                )
            }
        }

        if (formaPagamento == FormaPagamento.CREDITO) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Qual cartão?", fontSize = 16.sp)
            if (cartoes.isEmpty()) {
                Text(
                    text = "Nenhum cartão cadastrado ainda — cadastre no menu ☰ → Meus cartões.",
                    fontSize = 14.sp
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cartoes.forEach { cartao ->
                        FilterChip(
                            selected = cartaoId == cartao.id,
                            onClick = { cartaoId = cartao.id },
                            label = { Text(text = cartao.nome, fontSize = 15.sp) }
                        )
                    }
                }
                cartoes.firstOrNull { it.id == cartaoId }?.let {
                    Text(text = "Fatura vence dia ${it.diaVencimento}", fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = {
                Text(
                    if (tipo == TipoTransacao.GASTO) {
                        "Descrição (opcional, ex: conta de luz)"
                    } else {
                        "Descrição (opcional, ex: salário, venda)"
                    }
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                aoSalvar(
                    DadosLancamento(
                        tipo = tipo,
                        valorCentavos = valorCentavos,
                        categoria = categoria!!,
                        descricao = descricao,
                        dia = dataSelecionada,
                        recorrenciaId = recorrenciaId,
                        formaPagamento = formaPagamento,
                        cartaoId = cartaoId
                    )
                )
            },
            enabled = valorCentavos > 0 && categoria != null &&
                (formaPagamento != FormaPagamento.CREDITO || cartaoId.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Salvar", fontSize = 18.sp)
        }
        TextButton(
            onClick = aoCancelar,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Cancelar", fontSize = 16.sp)
        }
    }

    if (mostrandoCalendario) {
        // O DatePicker trabalha em milissegundos UTC; a conversão
        // de ida e volta precisa usar o MESMO fuso (UTC) para o dia
        // não escorregar.
        val estadoCalendario = rememberDatePickerState(
            initialSelectedDateMillis = dataSelecionada
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrandoCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoCalendario.selectedDateMillis?.let { millis ->
                        dataSelecionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrandoCalendario = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoCalendario)
        }
    }
}

private val FORMATO_DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
