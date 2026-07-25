package com.familia.controledegastos

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.familia.controledegastos.model.Categoria
import com.familia.controledegastos.model.Recorrencia
import com.familia.controledegastos.model.TipoCategoria
import com.familia.controledegastos.model.TipoTransacao
import com.familia.controledegastos.model.Usuario
import com.familia.controledegastos.model.formatarCentavos
import com.familia.controledegastos.ui.TransacoesViewModel
import com.familia.controledegastos.ui.telas.AbaContas
import com.familia.controledegastos.ui.telas.AbaOrcamento
import com.familia.controledegastos.ui.telas.TelaCategorias
import com.familia.controledegastos.ui.telas.TelaPrincipal
import com.familia.controledegastos.ui.theme.ControleDeGastosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Telas novas da semana de teste dos pais, exercitadas com dados de
// mentira (não precisa de login nem de internet).
@RunWith(AndroidJUnit4::class)
class MelhoriasUiTest {

    @get:Rule
    val regra = createComposeRule()

    // screencap do aparelho inteiro (pega diálogo junto) rodando como
    // shell, então o arquivo sobrevive à desinstalação do app.
    private fun print(nome: String) {
        regra.waitForIdle()
        val fd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/$nome.png")
        // ler até o fim = esperar o comando terminar
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
    }

    // ---------- item 7: aba Contas separada em dois grupos ----------

    @Test
    fun contas_ficam_separadas_em_gastos_e_ganhos() {
        val contas = listOf(
            Recorrencia("luz", "Conta de luz", TipoTransacao.GASTO, "LUZ", 18_000, 10),
            Recorrencia("net", "Internet", TipoTransacao.GASTO, "INTERNET", 12_000, 15),
            Recorrencia("salario", "Salário", TipoTransacao.GANHO, "SALARIO", 300_000, 5)
        )

        regra.setContent {
            ControleDeGastosTheme {
                AbaContas(
                    recorrencias = contas,
                    pagasNoMes = setOf("luz"),
                    corDaCategoria = { androidx.compose.ui.graphics.Color.Gray },
                    aoLancar = {},
                    aoEditar = {},
                    aoRemover = {},
                    aoCriarNova = {}
                )
            }
        }

        print("item7-contas-agrupadas")

        regra.onNodeWithText("Contas a pagar").assertIsDisplayed()
        regra.onNodeWithText("Rendas fixas").assertIsDisplayed()
        // O total de cada grupo prova que a divisão é por tipo: 180 + 120
        // nas contas, 3.000 na renda. (formatarCentavos monta o texto do
        // mesmo jeito que o app — o "R$ " do pt-BR usa espaço especial.)
        regra.onNodeWithText(formatarCentavos(30_000)).assertIsDisplayed()
        regra.onNodeWithText(formatarCentavos(300_000)).assertIsDisplayed()
        // a luz já foi lançada; a internet ainda não
        regra.onNodeWithText("1 de 2 ainda não lançada").assertIsDisplayed()
    }

    // ---------- item 8: arquivar categoria na aba Limites ----------

    @Test
    fun limites_escondem_arquivadas_ate_expandir_o_grupo() {
        var arquivou: Pair<String, Boolean>? = null
        val categorias = listOf(
            Categoria("MERCADO", "Mercado", "#EF6C00", TipoCategoria.GASTO),
            Categoria("LUZ", "Luz", "#F9A825", TipoCategoria.GASTO),
            Categoria("LAZER", "Lazer", "#9CCC65", TipoCategoria.GASTO, ocultaNosLimites = true)
        )

        regra.setContent {
            ControleDeGastosTheme {
                AbaOrcamento(
                    categorias = categorias,
                    gastoPorCategoriaId = mapOf("MERCADO" to 45_000L),
                    orcamentos = mapOf("MERCADO" to 80_000L),
                    aoDefinir = { _, _ -> },
                    aoArquivar = { id, oculta -> arquivou = id to oculta }
                )
            }
        }

        regra.onNodeWithText("Mercado").assertIsDisplayed()
        // arquivada não aparece na lista, só a contagem no grupo fechado
        regra.onAllNodesWithText("Lazer").assertCountEquals(0)
        regra.onNodeWithText("ARQUIVADAS (1)").assertIsDisplayed()
        print("item8-limites-grupo-fechado")

        // expandir mostra a arquivada com o botão de trazer de volta
        regra.onNodeWithText("ARQUIVADAS (1)").performClick()
        regra.onNodeWithText("Lazer").assertIsDisplayed()
        regra.onNodeWithText("Restaurar").assertIsDisplayed()
        print("item8-limites-grupo-aberto")

        // recolher esconde de novo
        regra.onNodeWithText("ARQUIVADAS (1)").performClick()
        regra.onAllNodesWithText("Lazer").assertCountEquals(0)

        // e arquivar uma visível avisa o ViewModel
        regra.onAllNodesWithText("Arquivar")[0].performClick()
        assertEquals("MERCADO" to true, arquivou)
    }

    // ---------- item 4: excluir categoria criada errada ----------

    @Test
    fun categoria_em_uso_nao_pode_ser_excluida() {
        var excluida: String? = null
        var arquivada: Pair<String, Boolean>? = null

        regra.setContent {
            ControleDeGastosTheme {
                TelaCategorias(
                    categorias = listOf(Categoria("MERCADO", "Mercado", "#EF6C00", TipoCategoria.GASTO)),
                    aoSalvar = {},
                    aoArquivar = { id, arq -> arquivada = id to arq },
                    aoExcluir = { excluida = it },
                    usosDaCategoria = { 5 },
                    aoVoltar = {}
                )
            }
        }

        regra.onNodeWithText("Mercado").performClick()          // abre a edição
        regra.onNodeWithText("Excluir categoria").performClick() // pede exclusão

        regra.onNodeWithText("Essa não dá para excluir").assertIsDisplayed()
        // nenhum botão de excluir sobra na tela: só resta arquivar
        regra.onAllNodesWithText("Excluir").assertCountEquals(0)
        print("item4-categoria-em-uso")

        regra.onNodeWithText("Arquivar em vez disso").performClick()
        assertNull(excluida)
        assertEquals("MERCADO" to true, arquivada)
    }

    @Test
    fun categoria_sem_lancamentos_e_excluida_de_vez() {
        var excluida: String? = null

        regra.setContent {
            ControleDeGastosTheme {
                TelaCategorias(
                    categorias = listOf(Categoria("ERRADA", "Caixa", "#EF6C00", TipoCategoria.GASTO)),
                    aoSalvar = {},
                    aoArquivar = { _, _ -> },
                    aoExcluir = { excluida = it },
                    usosDaCategoria = { 0 },
                    aoVoltar = {}
                )
            }
        }

        regra.onNodeWithText("Caixa").performClick()
        regra.onNodeWithText("Excluir categoria").performClick()

        regra.onNodeWithText("Excluir categoria?").assertIsDisplayed()
        print("item4-categoria-sem-uso")

        regra.onNodeWithText("Excluir").performClick()
        assertEquals("ERRADA", excluida)
    }

    // ---------- item 5: nome das abas sem quebrar linha ----------

    // Roda com a fonte do sistema aumentada (é o caso do celular dos
    // pais) — antes, "Gráficos" jogava as últimas letras para baixo.
    @Test
    fun nomes_das_abas_cabem_em_uma_linha() {
        regra.setContent {
            ControleDeGastosTheme {
                TelaPrincipal(
                    usuario = Usuario(id = "u1", nome = "Maria", familiaId = "f1"),
                    aoSair = {},
                    aoAtualizarNome = {},
                    vm = TransacoesViewModel(familiaId = "f1", uid = "u1")
                )
            }
        }

        val alturas = listOf("Resumo", "Gráficos", "Limites", "Contas").map { titulo ->
            val no = regra.onNodeWithText(titulo).fetchSemanticsNode()
            no.size.height
        }
        print("item5-abas")

        // todas as abas com a mesma altura = nenhuma quebrou em duas linhas
        assertEquals(1, alturas.distinct().size)
    }
}
