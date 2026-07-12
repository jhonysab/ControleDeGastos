package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.ui.theme.VerdeGanho
import com.familia.controledegastos.ui.theme.VermelhoGasto
import com.familia.controledegastos.ui.theme.cor

private val CorAlerta = Color(0xFFB26A00) // 80–100% do limite

@Composable
fun AbaOrcamento(
    gastosPorCategoria: Map<Categoria, Long>,
    orcamentos: Map<Categoria, Long>,
    aoDefinir: (Categoria, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editando by remember { mutableStateOf<Categoria?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Limites do mês", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(
            text = "Toque numa categoria para definir ou mudar o limite.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Categoria.entries
            .filter { it != Categoria.SALARIO }
            .forEach { categoria ->
                LinhaOrcamento(
                    categoria = categoria,
                    gastoCentavos = gastosPorCategoria[categoria] ?: 0L,
                    limiteCentavos = orcamentos[categoria],
                    aoTocar = { editando = categoria }
                )
            }
        Spacer(modifier = Modifier.height(80.dp)) // respiro para o botão Lançar
    }

    editando?.let { categoria ->
        DialogoLimite(
            categoria = categoria,
            limiteAtualCentavos = orcamentos[categoria] ?: 0L,
            aoSalvar = { novoLimite ->
                aoDefinir(categoria, novoLimite)
                editando = null
            },
            aoFechar = { editando = null }
        )
    }
}

@Composable
private fun LinhaOrcamento(
    categoria: Categoria,
    gastoCentavos: Long,
    limiteCentavos: Long?,
    aoTocar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color = categoria.cor(), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = categoria.rotulo, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (limiteCentavos == null) {
                Text(
                    text = "Definir limite",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "${formatarCentavos(gastoCentavos)} de ${formatarCentavos(limiteCentavos)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (limiteCentavos != null) {
            val fracao = gastoCentavos.toFloat() / limiteCentavos
            val corBarra = when {
                fracao > 1f -> VermelhoGasto
                fracao >= 0.8f -> CorAlerta
                else -> VerdeGanho
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(5.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = fracao.coerceIn(0f, 1f))
                        .height(10.dp)
                        .background(color = corBarra, shape = RoundedCornerShape(5.dp))
                )
            }
            if (fracao > 1f) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Passou ${formatarCentavos(gastoCentavos - limiteCentavos)} do limite",
                    fontSize = 13.sp,
                    color = VermelhoGasto,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DialogoLimite(
    categoria: Categoria,
    limiteAtualCentavos: Long,
    aoSalvar: (Long) -> Unit,
    aoFechar: () -> Unit
) {
    var novoLimite by remember(categoria) { mutableLongStateOf(limiteAtualCentavos) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Limite mensal — ${categoria.rotulo}") },
        text = {
            Column {
                CampoValorMonetario(
                    valorInicialCentavos = limiteAtualCentavos,
                    aoMudar = { novoLimite = it },
                    modifier = Modifier.fillMaxWidth()
                )
                if (limiteAtualCentavos > 0) {
                    TextButton(onClick = { aoSalvar(0L) }) {
                        Text(text = "Remover limite", color = VermelhoGasto)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { aoSalvar(novoLimite) },
                enabled = novoLimite > 0
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
        }
    )
}
