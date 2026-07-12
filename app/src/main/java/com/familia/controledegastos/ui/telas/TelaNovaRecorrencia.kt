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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.TipoTransacao

// Criar ou editar uma conta recorrente. `recorrencia` com id em
// branco significa conta nova.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaNovaRecorrencia(
    recorrencia: Recorrencia,
    aoSalvar: (Recorrencia) -> Unit,
    aoCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tipo by remember { mutableStateOf(recorrencia.tipo) }
    var descricao by remember { mutableStateOf(recorrencia.descricao) }
    var valorCentavos by remember { mutableLongStateOf(recorrencia.valorEsperadoCentavos) }
    var categoria by remember {
        mutableStateOf(if (recorrencia.id.isBlank()) null else recorrencia.categoria)
    }
    var diaTexto by remember {
        mutableStateOf(if (recorrencia.id.isBlank()) "" else recorrencia.diaVencimento.toString())
    }

    BackHandler(onBack = aoCancelar)

    val dia = diaTexto.toIntOrNull()
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
        Text(
            text = if (recorrencia.id.isBlank()) "Nova conta do mês" else "Editar conta",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ela vai aparecer todo mês na aba Contas, esperando ser lançada.",
            fontSize = 15.sp
        )
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
                Text(text = "Conta a pagar", fontSize = 16.sp)
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
                Text(text = "Renda fixa", fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = {
                Text(
                    if (tipo == TipoTransacao.GASTO) {
                        "Nome da conta (ex: Conta de luz)"
                    } else {
                        "Nome da renda (ex: Salário)"
                    }
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        CampoValorMonetario(
            valorInicialCentavos = recorrencia.valorEsperadoCentavos,
            aoMudar = { valorCentavos = it },
            rotulo = "Valor esperado (pode variar)",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = diaTexto,
            onValueChange = { novo -> diaTexto = novo.filter { it.isDigit() }.take(2) },
            label = { Text("Dia do vencimento (1 a 31)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                aoSalvar(
                    recorrencia.copy(
                        tipo = tipo,
                        descricao = descricao.trim(),
                        valorEsperadoCentavos = valorCentavos,
                        categoria = categoria!!,
                        diaVencimento = dia!!
                    )
                )
            },
            enabled = descricao.isNotBlank() && valorCentavos > 0 &&
                categoria != null && dia != null && dia in 1..31,
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
}
