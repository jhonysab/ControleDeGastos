package com.familia.controledegastos.model

import com.google.firebase.firestore.DocumentId

// O id do documento é o mesmo uid do Firebase Auth:
// login -> busca usuarios/{uid} -> descobre a familiaId.
data class Usuario(
    @DocumentId val id: String = "",
    val nome: String = "",
    val familiaId: String = ""
)
