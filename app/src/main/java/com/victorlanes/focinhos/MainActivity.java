package com.victorlanes.focinhos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Tela inicial: todos os focinhos, resumo e próximos lembretes. */
public class MainActivity extends Activity {

    private static final int REQ_SALVAR = 31, REQ_ABRIR = 32;
    private Ui.Cores k;
    private String busca = "";
    private LinearLayout lista;

    @Override
    protected void onCreate(Bundle b) {
        Visual.preparar(this, false);
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        Avisos.agendar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Visual.preparar(this, false);
        k = Ui.cores(this);
        montar();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void montar() {
        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(k.fundo);
        setContentView(raiz);
        ScrollView sv = new ScrollView(this);
        LinearLayout col = Ui.coluna(this);
        col.setPadding(dp(16), dp(14), dp(16), dp(110));
        sv.addView(col);
        raiz.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        Ui.aplicarInsets(raiz);

        LinearLayout topo = Ui.linha(this);
        LinearLayout tt = Ui.coluna(this);
        tt.addView(Ui.texto(this, "Meus Focinhos 🐾", 28, k.texto, true));
        tt.addView(Ui.texto(this, saudacao(), 14, k.sub, false));
        topo.addView(tt, Ui.lp(0, -2, 1f));
        TextView menu = Ui.texto(this, "⋮", 26, k.texto, true);
        menu.setPadding(dp(14), dp(4), dp(6), dp(4));
        menu.setOnClickListener(v -> menu());
        topo.addView(menu);
        col.addView(topo);

        List<Dados.Pet> pets = Dados.pets(this);
        if (pets.isEmpty()) {
            boasVindas(col);
        } else {
            resumo(col, pets);
            proximos(col);
            TextView t = Ui.texto(this, "Meus filhos de focinho (" + pets.size() + ")", 18, k.texto, true);
            t.setPadding(dp(4), dp(22), 0, dp(2));
            col.addView(t);
            if (pets.size() > 4) {
                EditText e = new EditText(this);
                Campos.estilo(this, e);
                e.setHint("🔎  Buscar por nome, raça ou dono");
                e.setSingleLine(true);
                e.setText(busca);
                e.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                    }

                    public void onTextChanged(CharSequence s, int a, int b, int c) {
                    }

                    public void afterTextChanged(Editable s) {
                        busca = s.toString();
                        pintarLista(pets);
                    }
                });
                LinearLayout.LayoutParams el = Ui.lp(-1, -2);
                el.topMargin = dp(8);
                col.addView(e, el);
            }
            lista = Ui.coluna(this);
            col.addView(lista);
            pintarLista(pets);
        }

        TextView fab = Ui.texto(this, "＋  Novo pet", 17, 0xFFFFFFFF, true);
        fab.setGravity(Gravity.CENTER);
        fab.setPadding(dp(24), 0, dp(24), 0);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, Ui.GRADIENTE);
        g.setCornerRadius(dp(30));
        fab.setBackground(g);
        fab.setElevation(dp(6));
        fab.setOnClickListener(v -> startActivity(new Intent(this, PetFormActivity.class)));
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-2, dp(60), Gravity.BOTTOM | Gravity.END);
        fl.setMargins(0, 0, dp(20), dp(24));
        raiz.addView(fab, fl);
    }

    private String saudacao() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return (h < 12 ? "Bom dia" : h < 18 ? "Boa tarde" : "Boa noite") + "! Tudo em dia com a turma?";
    }

    private void boasVindas(LinearLayout col) {
        LinearLayout c = Ui.coluna(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFFF9A5C, 0xFFFF6A8E});
        g.setCornerRadius(dp(28));
        c.setBackground(g);
        c.setPadding(dp(22), dp(30), dp(22), dp(30));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(24);
        col.addView(c, lp);
        c.addView(Ui.texto(this, "🐶", 64, 0, false));
        TextView t = Ui.texto(this, "Bem-vindo ao Meus Focinhos!", 20, 0xFFFFFFFF, true);
        t.setGravity(Gravity.CENTER);
        c.addView(t);
        TextView s = Ui.texto(this, "Cadastre seu primeiro dog e guarde aqui as consultas, vacinas, remédios, banhos, peso e gastos. "
                + "O app avisa as próximas doses e o aniversário de cada um 🎂", 14, 0xEEFFFFFF, false);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, dp(8), 0, dp(16));
        c.addView(s);
        android.widget.Button b = Ui.botao(this, "Cadastrar meu primeiro pet", false);
        b.setOnClickListener(v -> startActivity(new Intent(this, PetFormActivity.class)));
        c.addView(b, Ui.lp(-2, dp(50)));
    }

    private void resumo(LinearLayout col, List<Dados.Pet> pets) {
        String mes = Datas.hoje().substring(0, 7);
        long gasto = 0;
        int atrasadas = 0;
        for (Dados.Registro r : Dados.registros(this, 0)) if (r.data.startsWith(mes)) gasto += r.valor;
        for (Dados.Pet p : pets)
            for (Dados.Registro v : Relatorios.vacinasAtuais(this, p.id)) if (!v.proxima.isEmpty() && Datas.diasAte(v.proxima) < 0) atrasadas++;
        int pend = 0;
        for (Dados.Lembrete l : Dados.lembretes(this, 0)) if (!l.feito && Datas.diasAte(l.data) <= 7) pend++;

        LinearLayout l = Ui.linha(this);
        l.setPadding(0, dp(16), 0, 0);
        mini(l, "🐾", String.valueOf(pets.size()), pets.size() == 1 ? "focinho" : "focinhos", 0);
        mini(l, "💰", Dados.reais(gasto), "gastos no mês", 0);
        if (atrasadas > 0) mini(l, "💉", String.valueOf(atrasadas), atrasadas == 1 ? "vacina atrasada" : "vacinas atrasadas", 0xFFEF4444);
        else mini(l, "🔔", String.valueOf(pend), "lembretes na semana", 0);
        col.addView(l);
    }

    private void mini(LinearLayout l, String e, String v, String r, int cor) {
        LinearLayout c = Ui.coluna(this);
        c.setBackground(Ui.fundo(k.superficie, dp(18)));
        c.setPadding(dp(12), dp(12), dp(10), dp(12));
        c.setElevation(dp(1));
        c.addView(Ui.texto(this, e, 18, 0, false));
        TextView t = Ui.texto(this, v, 17, cor != 0 ? cor : k.texto, true);
        t.setMaxLines(1);
        t.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(t);
        TextView s = Ui.texto(this, r, 11, k.sub, false);
        s.setMaxLines(2);
        c.addView(s);
        LinearLayout.LayoutParams lp = Ui.lp(0, -1, 1f);
        if (l.getChildCount() > 0) lp.leftMargin = dp(8);
        l.addView(c, lp);
    }

    private void proximos(LinearLayout col) {
        List<Dados.Lembrete> ls = Dados.lembretes(this, 0);
        int n = 0;
        LinearLayout c = null;
        for (Dados.Lembrete m : ls) {
            if (m.feito || Datas.diasAte(m.data) > 30) continue;
            Dados.Pet p = Dados.pet(this, m.petId);
            if (p == null) continue;
            if (c == null) {
                c = Ui.secao(this, col, "🔔  Próximos lembretes", null);
            }
            LinearLayout li = Ui.linha(this);
            li.setPadding(0, dp(10), 0, 0);
            li.addView(Fotos.avatar(this, p, dp(36)));
            LinearLayout t = Ui.coluna(this);
            t.setPadding(dp(10), 0, 0, 0);
            TextView ti = Ui.texto(this, p.nome + " · " + m.titulo, 14, k.texto, true);
            ti.setMaxLines(1);
            ti.setEllipsize(TextUtils.TruncateAt.END);
            t.addView(ti);
            int d = Datas.diasAte(m.data);
            t.addView(Ui.texto(this, Datas.br(m.data) + " · " + Datas.relativo(m.data), 12, d < 0 ? 0xFFEF4444 : k.sub, false));
            li.addView(t, Ui.lp(0, -2, 1f));
            li.setOnClickListener(v -> startActivity(new Intent(this, PetActivity.class).putExtra("pet", p.id).putExtra("aba", "lembretes")));
            c.addView(li);
            if (++n == 4) break;
        }
        // aniversários do mês
        for (Dados.Pet p : Dados.pets(this)) {
            if (p.nascimento.isEmpty()) continue;
            int d = Datas.diasAte(Datas.proximoAniversario(p.nascimento));
            if (d > 30) continue;
            if (c == null) c = Ui.secao(this, col, "🔔  Próximos lembretes", null);
            TextView t = Ui.texto(this, "🎂  " + p.nome + (d == 0 ? " faz aniversário hoje!" : " faz aniversário " + Datas.relativo(Datas.proximoAniversario(p.nascimento))), 14, k.texto, false);
            t.setPadding(0, dp(10), 0, 0);
            c.addView(t);
        }
    }

    private void pintarLista(List<Dados.Pet> pets) {
        lista.removeAllViews();
        String q = busca.trim().toLowerCase();
        for (Dados.Pet p : pets) {
            if (!q.isEmpty() && !(p.nome + " " + p.raca + " " + p.donoNome).toLowerCase().contains(q)) continue;
            lista.addView(cartaoPet(p));
        }
    }

    private View cartaoPet(Dados.Pet p) {
        LinearLayout c = Ui.linha(this);
        c.setBackground(Ui.clicavel(k.superficie, dp(24), 0x22000000));
        c.setElevation(dp(1));
        c.setPadding(dp(14), dp(14), dp(14), dp(14));
        c.addView(Fotos.avatar(this, p, dp(66)));
        LinearLayout t = Ui.coluna(this);
        t.setPadding(dp(14), 0, 0, 0);
        TextView n = Ui.texto(this, p.nome, 19, k.texto, true);
        n.setMaxLines(1);
        n.setEllipsize(TextUtils.TruncateAt.END);
        t.addView(n);
        String sub = Relatorios.juntar(" · ", p.raca, p.idadeTexto());
        if (!sub.isEmpty()) t.addView(Ui.texto(this, sub, 13, k.sub, false));
        Dados.Registro vet = null;
        for (Dados.Registro r : Dados.registros(this, p.id)) if (r.foiAoVet() && Datas.diasAte(r.data) <= 0) {
            vet = r;
            break;
        }
        t.addView(Ui.texto(this, vet == null ? "🩺 Sem idas ao vet registradas" : "🩺 Vet " + Datas.relativo(vet.data) + " · " + Datas.br(vet.data), 12, k.sub, false));
        String prox = "";
        for (Dados.Registro v : Relatorios.vacinasAtuais(this, p.id))
            if (!v.proxima.isEmpty() && (prox.isEmpty() || v.proxima.compareTo(prox) < 0)) prox = v.proxima;
        if (!prox.isEmpty()) {
            int d = Datas.diasAte(prox);
            TextView vt = Ui.texto(this, d < 0 ? "💉 Vacina atrasada (" + Datas.br(prox) + ")" : "💉 Próxima vacina " + Datas.relativo(prox), 12,
                    d < 0 ? 0xFFEF4444 : d <= 15 ? 0xFFF59E0B : 0xFF10B981, true);
            t.addView(vt);
        }
        c.addView(t, Ui.lp(0, -2, 1f));
        c.addView(Ui.texto(this, "›", 28, k.sub, false));
        c.setOnClickListener(v -> startActivity(new Intent(this, PetActivity.class).putExtra("pet", p.id)));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    // ================================================================== menu

    private void menu() {
        String[] op = {"💾  Fazer backup (salvar arquivo)", "📂  Restaurar backup", "🎨  Tema: " + nomeTema(Dados.tema(this))};
        new AlertDialog.Builder(this).setItems(op, (d, w) -> {
            if (w == 0) {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE, "MeusFocinhos_backup_" + Datas.hoje() + ".json");
                startActivityForResult(i, REQ_SALVAR);
            } else if (w == 1) {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                startActivityForResult(i, REQ_ABRIR);
            } else {
                String[] ids = {"sistema", "claro", "escuro"};
                String[] ns = {"Igual ao celular", "Claro", "Escuro"};
                new AlertDialog.Builder(this).setTitle("Tema").setItems(ns, (a, x) -> {
                    Dados.salvarTema(this, ids[x]);
                    recreate();
                }).show();
            }
        }).show();
    }

    private String nomeTema(String t) {
        return t.equals("claro") ? "Claro" : t.equals("escuro") ? "Escuro" : "Igual ao celular";
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (res != RESULT_OK || d == null || d.getData() == null) return;
        Uri u = d.getData();
        if (req == REQ_SALVAR) {
            try (OutputStream out = getContentResolver().openOutputStream(u)) {
                out.write(Dados.exportar(this).toString().getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Backup salvo ✅ Guarde esse arquivo no Drive ou no computador", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Não consegui salvar o backup", Toast.LENGTH_SHORT).show();
            }
        } else if (req == REQ_ABRIR) {
            new AlertDialog.Builder(this).setTitle("Restaurar backup?").setMessage("Os dados atuais do app serão substituídos pelos do arquivo.")
                    .setPositiveButton("Restaurar", (a, w) -> {
                        try (InputStream in = getContentResolver().openInputStream(u)) {
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                            Dados.importar(this, new JSONObject(new String(bo.toByteArray(), StandardCharsets.UTF_8)));
                            Avisos.agendar(this);
                            Toast.makeText(this, "Backup restaurado 🐾", Toast.LENGTH_SHORT).show();
                            montar();
                        } catch (Exception e) {
                            Toast.makeText(this, "Arquivo inválido: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }).setNegativeButton("Cancelar", null).show();
        }
    }
}
