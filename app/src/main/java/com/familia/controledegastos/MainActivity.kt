package com.familia.controledegastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.ui.theme.ControleDeGastosTheme
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ControleDeGastosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TesteFirebase(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Tela TEMPORÁRIA só para validar a conexão com o Firestore.
// Será substituída pela tela real do app na Fase 2.
@Composable
fun TesteFirebase(modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Aperte o botão para testar") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = status,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            status = "Enviando para a nuvem..."
            FirebaseFirestore.getInstance()
                .collection("teste")
                .add(
                    mapOf(
                        "mensagem" to "Olá, nuvem! O app está conectado.",
                        "criadoEm" to Timestamp.now()
                    )
                )
                .addOnSuccessListener { doc ->
                    status = "✅ Funcionou!\nDocumento salvo com id:\n${doc.id}"
                }
                .addOnFailureListener { erro ->
                    status = "❌ Falhou: ${erro.message}"
                }
        }) {
            Text(text = "Testar conexão com a nuvem", fontSize = 18.sp)
        }
    }
}
