package com.familia.controledegastos.ui.telas

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.familia.controledegastos.model.formatarCentavos

// Campo de dinheiro "estilo app de banco": a pessoa digita só números
// e o texto se formata sozinho (8 -> R$ 0,08; 8990 -> R$ 89,90).
@Composable
fun CampoValorMonetario(
    valorInicialCentavos: Long,
    aoMudar: (Long) -> Unit,
    modifier: Modifier = Modifier,
    rotulo: String = "Valor"
) {
    var campo by remember {
        mutableStateOf(
            TextFieldValue(
                if (valorInicialCentavos > 0) formatarCentavos(valorInicialCentavos) else ""
            )
        )
    }

    OutlinedTextField(
        value = campo,
        onValueChange = { novo ->
            val digitos = novo.text.filter { it.isDigit() }.take(10)
            val centavos = digitos.toLongOrNull() ?: 0L
            aoMudar(centavos)
            val texto = if (digitos.isEmpty()) "" else formatarCentavos(centavos)
            campo = TextFieldValue(text = texto, selection = TextRange(texto.length))
        },
        label = { Text(rotulo) },
        placeholder = { Text("R$ 0,00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(fontSize = 24.sp),
        modifier = modifier
    )
}
