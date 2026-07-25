package com.familia.controledegastos.ui.telas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.R
import com.familia.controledegastos.model.Cartao
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.model.primeiraMaiuscula
import com.familia.controledegastos.ui.theme.VerdeGanho
import com.familia.controledegastos.ui.theme.VermelhoGasto

private val CorAlertaLimite = Color(0xFFB26A00)

// Cadastro dos cartões de crédito da família.
// Toque para editar, toque longo para excluir.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelaCartoes(
    cartoes: List<Cartao>,
    faturaDe: (Cartao) -> Long,
    aoSalvar: (Cartao) -> Unit,
    aoRemover: (Cartao) -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editando by remember { mutableStateOf<Cartao?>(null) }
    var paraExcluir by remember { mutableStateOf<Cartao?>(null) }

    BackHandler(onBack = aoVoltar)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Meus cartões",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Os cartões aparecem na hora de lançar um gasto no crédito.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (cartoes.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhum cartão ainda. Cadastre o primeiro!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        cartoes.forEach { cartao ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { editando = cartao },
                            onLongClick = { paraExcluir = cartao }
                        )
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cartao),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cartao.nome, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Fecha dia ${cartao.diaFechamento} • Vence dia ${cartao.diaVencimento}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val fatura = faturaDe(cartao)
                        Text(
                            text = "Fatura atual: ${formatarCentavos(fatura)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (cartao.limiteCentavos > 0) {
                            val fracao = fatura.toFloat() / cartao.limiteCentavos
                            val corBarra = when {
                                fracao > 1f -> VermelhoGasto
                                fracao >= 0.8f -> CorAlertaLimite
                                else -> VerdeGanho
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = fracao.coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .background(color = corBarra, shape = RoundedCornerShape(4.dp))
                                )
                            }
                            Text(
                                text = "de ${formatarCentavos(cartao.limiteCentavos)} de limite",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { editando = Cartao() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "+ Novo cartão", fontSize = 16.sp)
        }
        TextButton(
            onClick = aoVoltar,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Voltar", fontSize = 16.sp)
        }
    }

    editando?.let { cartao ->
        var nome by remember(cartao) { mutableStateOf(cartao.nome) }
        var vencimentoTexto by remember(cartao) {
            mutableStateOf(if (cartao.id.isBlank()) "" else cartao.diaVencimento.toString())
        }
        var fechamentoTexto by remember(cartao) {
            mutableStateOf(if (cartao.id.isBlank()) "" else cartao.diaFechamento.toString())
        }
        var limiteCentavos by remember(cartao) { mutableStateOf(cartao.limiteCentavos) }
        val vencimento = vencimentoTexto.toIntOrNull()
        val fechamento = fechamentoTexto.toIntOrNull()

        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text(if (cartao.id.isBlank()) "Novo cartão" else "Editar cartão") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome (ex: Nubank do Pai)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fechamentoTexto,
                        onValueChange = { novo -> fechamentoTexto = novo.filter { it.isDigit() }.take(2) },
                        label = { Text("Dia do fechamento da fatura") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = vencimentoTexto,
                        onValueChange = { novo -> vencimentoTexto = novo.filter { it.isDigit() }.take(2) },
                        label = { Text("Dia do vencimento da fatura") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CampoValorMonetario(
                        valorInicialCentavos = cartao.limiteCentavos,
                        aoMudar = { limiteCentavos = it },
                        rotulo = "Limite do cartão (opcional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        aoSalvar(
                            cartao.copy(
                                nome = nome.primeiraMaiuscula(),
                                diaVencimento = vencimento!!,
                                diaFechamento = fechamento!!,
                                limiteCentavos = limiteCentavos
                            )
                        )
                        editando = null
                    },
                    enabled = nome.isNotBlank() &&
                        vencimento != null && vencimento in 1..31 &&
                        fechamento != null && fechamento in 1..31
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editando = null }) { Text("Cancelar") }
            }
        )
    }

    paraExcluir?.let { cartao ->
        AlertDialog(
            onDismissRequest = { paraExcluir = null },
            title = { Text("Excluir cartão?") },
            text = {
                Text(
                    text = "${cartao.nome} — os lançamentos já feitos com ele não são apagados.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aoRemover(cartao)
                    paraExcluir = null
                }) { Text("Excluir", color = VermelhoGasto) }
            },
            dismissButton = {
                TextButton(onClick = { paraExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}
