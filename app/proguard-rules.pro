# Regras do R8 para o build de release.

# --- Firestore: os modelos são desserializados por REFLEXÃO ---
# (toObject/toObjects lê os campos pelo nome). Sem isto o R8 renomeia
# os campos e o Firestore não consegue remontar Transacao, Categoria etc.
-keep class com.familia.controledegastos.model.** { *; }

# Enums (TipoTransacao, FormaPagamento, TipoCategoria) são gravados no
# banco pelo .name ("GASTO", "CREDITO"...). Os nomes das constantes não
# podem mudar, senão o valor salvo deixa de bater na leitura.
-keepclassmembers enum com.familia.controledegastos.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# WorkManager instancia o worker de lembretes por NOME DE CLASSE.
# Se o R8 renomear, o lembrete diário de vencimento para de funcionar.
-keep class com.familia.controledegastos.notificacoes.LembreteVencimentoWorker { *; }
