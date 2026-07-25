package com.familia.controledegastos.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

// Toque em qualquer área vazia da tela fecha o teclado.
// Botões, campos e itens da lista consomem o toque antes de chegar
// aqui, então continuam funcionando normalmente — só o toque "no vazio"
// sobra para nós.
@Composable
fun Modifier.fecharTecladoAoTocar(): Modifier {
    val foco = LocalFocusManager.current
    return pointerInput(Unit) {
        detectTapGestures(onTap = { foco.clearFocus() })
    }
}
