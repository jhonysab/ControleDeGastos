package com.familia.controledegastos.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

// Grava o CSV no cache do app e abre a folha de compartilhamento do
// Android (WhatsApp, e-mail, Drive, "salvar em Arquivos"...). Nenhuma
// permissao de armazenamento e necessaria: o FileProvider empresta o
// arquivo por uma URI temporaria.
object ExportacaoCsv {
    // BOM UTF-8: faz o Excel abrir os acentos (a, c...) corretamente.
    private const val BOM = "﻿"

    fun compartilhar(context: Context, conteudo: String, nomeArquivo: String) {
        val arquivo = File(context.cacheDir, nomeArquivo)
        arquivo.writeText(BOM + conteudo, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            arquivo
        )
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, nomeArquivo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(envio, "Exportar lançamentos"))
    }
}
