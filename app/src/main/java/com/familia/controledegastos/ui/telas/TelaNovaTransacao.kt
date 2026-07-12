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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.formatarCentavos

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaNovaTransacao(
    aoSalvar: (TipoTransacao, Long, Categoria, String) -> Unit,
    aoCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tipo by remember { mutableStateOf(TipoTransacao.GASTO) }
    var valorCampo by remember { mutableStateOf(TextFieldValue("")) }
    var valorCentavos by remember { mutableLongStateOf(0L) }
    var categoria by remember { mutableStateOf<Categoria?>(null) }
    var descricao by remember { mutableStateOf("") }

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

        // A pessoa digita só números e o campo se formata sozinho:
        // 8 -> R$ 0,08; 89 -> R$ 0,89; 8990 -> R$ 89,90 (estilo app de banco)
        OutlinedTextField(
            value = valorCampo,
            onValueChange = { novo ->
                val digitos = novo.text.filter { it.isDigit() }.take(10)
                valorCentavos = digitos.toLongOrNull() ?: 0L
                val texto = if (digitos.isEmpty()) "" else formatarCentavos(valorCentavos)
                valorCampo = TextFieldValue(text = texto, selection = TextRange(texto.length))
            },
            label = { Text("Valor") },
            placeholder = { Text("R$ 0,00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp),
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

        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição (opcional, ex: conta de luz)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { aoSalvar(tipo, valorCentavos, categoria!!, descricao) },
            enabled = valorCentavos > 0 && categoria != null,
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
