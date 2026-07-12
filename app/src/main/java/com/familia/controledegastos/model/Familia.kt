package com.familia.controledegastos.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// O "cofre" compartilhado: as transações vivem numa subcoleção
// dentro de familias/{id}. `membros` guarda os uids com acesso —
// é o que as regras de segurança do Firestore vão verificar.
data class Familia(
    @DocumentId val id: String = "",
    val nome: String = "",
    val membros: List<String> = emptyList(),
    val criadaEm: Timestamp = Timestamp.now()
)
