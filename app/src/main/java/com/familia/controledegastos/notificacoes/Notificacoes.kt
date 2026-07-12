package com.familia.controledegastos.notificacoes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familia.controledegastos.MainActivity
import com.familia.controledegastos.R

object Notificacoes {

    private const val CANAL_VENCIMENTOS = "vencimentos"
    private const val ID_NOTIFICACAO_VENCIMENTOS = 1001

    fun criarCanal(contexto: Context) {
        val canal = NotificationChannel(
            CANAL_VENCIMENTOS,
            "Vencimentos de contas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos quando uma conta da família está perto de vencer"
        }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun avisarVencimentos(contexto: Context, mensagens: List<String>) {
        if (mensagens.isEmpty()) return
        if (!NotificationManagerCompat.from(contexto).areNotificationsEnabled()) return
        criarCanal(contexto)

        val abrirApp = PendingIntent.getActivity(
            contexto,
            0,
            Intent(contexto, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val construtor = NotificationCompat.Builder(contexto, CANAL_VENCIMENTOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(
                if (mensagens.size == 1) mensagens.first()
                else "${mensagens.size} contas perto de vencer"
            )
            .setContentIntent(abrirApp)
            .setAutoCancel(true)

        if (mensagens.size > 1) {
            val estilo = NotificationCompat.InboxStyle()
            mensagens.forEach { estilo.addLine(it) }
            construtor.setStyle(estilo)
        }

        try {
            NotificationManagerCompat.from(contexto)
                .notify(ID_NOTIFICACAO_VENCIMENTOS, construtor.build())
        } catch (_: SecurityException) {
            // permissão revogada entre a checagem e o notify — sem drama
        }
    }
}
