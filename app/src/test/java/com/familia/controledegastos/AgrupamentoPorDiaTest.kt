package com.familia.controledegastos

import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Transacao
import com.familia.controledegastos.ui.agruparPorDia
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// A separação por dia da aba Resumo.
class AgrupamentoPorDiaTest {

    private val zona: ZoneId = ZoneId.of("America/Sao_Paulo")

    private fun lancamento(
        dia: String,
        centavos: Long,
        tipo: TipoTransacao = TipoTransacao.GASTO,
        descricao: String = ""
    ) = Transacao(
        tipo = tipo,
        valorCentavos = centavos,
        descricao = descricao,
        data = Timestamp(
            Date.from(LocalDate.parse(dia).atTime(12, 0).atZone(zona).toInstant())
        )
    )

    @Test
    fun `agrupa na ordem em que a lista chega`() {
        // como a tela entrega: mais recentes primeiro
        val lista = listOf(
            lancamento("2026-07-20", 1000),
            lancamento("2026-07-20", 2000),
            lancamento("2026-07-18", 500)
        )

        val dias = agruparPorDia(lista, zona)

        assertEquals(2, dias.size)
        assertEquals(LocalDate.parse("2026-07-20"), dias[0].dia)
        assertEquals(LocalDate.parse("2026-07-18"), dias[1].dia)
        assertEquals(2, dias[0].itens.size)
        assertEquals(1, dias[1].itens.size)
    }

    @Test
    fun `ordem antiga primeiro tambem e respeitada`() {
        val lista = listOf(
            lancamento("2026-07-18", 500),
            lancamento("2026-07-20", 1000)
        )

        val dias = agruparPorDia(lista, zona)

        assertEquals(LocalDate.parse("2026-07-18"), dias[0].dia)
        assertEquals(LocalDate.parse("2026-07-20"), dias[1].dia)
    }

    @Test
    fun `saldo do dia soma ganhos e desconta gastos`() {
        val lista = listOf(
            lancamento("2026-07-20", 30000, TipoTransacao.GANHO),
            lancamento("2026-07-20", 5000, TipoTransacao.GASTO),
            lancamento("2026-07-20", 2500, TipoTransacao.GASTO)
        )

        val dias = agruparPorDia(lista, zona)

        assertEquals(1, dias.size)
        assertEquals(22500L, dias[0].saldoCentavos)
    }

    @Test
    fun `dia so de gastos fica negativo`() {
        val dias = agruparPorDia(listOf(lancamento("2026-07-20", 7500)), zona)

        assertTrue(dias[0].saldoCentavos < 0)
        assertEquals(-7500L, dias[0].saldoCentavos)
    }

    @Test
    fun `lancamento de meio-dia nao escorrega para o dia vizinho`() {
        // o app grava tudo ao meio-dia local justamente para isso
        val dias = agruparPorDia(listOf(lancamento("2026-07-01", 100)), zona)

        assertEquals(LocalDate.parse("2026-07-01"), dias[0].dia)
    }

    @Test
    fun `lista vazia nao gera nenhum cabecalho`() {
        assertEquals(emptyList<Any>(), agruparPorDia(emptyList(), zona))
    }
}
