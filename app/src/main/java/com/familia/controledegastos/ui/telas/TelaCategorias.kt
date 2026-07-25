package com.familia.controledegastos.ui.telas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.CategoriasPadrao
import com.familia.controledegastos.model.TipoCategoria
import com.familia.controledegastos.model.primeiraMaiuscula
import com.familia.controledegastos.ui.theme.VermelhoGasto
import com.familia.controledegastos.ui.theme.corDoHex

// Gerenciar as categorias da família: criar, renomear, mudar cor/tipo,
// arquivar (some das opções, mas o histórico continua mostrando o nome)
// e excluir de vez as que foram criadas por engano.
@Composable
fun TelaCategorias(
    categorias: List<Categoria>,
    aoSalvar: (Categoria) -> Unit,
    aoArquivar: (String, Boolean) -> Unit,
    aoExcluir: (String) -> Unit,
    usosDaCategoria: (String) -> Int,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editando by remember { mutableStateOf<Categoria?>(null) }
    var paraExcluir by remember { mutableStateOf<Categoria?>(null) }

    BackHandler(onBack = aoVoltar)

    val ativas = categorias.filter { !it.arquivada }
    val arquivadas = categorias.filter { it.arquivada }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Categorias",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Personalize como sua família organiza os gastos e ganhos.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        ativas.forEach { categoria ->
            LinhaCategoria(
                categoria = categoria,
                aoTocar = { editando = categoria },
                aoArquivar = { aoArquivar(categoria.id, true) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { editando = Categoria() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "+ Nova categoria", fontSize = 16.sp)
        }

        if (arquivadas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Arquivadas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            arquivadas.forEach { categoria ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = categoria.nome,
                        fontSize = 16.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { aoArquivar(categoria.id, false) }) {
                        Text(text = "Reativar", fontSize = 14.sp)
                    }
                    TextButton(onClick = { paraExcluir = categoria }) {
                        Text(text = "Excluir", fontSize = 14.sp, color = VermelhoGasto)
                    }
                }
            }
        }

        TextButton(
            onClick = aoVoltar,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Voltar", fontSize = 16.sp)
        }
    }

    editando?.let { categoria ->
        DialogoCategoria(
            categoria = categoria,
            aoSalvar = {
                aoSalvar(it)
                editando = null
            },
            // Categoria nova ainda não existe para ser apagada
            aoPedirExclusao = if (categoria.id.isBlank()) null else {
                {
                    editando = null
                    paraExcluir = categoria
                }
            },
            aoFechar = { editando = null }
        )
    }

    paraExcluir?.let { categoria ->
        // Apagar uma categoria em uso deixaria os lançamentos antigos
        // apontando para o nada — nesse caso só oferecemos arquivar.
        val usos = usosDaCategoria(categoria.id)
        val emUso = usos > 0
        AlertDialog(
            onDismissRequest = { paraExcluir = null },
            title = { Text(if (emUso) "Essa não dá para excluir" else "Excluir categoria?") },
            text = {
                Text(
                    text = if (emUso) {
                        "\"${categoria.nome}\" já está em $usos lançamento" +
                            (if (usos > 1) "s" else "") + ". Se apagasse, eles ficariam " +
                            "sem categoria. Arquivar some das opções e preserva o histórico."
                    } else {
                        "\"${categoria.nome}\" será apagada de vez. Como ela não tem " +
                            "nenhum lançamento, nada do histórico se perde."
                    },
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                if (emUso) {
                    TextButton(onClick = {
                        aoArquivar(categoria.id, true)
                        paraExcluir = null
                    }) { Text("Arquivar em vez disso") }
                } else {
                    TextButton(onClick = {
                        aoExcluir(categoria.id)
                        paraExcluir = null
                    }) { Text("Excluir", color = VermelhoGasto) }
                }
            },
            dismissButton = {
                TextButton(onClick = { paraExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LinhaCategoria(
    categoria: Categoria,
    aoTocar: () -> Unit,
    aoArquivar: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(color = corDoHex(categoria.corHex), shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = categoria.nome, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(
                text = when (categoria.tipo) {
                    TipoCategoria.GASTO -> "Gastos"
                    TipoCategoria.GANHO -> "Ganhos"
                    TipoCategoria.AMBOS -> "Gastos e ganhos"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = aoArquivar) { Text("Arquivar") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoCategoria(
    categoria: Categoria,
    aoSalvar: (Categoria) -> Unit,
    aoPedirExclusao: (() -> Unit)?,
    aoFechar: () -> Unit
) {
    var nome by remember(categoria) { mutableStateOf(categoria.nome) }
    var corHex by remember(categoria) {
        mutableStateOf(if (categoria.id.isBlank()) CategoriasPadrao.paleta.first() else categoria.corHex)
    }
    var tipo by remember(categoria) { mutableStateOf(categoria.tipo) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (categoria.id.isBlank()) "Nova categoria" else "Editar categoria") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Cor", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoriasPadrao.paleta.forEach { hex ->
                        val selecionada = hex == corHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(color = corDoHex(hex), shape = CircleShape)
                                .border(
                                    width = if (selecionada) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { corHex = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Aparece em", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tipos = listOf(
                        TipoCategoria.GASTO to "Gastos",
                        TipoCategoria.GANHO to "Ganhos",
                        TipoCategoria.AMBOS to "Ambos"
                    )
                    tipos.forEachIndexed { index, (valor, rotulo) ->
                        SegmentedButton(
                            selected = tipo == valor,
                            onClick = { tipo = valor },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tipos.size)
                        ) {
                            Text(text = rotulo, fontSize = 14.sp)
                        }
                    }
                }
                aoPedirExclusao?.let { pedirExclusao ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = pedirExclusao) {
                        Text(text = "Excluir categoria", color = VermelhoGasto)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    aoSalvar(
                        categoria.copy(
                            nome = nome.primeiraMaiuscula(),
                            corHex = corHex,
                            tipo = tipo
                        )
                    )
                },
                enabled = nome.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
        }
    )
}
