package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.ui.ResumoMensal
import com.familia.controledegastos.ui.theme.VerdeGanho
import com.familia.controledegastos.ui.theme.VermelhoGasto
import com.familia.controledegastos.ui.theme.cor
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AbaGraficos(
    gastosPorCategoria: List<Pair<Categoria, Long>>,
    resumoMeses: List<ResumoMensal>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Para onde foi o dinheiro", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))

        if (gastosPorCategoria.isEmpty()) {
            Text(
                text = "Nenhum gasto neste mês ainda.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            GraficoDonut(fatias = gastosPorCategoria)
            Spacer(modifier = Modifier.height(16.dp))
            LegendaCategorias(fatias = gastosPorCategoria)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Últimos 6 meses", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
        GraficoBarrasMensal(resumo = resumoMeses)
        Spacer(modifier = Modifier.height(80.dp)) // respiro para o botão Lançar
    }
}

// Donut desenhado à mão: cada categoria vira um arco proporcional
// ao seu peso no total (360° distribuídos pela regra de três).
@Composable
private fun GraficoDonut(fatias: List<Pair<Categoria, Long>>) {
    val total = fatias.sumOf { it.second }.coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(210.dp)) {
            val espessura = 44.dp.toPx()
            val alvo = Size(size.width - espessura, size.height - espessura)
            val canto = Offset(espessura / 2f, espessura / 2f)
            var anguloInicio = -90f // começa no "meio-dia"

            fatias.forEach { (categoria, valor) ->
                val varredura = 360f * valor / total
                drawArc(
                    color = categoria.cor(),
                    startAngle = anguloInicio,
                    sweepAngle = varredura - 1.5f, // frestinha entre fatias
                    useCenter = false,
                    style = Stroke(width = espessura),
                    topLeft = canto,
                    size = alvo
                )
                anguloInicio += varredura
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatarCentavos(fatias.sumOf { it.second }),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VermelhoGasto
            )
        }
    }
}

@Composable
private fun LegendaCategorias(fatias: List<Pair<Categoria, Long>>) {
    val total = fatias.sumOf { it.second }.coerceAtLeast(1)
    Column {
        fatias.forEach { (categoria, valor) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color = categoria.cor(), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = categoria.rotulo,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatarCentavos(valor),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${valor * 100 / total}%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(42.dp)
                )
            }
        }
    }
}

// Barras feitas só com Boxes: a altura de cada barra é a fração
// do maior valor entre todos os meses (alinhadas pela base).
@Composable
private fun GraficoBarrasMensal(resumo: List<ResumoMensal>) {
    val maior = resumo.maxOfOrNull { maxOf(it.ganhosCentavos, it.gastosCentavos) } ?: 0L

    if (maior == 0L) {
        Text(
            text = "Sem movimentações no período.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        resumo.forEach { mes ->
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Barra(fracao = mes.ganhosCentavos.toFloat() / maior, cor = VerdeGanho)
                Spacer(modifier = Modifier.width(3.dp))
                Barra(fracao = mes.gastosCentavos.toFloat() / maior, cor = VermelhoGasto)
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        resumo.forEach { mes ->
            Text(
                text = nomeCurtoDoMes(mes),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(12.dp)
            .background(VerdeGanho, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Entradas", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier
            .size(12.dp)
            .background(VermelhoGasto, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Saídas", fontSize = 14.sp)
    }
}

@Composable
private fun Barra(fracao: Float, cor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .fillMaxHeight(fraction = fracao.coerceIn(0.005f, 1f))
            .background(color = cor, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    )
}

private fun nomeCurtoDoMes(resumo: ResumoMensal): String =
    resumo.mes.format(DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("pt-BR")))
        .replace(".", "")
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) }
