package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaCadastro(
    processando: Boolean,
    erro: String?,
    aoCadastrar: (nome: String, email: String, senha: String) -> Unit,
    aoVoltarParaLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Criar conta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Seu nome (ex: Maria)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha (mínimo 6 caracteres)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (erro != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = erro, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { aoCadastrar(nome, email, senha) },
            enabled = !processando,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (processando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(text = "Cadastrar", fontSize = 18.sp)
            }
        }
        TextButton(
            onClick = aoVoltarParaLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Já tenho conta", fontSize = 16.sp)
        }
    }
}
