package com.victorlanes.focinhos;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.Calendar;

/** Lembretes (vacinas, remédios, banho...) e aniversário dos pets como notificações. */
public class Avisos extends BroadcastReceiver {

    static final String ACAO_LEMBRETE = "com.victorlanes.focinhos.LEMBRETE";
    static final String ACAO_ANIVER = "com.victorlanes.focinhos.ANIVERSARIO";
    static final String ACAO_FEITO = "com.victorlanes.focinhos.FEITO";
    static final String CANAL = "lembretes";

    private static PendingIntent pi(Context c, String acao, long id) {
        Intent i = new Intent(c, Avisos.class).setAction(acao).setData(Uri.parse("focinhos://" + acao + "/" + id)).putExtra("id", id);
        return PendingIntent.getBroadcast(c, (int) (id % Integer.MAX_VALUE), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** (re)agenda todos os lembretes pendentes e os aniversários */
    public static void agendar(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        long agora = System.currentTimeMillis();
        for (Dados.Lembrete l : Dados.lembretes(c, 0)) {
            PendingIntent op = pi(c, ACAO_LEMBRETE, l.id);
            am.cancel(op);
            if (l.feito) continue;
            long t = l.quando();
            if (t > agora) definir(am, t, op);
        }
        for (Dados.Pet p : Dados.pets(c)) {
            PendingIntent op = pi(c, ACAO_ANIVER, p.id);
            am.cancel(op);
            if (p.nascimento.isEmpty()) continue;
            Calendar k = Datas.cal(Datas.proximoAniversario(p.nascimento));
            if (k == null) continue;
            k.set(Calendar.HOUR_OF_DAY, 9);
            if (k.getTimeInMillis() <= agora) k.add(Calendar.YEAR, 1);
            definir(am, k.getTimeInMillis(), op);
        }
        WidgetPet.atualizarTodos(c);
    }

    private static void definir(AlarmManager am, long t, PendingIntent op) {
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, op);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, op);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, op);
        }
    }

    /** marca como feito; se repete, empurra para a próxima data */
    public static void marcarFeito(Context c, Dados.Lembrete l) {
        if (l.repetirDias > 0) {
            String d = l.data;
            do d = Datas.somarDias(d, l.repetirDias); while (Datas.diasAte(d) < 0);
            l.data = d;
        } else l.feito = true;
        Dados.salvarLembrete(c, l);
        c.getSystemService(NotificationManager.class).cancel((int) (l.id % 100000));
        agendar(c);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        long id = i.getLongExtra("id", 0);
        if (ACAO_LEMBRETE.equals(a)) {
            Dados.Lembrete l = Dados.lembrete(c, id);
            if (l == null || l.feito) return;
            Dados.Pet p = Dados.pet(c, l.petId);
            notificar(c, (int) (l.id % 100000), "🐾 " + (p != null ? p.nome : "Lembrete"), l.titulo, p != null ? p.id : 0, l.id);
        } else if (ACAO_ANIVER.equals(a)) {
            Dados.Pet p = Dados.pet(c, id);
            if (p != null) {
                String idade = Datas.idade(p.nascimento);
                notificar(c, 200000 + (int) (p.id % 100000), "🎂 Hoje é aniversário do " + p.nome + "!",
                        idade.isEmpty() ? "Dia de festa e petisco extra 🦴" : "Está fazendo " + idade + ". Dia de festa e petisco extra 🦴", p.id, 0);
            }
            agendar(c);
        } else if (ACAO_FEITO.equals(a)) {
            Dados.Lembrete l = Dados.lembrete(c, id);
            if (l != null) marcarFeito(c, l);
        } else {
            agendar(c); // boot, troca de horário, atualização do app
        }
    }

    static void notificar(Context c, int nid, String titulo, String texto, long petId, long lembreteId) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CANAL) == null) {
            NotificationChannel ch = new NotificationChannel(CANAL, "Lembretes dos pets", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
        Intent abrir = new Intent(c, petId != 0 ? PetActivity.class : MainActivity.class).putExtra("pet", petId).putExtra("aba", "lembretes")
                .setData(Uri.parse("focinhos://abrir/" + petId + "/" + nid)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Notification.Builder b = new Notification.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_pata)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new Notification.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .setColor(0xFFF26B2E)
                .setContentIntent(PendingIntent.getActivity(c, nid, abrir, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        if (lembreteId != 0) b.addAction(new Notification.Action.Builder(null, "✓ Feito", pi(c, ACAO_FEITO, lembreteId)).build());
        try {
            nm.notify(nid, b.build());
        } catch (SecurityException ignored) {
        }
    }
}
