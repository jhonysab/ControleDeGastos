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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
    categorias: List<Categoria>,
    gastoPorCategoriaId: Map<String, Long>,
    orcamentos: Map<String, Long>,
    aoDefinir: (String, Long) -> Unit,
    aoArquivar: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editando by remember { mutableStateOf<Categoria?>(null) }
    var mostrandoArquivadas by remember { mutableStateOf(false) }

    val visiveis = categorias.filter { !it.ocultaNosLimites }
    val arquivadas = categorias.filter { it.ocultaNosLimites }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Limites do mês", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(
            text = "Toque numa categoria para definir o limite. Arquive as que " +
                "não precisam — elas continuam valendo nos lançamentos.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (visiveis.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Todas as categorias estão arquivadas aqui. Abra o grupo " +
                    "abaixo para trazer alguma de volta.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        visiveis.forEach { categoria ->
            LinhaOrcamento(
                categoria = categoria,
                gastoCentavos = gastoPorCategoriaId[categoria.id] ?: 0L,
                limiteCentavos = orcamentos[categoria.id],
                aoTocar = { editando = categoria },
                aoArquivar = { aoArquivar(categoria.id, true) }
            )
        }

        if (arquivadas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrandoArquivadas = !mostrandoArquivadas }
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "ARQUIVADAS (${arquivadas.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (mostrandoArquivadas) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (mostrandoArquivadas) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mostrandoArquivadas) {
                arquivadas.forEach { categoria ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color = categoria.cor(), shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = categoria.rotulo,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { aoArquivar(categoria.id, false) }) {
                            Text(text = "Restaurar", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // respiro para o botão Lançar
    }

    editando?.let { categoria ->
        DialogoLimite(
            categoria = categoria,
            limiteAtualCentavos = orcamentos[categoria.id] ?: 0L,
            aoSalvar = { novoLimite ->
                aoDefinir(categoria.id, novoLimite)
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
    aoTocar: () -> Unit,
    aoArquivar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(vertical = 4.dp)
    ) {
        // O nome fica sozinho na primeira linha: sobra espaço para o
        // botão de arquivar sem espremer nada.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color = categoria.cor(), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = categoria.rotulo,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = aoArquivar) {
                Text(text = "Arquivar", fontSize = 14.sp)
            }
        }

        if (limiteCentavos == null) {
            Text(
                text = "Definir limite",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        } else {
            val fracao = gastoCentavos.toFloat() / limiteCentavos
            val corBarra = when {
                fracao > 1f -> VermelhoGasto
                fracao >= 0.8f -> CorAlerta
                else -> VerdeGanho
            }
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatarCentavos(gastoCentavos)} de ${formatarCentavos(limiteCentavos)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (fracao > 1f) {
                Text(
                    text = "Passou ${formatarCentavos(gastoCentavos - limiteCentavos)} do limite",
                    fontSize = 13.sp,
                    color = VermelhoGasto,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
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
