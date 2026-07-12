package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.Usuario

// Tela inicial PROVISÓRIA: prova que o login e a família funcionaram.
// Vai virar a tela principal do app (lista + total do mês) na sequência.
@Composable
fun TelaInicial(
    usuario: Usuario,
    aoSair: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Olá, ${usuario.nome}! 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Sua família está conectada. ✅", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Código da família — compartilhe com quem for entrar no outro celular:",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectionContainer {
            Text(
                text = usuario.familiaId,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        TextButton(onClick = aoSair) {
            Text(text = "Sair da conta", fontSize = 16.sp)
        }
    }
}
