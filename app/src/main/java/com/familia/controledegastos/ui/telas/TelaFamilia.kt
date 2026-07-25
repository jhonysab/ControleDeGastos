package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

// Primeira pessoa da casa cria a família; a segunda entra com o
// código que a primeira compartilhou.
@Composable
fun TelaFamilia(
    processando: Boolean,
    erro: String?,
    aoCriarFamilia: (nome: String) -> Unit,
    aoEntrarNaFamilia: (codigo: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nomeFamilia by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Falta um passo!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "Os gastos são compartilhados dentro de uma família.",
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(40.dp))

        Text(text = "Sou a primeira pessoa da casa:", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = nomeFamilia,
            onValueChange = { nomeFamilia = it },
            label = { Text("Nome da família (ex: Família Silva)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { aoCriarFamilia(nomeFamilia) },
            enabled = !processando,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Criar família", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Alguém já criou a família no outro celular:", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código da família") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { aoEntrarNaFamilia(codigo) },
            enabled = !processando,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Entrar na família", fontSize = 18.sp)
        }

        if (erro != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = erro, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
        }
    }
}
