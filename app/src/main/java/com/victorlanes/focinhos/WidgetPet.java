package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Widget da tela inicial: foto do dog, idade, próxima vacina e próximo lembrete (QR da coleira opcional). */
public class WidgetPet extends AppWidgetProvider {

    static final String ACAO_FIXADO = "com.victorlanes.focinhos.WIDGET_FIXADO";

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("widgets", Context.MODE_PRIVATE);
    }

    static void salvar(Context c, int wid, long petId, boolean qr) {
        sp(c).edit().putLong("pet" + wid, petId).putBoolean("qr" + wid, qr).apply();
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager am, int[] ids) {
        for (int id : ids) atualizar(c, am, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager am, int id, Bundle o) {
        atualizar(c, am, id);
    }

    @Override
    public void onDeleted(Context c, int[] ids) {
        SharedPreferences.Editor e = sp(c).edit();
        for (int id : ids) e.remove("pet" + id).remove("qr" + id);
        e.apply();
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (ACAO_FIXADO.equals(i.getAction())) {
            int wid = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (wid != AppWidgetManager.INVALID_APPWIDGET_ID) {
                salvar(c, wid, i.getLongExtra("pet", 0), i.getBooleanExtra("qr", false));
                atualizar(c, AppWidgetManager.getInstance(c), wid);
            }
            return;
        }
        super.onReceive(c, i);
    }

    public static void atualizarTodos(Context c) {
        AppWidgetManager am = AppWidgetManager.getInstance(c);
        for (int id : am.getAppWidgetIds(new ComponentName(c, WidgetPet.class))) atualizar(c, am, id);
    }

    static void atualizar(Context c, AppWidgetManager am, int wid) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_pet);
        long petId = sp(c).getLong("pet" + wid, 0);
        Dados.Pet p = petId != 0 ? Dados.pet(c, petId) : null;
        if (p == null) {
            List<Dados.Pet> ps = Dados.pets(c);
            if (ps.size() == 1) p = ps.get(0); // só um pet: mostra ele
        }
        Intent abrir;
        if (p == null) {
            rv.setTextViewText(R.id.nome, "Meus Focinhos");
            rv.setTextViewText(R.id.linha1, "Toque para escolher o pet");
            rv.setViewVisibility(R.id.linha2, View.GONE);
            rv.setViewVisibility(R.id.linha3, View.GONE);
            rv.setViewVisibility(R.id.qr, View.GONE);
            abrir = new Intent(c, MainActivity.class);
        } else {
            rv.setTextViewText(R.id.nome, p.nome);
            rv.setTextViewText(R.id.linha1, Relatorios.juntar(" · ", p.raca, p.idadeTexto()));
            Bitmap f = circulo(c, p, 160);
            if (f != null) rv.setImageViewBitmap(R.id.foto, f);

            // vacina
            String prox = "";
            for (Dados.Registro v : Relatorios.vacinasAtuais(c, p.id))
                if (!v.proxima.isEmpty() && (prox.isEmpty() || v.proxima.compareTo(prox) < 0)) prox = v.proxima;
            rv.setViewVisibility(R.id.linha2, View.VISIBLE);
            if (prox.isEmpty()) rv.setTextViewText(R.id.linha2, "💉 Sem vacina agendada");
            else {
                int d = Datas.diasAte(prox);
                rv.setTextViewText(R.id.linha2, d < 0 ? "⚠️ Vacina atrasada (" + Datas.br(prox) + ")" : "💉 Próxima vacina " + Datas.relativo(prox));
            }
            // lembrete / aniversário
            String l3 = null;
            for (Dados.Lembrete m : Dados.lembretes(c, p.id)) {
                if (m.feito) continue;
                l3 = "🔔 " + m.titulo + " · " + Datas.relativo(m.data);
                break;
            }
            if (!p.nascimento.isEmpty()) {
                int d = Datas.diasAte(Datas.proximoAniversario(p.nascimento));
                if (d == 0) l3 = "🎂 Hoje é o aniversário do " + p.nome + "!";
                else if (d <= 7 && l3 == null) l3 = "🎂 Aniversário " + Datas.relativo(Datas.proximoAniversario(p.nascimento));
            }
            rv.setViewVisibility(R.id.linha3, View.VISIBLE);
            rv.setTextViewText(R.id.linha3, l3 != null ? l3 : "🔔 Nenhum lembrete pendente");

            boolean qr = sp(c).getBoolean("qr" + wid, false);
            Bundle o = am.getAppWidgetOptions(wid);
            int larg = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300);
            if (qr && larg >= 220) {
                Bitmap q = Qr.gerar(Qr.textoColeira(c, p, Qr.opcoes(c, p.id)), 220, 0xFF2B1D14, 0xFFFFFFFF);
                if (q != null) {
                    rv.setImageViewBitmap(R.id.qr, q);
                    rv.setViewVisibility(R.id.qr, View.VISIBLE);
                }
            } else rv.setViewVisibility(R.id.qr, View.GONE);
            abrir = new Intent(c, PetActivity.class).putExtra("pet", p.id);
        }
        abrir.setData(Uri.parse("focinhos://widget/" + wid)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.raiz, PendingIntent.getActivity(c, 7000 + wid, abrir, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        try {
            am.updateAppWidget(wid, rv);
        } catch (Exception ignored) {
        }
    }

    /** foto redonda com borda branca (ou patinha) */
    static Bitmap circulo(Context c, Dados.Pet p, int px) {
        Bitmap out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        Paint borda = new Paint(Paint.ANTI_ALIAS_FLAG);
        borda.setColor(0xFFFFFFFF);
        cv.drawCircle(px / 2f, px / 2f, px / 2f, borda);
        float r = px / 2f - px * 0.04f;
        Bitmap f = Fotos.carregar(c, p.foto);
        if (f != null) {
            Paint ft = new Paint(Paint.ANTI_ALIAS_FLAG);
            BitmapShader sh = new BitmapShader(f, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            float e = 2 * r / Math.min(f.getWidth(), f.getHeight());
            m.setScale(e, e);
            m.postTranslate(px / 2f - r, px / 2f - r);
            sh.setLocalMatrix(m);
            ft.setShader(sh);
            cv.drawCircle(px / 2f, px / 2f, r, ft);
        } else {
            Paint vz = new Paint(Paint.ANTI_ALIAS_FLAG);
            vz.setColor(0xFFFFE6D9);
            cv.drawCircle(px / 2f, px / 2f, r, vz);
            Paint em = new Paint(Paint.ANTI_ALIAS_FLAG);
            em.setTextSize(px * 0.5f);
            em.setTextAlign(Paint.Align.CENTER);
            cv.drawText("🐶", px / 2f, px / 2f + px * 0.18f, em);
        }
        return out;
    }

    /** pede ao Android para colocar o widget de um pet na tela inicial */
    public static void fixar(Activity a, long petId, boolean qr) {
        AppWidgetManager am = AppWidgetManager.getInstance(a);
        if (!am.isRequestPinAppWidgetSupported()) {
            Toast.makeText(a, "Segure a tela inicial → Widgets → Meus Focinhos", Toast.LENGTH_LONG).show();
            return;
        }
        Intent cb = new Intent(a, WidgetPet.class).setAction(ACAO_FIXADO).putExtra("pet", petId).putExtra("qr", qr)
                .setData(Uri.parse("focinhos://fixar/" + petId));
        PendingIntent pi = PendingIntent.getBroadcast(a, (int) (petId % 100000), cb, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        am.requestPinAppWidget(new ComponentName(a, WidgetPet.class), null, pi);
    }

    // ------------------------------------------------------------------ escolher o pet do widget

    public static class Config extends Activity {
        private int wid;

        @Override
        protected void onCreate(Bundle b) {
            Visual.preparar(this, false);
            super.onCreate(b);
            setResult(RESULT_CANCELED);
            wid = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) {
                finish();
                return;
            }
            Ui.Cores k = Ui.cores(this);
            ScrollView sv = new ScrollView(this);
            sv.setBackgroundColor(k.fundo);
            LinearLayout col = Ui.coluna(this);
            int p = Ui.dp(this, 18);
            col.setPadding(p, p, p, p);
            sv.addView(col);
            setContentView(sv);
            Ui.aplicarInsets(sv);
            col.addView(Ui.texto(this, "Widget de qual pet?", 22, k.texto, true));
            Switch qr = new Switch(this);
            qr.setText("Mostrar o QR code da coleira no widget");
            qr.setTextColor(k.texto);
            qr.setTextSize(15);
            qr.setChecked(sp(this).getBoolean("qr" + wid, false));
            qr.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
            col.addView(qr);
            List<Dados.Pet> ps = Dados.pets(this);
            if (ps.isEmpty()) {
                col.addView(Ui.texto(this, "Cadastre um pet no app primeiro 🐾", 15, k.sub, false));
            }
            long atual = sp(this).getLong("pet" + wid, 0);
            for (Dados.Pet pet : ps) {
                LinearLayout l = Ui.linha(this);
                l.setBackground(Ui.clicavel(pet.id == atual ? k.chipSel : k.superficie, Ui.dp(this, 20), 0x22000000));
                l.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
                l.addView(Fotos.avatar(this, pet, Ui.dp(this, 52)));
                LinearLayout t = Ui.coluna(this);
                t.setPadding(Ui.dp(this, 12), 0, 0, 0);
                t.addView(Ui.texto(this, pet.nome, 17, k.texto, true));
                t.addView(Ui.texto(this, Relatorios.juntar(" · ", pet.raca, pet.idadeTexto()), 13, k.sub, false));
                l.addView(t, Ui.lp(0, -2, 1f));
                l.setOnClickListener(v -> {
                    salvar(this, wid, pet.id, qr.isChecked());
                    atualizar(this, AppWidgetManager.getInstance(this), wid);
                    setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
                    finish();
                });
                LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
                lp.topMargin = Ui.dp(this, 10);
                col.addView(l, lp);
            }
        }
    }
}
