package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Peças compartilhadas entre as telas: detalhes de registro, editor e item de lembrete. */
public final class Detalhes {
    private Detalhes() {
    }

    private static int dp(Activity a, float v) {
        return Ui.dp(a, v);
    }

    // ------------------------------------------------------------------ registro

    /** detalhes do registro com ✏️ (editar) e ✕ (excluir) */
    public static void registro(Activity a, Dados.Registro r, Runnable aoMudar) {
        Ui.Cores k = Ui.cores(a);
        Dados.Pet pet = Dados.pet(a, r.petId);
        LinearLayout v = Ui.coluna(a);
        v.setPadding(dp(a, 22), dp(a, 18), dp(a, 22), dp(a, 6));
        LinearLayout top = Ui.linha(a);
        top.addView(Ui.texto(a, r.emoji() + "  " + r.nomeTipo(), 20, k.texto, true), Ui.lp(0, -2, 1f));
        TextView lapis = Ui.texto(a, "✏️", 22, 0, false);
        lapis.setPadding(dp(a, 10), dp(a, 4), dp(a, 10), dp(a, 4));
        TextView x = Ui.texto(a, "✕", 22, 0xFFEF4444, true);
        x.setPadding(dp(a, 10), dp(a, 4), dp(a, 4), dp(a, 4));
        top.addView(lapis);
        top.addView(x);
        v.addView(top);
        if (pet != null) linha(a, v, "Dog", pet.nome);
        linha(a, v, "Data", Datas.br(r.data) + " (" + Datas.relativo(r.data) + ")");
        if (!r.tipo.equals("peso")) linha(a, v, r.tipo.equals("vacina") ? "Qual vacina" : r.tipo.equals("consulta") ? "Motivo" : "O quê", r.titulo);
        linha(a, v, r.tipo.equals("banho") ? "Pet shop" : "Veterinário / clínica", r.clinica);
        if (r.tipo.equals("consulta")) linha(a, v, "Teve procedimento?", r.procedimento.isEmpty() ? "Não" : "Sim — " + r.procedimento);
        linha(a, v, r.tipo.equals("consulta") ? "Descrição da consulta" : "Descrição", r.descricao);
        if (r.peso > 0) linha(a, v, "Peso", Datas.kg(r.peso));
        if (!r.tipo.equals("peso")) linha(a, v, "Valor cobrado", r.valor > 0 ? Dados.reais(r.valor) + (r.plano ? " · coberto pelo plano" : "") : "Sem custo");
        if (!r.proxima.isEmpty()) linha(a, v, r.tipo.equals("vacina") ? "Próxima dose" : "Próxima data", Datas.br(r.proxima) + " (" + Datas.relativo(r.proxima) + ")");
        ScrollView sv = new ScrollView(a);
        sv.addView(v);
        AlertDialog d = new AlertDialog.Builder(a).setView(sv).setPositiveButton("Fechar", null).create();
        lapis.setOnClickListener(z -> {
            d.dismiss();
            a.startActivity(new Intent(a, RegistroActivity.class).putExtra("registro", r.id));
        });
        x.setOnClickListener(z -> new AlertDialog.Builder(a).setTitle("Excluir este registro?").setMessage(r.resumo() + " · " + Datas.br(r.data))
                .setPositiveButton("Excluir", (q, w) -> {
                    Dados.removerRegistro(a, r.id);
                    Avisos.agendar(a);
                    d.dismiss();
                    if (aoMudar != null) aoMudar.run();
                }).setNegativeButton("Cancelar", null).show());
        d.show();
    }

    private static void linha(Activity a, LinearLayout v, String r, String val) {
        if (val == null || val.isEmpty()) return;
        Ui.Cores k = Ui.cores(a);
        TextView t = Ui.texto(a, r, 12, k.sub, true);
        t.setPadding(0, dp(a, 12), 0, dp(a, 2));
        v.addView(t);
        TextView b = Ui.texto(a, val, 16, k.texto, false);
        b.setTextIsSelectable(true);
        v.addView(b);
    }

    // ------------------------------------------------------------------ lembretes

    /** linha de lembrete com ✓ (feito) — mostrarPet coloca o focinho e o nome do dog */
    public static View itemLembrete(Activity a, Dados.Lembrete m, boolean mostrarPet, Runnable aoMudar) {
        Ui.Cores k = Ui.cores(a);
        LinearLayout c = Ui.linha(a);
        c.setBackground(Ui.clicavel(k.superficie, dp(a, 18), 0x22000000));
        c.setPadding(dp(a, 12), dp(a, 12), dp(a, 10), dp(a, 12));
        int d = Datas.diasAte(m.data);
        Dados.Pet p = Dados.pet(a, m.petId);
        if (mostrarPet && p != null) c.addView(Fotos.avatar(a, p, dp(a, 40)));
        else c.addView(Ui.texto(a, m.feito ? "✅" : d < 0 ? "⏰" : "🔔", 22, 0, false));
        LinearLayout t = Ui.coluna(a);
        t.setPadding(dp(a, 12), 0, dp(a, 8), 0);
        TextView ti = Ui.texto(a, (mostrarPet && p != null ? p.nome + " · " : "") + m.titulo, 15, m.feito ? k.sub : k.texto, true);
        ti.setMaxLines(2);
        ti.setEllipsize(TextUtils.TruncateAt.END);
        if (m.feito) ti.setPaintFlags(ti.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        t.addView(ti);
        String rep = m.repetirDias == 0 ? "" : " · 🔁 " + (m.repetirDias == 1 ? "todo dia" : m.repetirDias == 365 ? "todo ano" : "a cada " + m.repetirDias + " dias");
        String quando = m.feito ? "Concluído" : d < 0 ? "Atrasado · " + Datas.relativo(m.data) : Datas.relativo(m.data);
        t.addView(Ui.texto(a, Datas.br(m.data) + " às " + Campos.hora(m.hora, m.minuto) + " · " + quando + rep, 12,
                !m.feito && d < 0 ? 0xFFEF4444 : k.sub, !m.feito && d < 0));
        c.addView(t, Ui.lp(0, -2, 1f));
        if (!m.feito) {
            TextView ok = Ui.texto(a, "✓", 20, k.destaque, true);
            ok.setGravity(Gravity.CENTER);
            ok.setBackground(Ui.clicavel(k.chipSel, dp(a, 18), 0x22000000));
            ok.setOnClickListener(v -> {
                Avisos.marcarFeito(a, m);
                Toast.makeText(a, m.repetirDias > 0 ? "Feito! Próximo em " + Datas.br(m.data) : "Feito ✅", Toast.LENGTH_SHORT).show();
                if (aoMudar != null) aoMudar.run();
            });
            c.addView(ok, Ui.lp(dp(a, 42), dp(a, 42)));
        }
        c.setOnClickListener(v -> lembrete(a, m, m.petId, aoMudar));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(a, 10);
        c.setLayoutParams(lp);
        return c;
    }

    /** novo lembrete sem pet definido: pergunta de qual dog */
    public static void novoLembrete(Activity a, long petId, Runnable aoMudar) {
        if (petId != 0) {
            lembrete(a, null, petId, aoMudar);
            return;
        }
        List<Dados.Pet> ps = Dados.pets(a);
        if (ps.isEmpty()) {
            Toast.makeText(a, "Cadastre um pet primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ps.size() == 1) {
            lembrete(a, null, ps.get(0).id, aoMudar);
            return;
        }
        String[] n = new String[ps.size()];
        for (int i = 0; i < n.length; i++) n[i] = "🐶  " + ps.get(i).nome;
        new AlertDialog.Builder(a).setTitle("Lembrete para qual dog?").setItems(n, (d, w) -> lembrete(a, null, ps.get(w).id, aoMudar)).show();
    }

    public static void lembrete(Activity a, Dados.Lembrete orig, long petId, Runnable aoMudar) {
        Ui.Cores k = Ui.cores(a);
        boolean novo = orig == null;
        Dados.Lembrete m = novo ? new Dados.Lembrete() : orig;
        if (novo) {
            m.petId = petId;
            m.data = Datas.somarDias(Datas.hoje(), 1);
        }
        Dados.Pet pet = Dados.pet(a, m.petId);
        ScrollView sv = new ScrollView(a);
        LinearLayout v = Ui.coluna(a);
        v.setPadding(dp(a, 20), dp(a, 6), dp(a, 20), dp(a, 6));
        sv.addView(v);
        EditText ti = Campos.texto(a, v, "Lembrar de", m.titulo, "Ex.: Dar vermífugo", false);
        ti.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        String[][] pre = {{"💊 Vermífugo", "Dar vermífugo", "90"}, {"🦟 Antipulgas", "Aplicar antipulgas", "30"}, {"🛁 Banho", "Banho", "15"},
                {"💉 Vacina", "Vacina", "365"}, {"💊 Remédio", "Dar remédio", "0"}, {"🩺 Consulta", "Consulta no veterinário", "0"}, {"🦷 Dentes", "Escovar os dentes", "7"}};
        LinearLayout pl = Ui.linha(a);
        pl.setPadding(0, dp(a, 8), 0, 0);
        final int[] rep = {m.repetirDias};
        String[] reps = {"0", "1", "7", "15", "30", "90", "365"};
        String[] repN = {"Não repete", "Todo dia", "7 dias", "15 dias", "30 dias", "3 meses", "1 ano"};
        Ui.Chips chipsRep = new Ui.Chips(a, reps, repN, String.valueOf(m.repetirDias), id -> rep[0] = Integer.parseInt(id));
        for (String[] p : pre) {
            TextView c = Ui.texto(a, p[0], 13, k.texto, false);
            c.setPadding(dp(a, 12), dp(a, 7), dp(a, 12), dp(a, 7));
            c.setBackground(Ui.clicavel(k.chip, dp(a, 14), 0x22000000));
            c.setOnClickListener(z -> {
                ti.setText(p[1]);
                rep[0] = Integer.parseInt(p[2]);
                chipsRep.selecionar(p[2]);
            });
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = dp(a, 6);
            pl.addView(c, lp);
        }
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(pl);
        v.addView(hs);
        Campos.Data dt = Campos.Data.criar(a, v, "Dia", m.data, false, "Escolha");
        Campos.rotulo(a, v, "Horário", false);
        final int[] hm = {m.hora, m.minuto};
        TextView hora = Ui.texto(a, "🕘  " + Campos.hora(m.hora, m.minuto), 16, k.texto, false);
        hora.setBackground(Campos.caixa(a));
        hora.setPadding(dp(a, 14), dp(a, 13), dp(a, 14), dp(a, 13));
        hora.setOnClickListener(z -> Campos.escolherHora(a, hm[0], hm[1], (tp, h, mi) -> {
            hm[0] = h;
            hm[1] = mi;
            hora.setText("🕘  " + Campos.hora(h, mi));
        }));
        v.addView(hora);
        Campos.rotulo(a, v, "Repetir", false);
        v.addView(chipsRep.view);

        String titulo = (novo ? "Novo lembrete" : "Editar lembrete") + (pet != null ? " · " + pet.nome : "");
        AlertDialog.Builder b = new AlertDialog.Builder(a).setTitle(titulo).setView(sv)
                .setPositiveButton("Salvar", (d, w) -> {
                    String t = ti.getText().toString().trim();
                    if (t.isEmpty() || dt.valor().isEmpty()) {
                        Toast.makeText(a, "Preencha o que lembrar e o dia", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    m.titulo = t;
                    m.data = dt.valor();
                    m.hora = hm[0];
                    m.minuto = hm[1];
                    m.repetirDias = rep[0];
                    m.feito = false;
                    Dados.salvarLembrete(a, m);
                    Avisos.agendar(a);
                    if (aoMudar != null) aoMudar.run();
                }).setNegativeButton("Cancelar", null);
        if (!novo) b.setNeutralButton("Excluir", (d, w) -> {
            Dados.removerLembrete(a, m.id);
            Avisos.agendar(a);
            if (aoMudar != null) aoMudar.run();
        });
        b.show();
    }

    // ------------------------------------------------------------------ filtro de focinhos

    public interface AoEscolherPet {
        void escolheu(long petId);
    }

    /** fileira de focinhos para filtrar (0 = todos) */
    public static View filtroPets(Activity a, long selecionado, AoEscolherPet cb) {
        Ui.Cores k = Ui.cores(a);
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout l = Ui.linha(a);
        l.setPadding(0, dp(a, 12), 0, dp(a, 4));
        hs.addView(l);
        List<Dados.Pet> ps = Dados.pets(a);
        int tam = dp(a, 56);
        for (int i = -1; i < ps.size(); i++) {
            Dados.Pet p = i < 0 ? null : ps.get(i);
            long id = p == null ? 0 : p.id;
            boolean on = id == selecionado;
            LinearLayout it = Ui.coluna(a);
            it.setGravity(Gravity.CENTER_HORIZONTAL);
            android.widget.FrameLayout anel = new android.widget.FrameLayout(a);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            if (on) g.setColors(Ui.GRADIENTE);
            else g.setColor(k.linha);
            anel.setBackground(g);
            int pad = dp(a, on ? 3 : 1.5f);
            anel.setPadding(pad, pad, pad, pad);
            View av;
            if (p == null) {
                TextView t = Ui.texto(a, "🐾", 24, 0, false);
                t.setGravity(Gravity.CENTER);
                android.graphics.drawable.GradientDrawable f = new android.graphics.drawable.GradientDrawable();
                f.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                f.setColor(k.superficie);
                t.setBackground(f);
                av = t;
            } else av = Fotos.avatar(a, p, tam - 2 * pad);
            anel.addView(av, new android.widget.FrameLayout.LayoutParams(-1, -1));
            it.addView(anel, Ui.lp(tam, tam));
            TextView n = Ui.texto(a, p == null ? "Todos" : p.nome, 12, on ? k.texto : k.sub, on);
            n.setMaxLines(1);
            n.setEllipsize(TextUtils.TruncateAt.END);
            n.setGravity(Gravity.CENTER);
            n.setPadding(0, dp(a, 4), 0, 0);
            it.addView(n, Ui.lp(dp(a, 66), -2));
            it.setOnClickListener(v -> cb.escolheu(id));
            l.addView(it);
        }
        return hs;
    }
}
