package com.familia.controledegastos.ui.telas

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.ui.theme.VerdeGanho
import com.familia.controledegastos.ui.theme.VermelhoGasto
import androidx.compose.ui.graphics.Color

// Contas do mês, separadas em dois grupos: o que sai (contas a pagar)
// e o que entra (rendas fixas). Cada linha mostra o status no mês
// selecionado. Toque = editar; toque longo = excluir; "Lançar" =
// registrar o pagamento com tudo pré-preenchido.
@Composable
fun AbaContas(
    recorrencias: List<Recorrencia>,
    pagasNoMes: Set<String>,
    corDaCategoria: (String) -> Color,
    aoLancar: (Recorrencia) -> Unit,
    aoEditar: (Recorrencia) -> Unit,
    aoRemover: (Recorrencia) -> Unit,
    aoCriarNova: () -> Unit,
    modifier: Modifier = Modifier
) {
    var paraExcluir by remember { mutableStateOf<Recorrencia?>(null) }

    val aPagar = recorrencias.filter { it.tipo == TipoTransacao.GASTO }
    val aReceber = recorrencias.filter { it.tipo == TipoTransacao.GANHO }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Contas de todo mês", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(
            text = "Toque para editar, segure para excluir.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (recorrencias.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cadastre aqui o que se repete todo mês — luz, água, internet, salário — e o app te espera lançar cada uma.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        GrupoDeContas(
            titulo = "Contas a pagar",
            cor = VermelhoGasto,
            itens = aPagar,
            pagasNoMes = pagasNoMes,
            corDaCategoria = corDaCategoria,
            aoLancar = aoLancar,
            aoEditar = aoEditar,
            aoSegurar = { paraExcluir = it }
        )
        GrupoDeContas(
            titulo = "Rendas fixas",
            cor = VerdeGanho,
            itens = aReceber,
            pagasNoMes = pagasNoMes,
            corDaCategoria = corDaCategoria,
            aoLancar = aoLancar,
            aoEditar = aoEditar,
            aoSegurar = { paraExcluir = it }
        )

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = aoCriarNova,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "+ Nova conta recorrente", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(80.dp)) // respiro para o botão Lançar
    }

    paraExcluir?.let { recorrencia ->
        AlertDialog(
            onDismissRequest = { paraExcluir = null },
            title = { Text("Excluir conta recorrente?") },
            text = {
                Text(
                    text = "${recorrencia.descricao} — os lançamentos já feitos não são apagados.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    aoRemover(recorrencia)
                    paraExcluir = null
                }) { Text("Excluir", color = VermelhoGasto) }
            },
            dismissButton = {
                TextButton(onClick = { paraExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

// Um bloco da aba (a pagar / a receber). Some da tela quando não
// há nenhuma conta daquele tipo.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrupoDeContas(
    titulo: String,
    cor: Color,
    itens: List<Recorrencia>,
    pagasNoMes: Set<String>,
    corDaCategoria: (String) -> Color,
    aoLancar: (Recorrencia) -> Unit,
    aoEditar: (Recorrencia) -> Unit,
    aoSegurar: (Recorrencia) -> Unit
) {
    if (itens.isEmpty()) return

    val total = itens.sumOf { it.valorEsperadoCentavos }
    val faltam = itens.count { it.id !in pagasNoMes }

    Spacer(modifier = Modifier.height(16.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = titulo,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = cor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatarCentavos(total),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text(
        text = if (faltam == 0) {
            "Tudo lançado neste mês ✓"
        } else {
            "$faltam de ${itens.size} ainda não lançada" + if (faltam > 1) "s" else ""
        },
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider(thickness = 1.dp, color = cor)

    itens.forEach { recorrencia ->
        val paga = recorrencia.id in pagasNoMes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { aoEditar(recorrencia) },
                    onLongClick = { aoSegurar(recorrencia) }
                )
                .padding(vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color = corDaCategoria(recorrencia.categoria), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recorrencia.descricao,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Dia ${recorrencia.diaVencimento} • ${formatarCentavos(recorrencia.valorEsperadoCentavos)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (paga) {
                Text(
                    text = "✓ Lançada",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = VerdeGanho
                )
            } else {
                Button(onClick = { aoLancar(recorrencia) }) {
                    Text(text = "Lançar", fontSize = 15.sp)
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp)
    }
}
