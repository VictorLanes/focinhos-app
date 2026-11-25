package com.victorlanes.focinhos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Telas de todos os dogs juntos: gastos completos, lembretes e vacinas — com busca e filtro por focinho. */
public class ListaActivity extends Activity {

    private Ui.Cores k;
    private String modo;
    private long petFiltro;
    private String busca = "";
    private String periodo = "mes", categoria = "todas", situacao = "pendentes";
    private LinearLayout filtros, corpo;
    private ScrollView rolagem;

    @Override
    protected void onCreate(Bundle b) {
        Visual.preparar(this, false);
        super.onCreate(b);
        k = Ui.cores(this);
        modo = getIntent().getStringExtra("modo");
        if (modo == null) modo = "gastos";
        petFiltro = getIntent().getLongExtra("pet", 0);
        String per = getIntent().getStringExtra("periodo");
        if (per != null) periodo = per;
        if (b != null) {
            petFiltro = b.getLong("pet", petFiltro);
            periodo = b.getString("periodo", periodo);
            categoria = b.getString("categoria", categoria);
            situacao = b.getString("situacao", situacao);
        }
        montar();
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        o.putLong("pet", petFiltro);
        o.putString("periodo", periodo);
        o.putString("categoria", categoria);
        o.putString("situacao", situacao);
    }

    private boolean voltou;

    @Override
    protected void onResume() {
        super.onResume();
        if (voltou) pintar(); // voltou de uma edição
        voltou = true;
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void montar() {
        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(k.fundo);
        setContentView(raiz);
        rolagem = new ScrollView(this);
        LinearLayout col = Ui.coluna(this);
        col.setPadding(dp(16), dp(8), dp(16), dp(110));
        rolagem.addView(col);
        raiz.addView(rolagem, new FrameLayout.LayoutParams(-1, -1));
        Ui.aplicarInsets(raiz);

        LinearLayout topo = Ui.linha(this);
        TextView voltar = Ui.texto(this, "←", 26, k.texto, false);
        voltar.setPadding(dp(4), dp(4), dp(14), dp(4));
        voltar.setOnClickListener(v -> finish());
        topo.addView(voltar);
        String t = modo.equals("lembretes") ? "🔔 Lembretes" : modo.equals("vacinas") ? "💉 Vacinas" : "💰 Gastos";
        topo.addView(Ui.texto(this, t, 24, k.texto, true), Ui.lp(0, -2, 1f));
        if (modo.equals("gastos")) {
            TextView csv = Ui.texto(this, "📤", 22, 0, false);
            csv.setPadding(dp(10), dp(4), dp(4), dp(4));
            csv.setOnClickListener(v -> exportarCsv());
            topo.addView(csv);
        }
        col.addView(topo);

        EditText e = new EditText(this);
        Campos.estilo(this, e);
        e.setHint(modo.equals("gastos") ? "🔎  Buscar: vacina, clínica, descrição…" : modo.equals("vacinas") ? "🔎  Buscar vacina" : "🔎  Buscar lembrete");
        e.setSingleLine(true);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                busca = s.toString().trim().toLowerCase();
                pintarCorpo();
            }
        });
        LinearLayout.LayoutParams el = Ui.lp(-1, -2);
        el.topMargin = dp(10);
        col.addView(e, el);

        filtros = Ui.coluna(this);
        col.addView(filtros);
        corpo = Ui.coluna(this);
        col.addView(corpo);
        pintar();

        if (!modo.equals("vacinas")) {
            TextView fab = Ui.texto(this, "＋", 30, 0xFFFFFFFF, true);
            fab.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, Ui.GRADIENTE);
            g.setCornerRadius(dp(22));
            fab.setBackground(g);
            fab.setElevation(dp(6));
            fab.setOnClickListener(v -> {
                if (modo.equals("lembretes")) Detalhes.novoLembrete(this, petFiltro, this::pintar);
                else novoGasto();
            });
            FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM | Gravity.END);
            fl.setMargins(0, 0, dp(20), dp(24));
            raiz.addView(fab, fl);
        }
    }

    private void novoGasto() {
        List<Dados.Pet> ps = Dados.pets(this);
        if (ps.isEmpty()) return;
        Runnable abrir = () -> {
        };
        if (petFiltro != 0 || ps.size() == 1) {
            long id = petFiltro != 0 ? petFiltro : ps.get(0).id;
            escolherTipo(id);
            return;
        }
        String[] n = new String[ps.size()];
        for (int i = 0; i < n.length; i++) n[i] = "🐶  " + ps.get(i).nome;
        new android.app.AlertDialog.Builder(this).setTitle("Gasto de qual dog?").setItems(n, (d, w) -> escolherTipo(ps.get(w).id)).show();
    }

    private void escolherTipo(long petId) {
        String[] op = new String[Dados.TIPOS.length - 1];
        List<String> ids = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < Dados.TIPOS.length; i++) {
            if (Dados.TIPOS[i].equals("peso")) continue;
            op[j++] = Dados.EMOJI_TIPOS[i] + "  " + Dados.NOMES_TIPOS[i];
            ids.add(Dados.TIPOS[i]);
        }
        new android.app.AlertDialog.Builder(this).setTitle("Tipo do gasto").setItems(op, (d, w) ->
                startActivity(new Intent(this, RegistroActivity.class).putExtra("pet", petId).putExtra("tipo", ids.get(w)))).show();
    }

    /** redesenha filtros e lista */
    private void pintar() {
        filtros.removeAllViews();
        filtros.addView(Detalhes.filtroPets(this, petFiltro, id -> {
            petFiltro = id;
            pintar();
        }));
        if (modo.equals("gastos")) {
            filtros.addView(new Ui.Chips(this, new String[]{"mes", "passado", "3m", "ano", "anoPassado", "tudo"},
                    new String[]{"Este mês", "Mês passado", "Últimos 3 meses", "Este ano", "Ano passado", "Tudo"}, periodo, id -> {
                periodo = id;
                pintarCorpo();
            }).view);
            String[] ids = new String[Dados.TIPOS.length], ns = new String[Dados.TIPOS.length];
            ids[0] = "todas";
            ns[0] = "Todas as categorias";
            int j = 1;
            for (int i = 0; i < Dados.TIPOS.length; i++) {
                if (Dados.TIPOS[i].equals("peso")) continue;
                ids[j] = Dados.TIPOS[i];
                ns[j++] = Dados.EMOJI_TIPOS[i] + " " + Dados.NOMES_TIPOS[i];
            }
            filtros.addView(new Ui.Chips(this, ids, ns, categoria, id -> {
                categoria = id;
                pintarCorpo();
            }).view);
        } else if (modo.equals("lembretes")) {
            filtros.addView(new Ui.Chips(this, new String[]{"pendentes", "atrasados", "semana", "concluidos", "todos"},
                    new String[]{"Pendentes", "⏰ Atrasados", "Próximos 7 dias", "✅ Concluídos", "Todos"}, situacao, id -> {
                situacao = id;
                pintarCorpo();
            }).view);
        } else {
            filtros.addView(new Ui.Chips(this, new String[]{"todas", "atrasadas", "breve", "emdia"},
                    new String[]{"Todas", "⚠️ Atrasadas", "Vencem em 30 dias", "✅ Em dia"}, situacao.equals("pendentes") ? "todas" : situacao, id -> {
                situacao = id;
                pintarCorpo();
            }).view);
        }
        pintarCorpo();
    }

    private void pintarCorpo() {
        corpo.removeAllViews();
        if (modo.equals("lembretes")) lembretes();
        else if (modo.equals("vacinas")) vacinas();
        else gastos();
    }

    private TextView vazio(String s) {
        TextView t = Ui.texto(this, s, 15, k.sub, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(20), dp(40), dp(20), dp(20));
        return t;
    }

    private String nomePet(long id) {
        Dados.Pet p = Dados.pet(this, id);
        return p == null ? "" : p.nome;
    }

    // ================================================================== GASTOS

    /** intervalo do período escolhido: {inicio, fim} em yyyy-MM-dd (inclusive) */
    private String[] intervalo() {
        Calendar h = Calendar.getInstance();
        int ano = h.get(Calendar.YEAR), mes = h.get(Calendar.MONTH);
        switch (periodo) {
            case "mes":
                return new String[]{Datas.iso(ano, mes, 1), Datas.iso(ano, mes, 31)};
            case "passado": {
                Calendar p = Calendar.getInstance();
                p.add(Calendar.MONTH, -1);
                return new String[]{Datas.iso(p.get(Calendar.YEAR), p.get(Calendar.MONTH), 1), Datas.iso(p.get(Calendar.YEAR), p.get(Calendar.MONTH), 31)};
            }
            case "3m": {
                Calendar p = Calendar.getInstance();
                p.add(Calendar.MONTH, -2);
                return new String[]{Datas.iso(p.get(Calendar.YEAR), p.get(Calendar.MONTH), 1), Datas.iso(ano, mes, 31)};
            }
            case "ano":
                return new String[]{ano + "-01-01", ano + "-12-31"};
            case "anoPassado":
                return new String[]{(ano - 1) + "-01-01", (ano - 1) + "-12-31"};
            default:
                return new String[]{"0000-00-00", "9999-99-99"};
        }
    }

    private List<Dados.Registro> gastosFiltrados() {
        String[] iv = intervalo();
        List<Dados.Registro> l = new ArrayList<>();
        for (Dados.Registro r : Dados.registros(this, petFiltro)) {
            if (r.valor <= 0) continue;
            if (r.data.compareTo(iv[0]) < 0 || r.data.compareTo(iv[1]) > 0) continue;
            if (!categoria.equals("todas") && !r.tipo.equals(categoria)) continue;
            if (!busca.isEmpty()) {
                String t = (r.resumo() + " " + r.clinica + " " + r.descricao + " " + r.procedimento + " " + nomePet(r.petId)).toLowerCase();
                if (!t.contains(busca)) continue;
            }
            l.add(r);
        }
        return l;
    }

    private void gastos() {
        List<Dados.Registro> l = gastosFiltrados();
        if (l.isEmpty()) {
            corpo.addView(vazio("Nenhum gasto neste filtro 💰"));
            return;
        }
        long total = 0, plano = 0, maior = 0;
        Dados.Registro maiorR = null;
        long[] porTipo = new long[Dados.TIPOS.length];
        Map<Long, Long> porPet = new LinkedHashMap<>();
        Map<String, Long> porClinica = new LinkedHashMap<>();
        for (Dados.Registro r : l) {
            total += r.valor;
            if (r.plano) plano += r.valor;
            if (r.valor > maior) {
                maior = r.valor;
                maiorR = r;
            }
            porTipo[Dados.indiceTipo(r.tipo)] += r.valor;
            porPet.put(r.petId, (porPet.containsKey(r.petId) ? porPet.get(r.petId) : 0) + r.valor);
            if (!r.clinica.isEmpty()) porClinica.put(r.clinica, (porClinica.containsKey(r.clinica) ? porClinica.get(r.clinica) : 0) + r.valor);
        }

        // resumo
        LinearLayout c = Ui.coluna(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF2B1D14, 0xFF5A2E18});
        g.setCornerRadius(dp(22));
        c.setBackground(g);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cl = Ui.lp(-1, -2);
        cl.topMargin = dp(12);
        corpo.addView(c, cl);
        c.addView(Ui.texto(this, "Total" + (petFiltro != 0 ? " do " + nomePet(petFiltro) : " de todos os dogs"), 13, 0xCCFFFFFF, false));
        c.addView(Ui.texto(this, Dados.reais(total), 32, 0xFFFFFFFF, true));
        LinearLayout lin = Ui.linha(this);
        lin.setPadding(0, dp(10), 0, 0);
        mini(lin, "Lançamentos", String.valueOf(l.size()));
        mini(lin, "Média", Dados.reais(total / l.size()));
        mini(lin, "Pelo plano", Dados.reais(plano));
        c.addView(lin);
        if (maiorR != null)
            c.addView(Ui.texto(this, "Maior gasto: " + maiorR.resumo() + " · " + Dados.reais(maior) + (petFiltro == 0 ? " (" + nomePet(maiorR.petId) + ")" : ""), 12, 0xCCFFFFFF, false));

        // por dog
        if (petFiltro == 0 && porPet.size() > 1) {
            LinearLayout s = cartao("🐶  Por dog");
            long mx = 1;
            for (long v : porPet.values()) mx = Math.max(mx, v);
            for (Map.Entry<Long, Long> e : porPet.entrySet()) {
                long id = e.getKey();
                View b = barra(nomePet(id), e.getValue(), e.getValue() / (float) mx, k.destaque, Math.round(100f * e.getValue() / total) + "%");
                b.setOnClickListener(v -> {
                    petFiltro = id;
                    pintar();
                });
                s.addView(b);
            }
        }
        // por categoria
        LinearLayout s2 = cartao("📊  Por categoria");
        long mx = 1;
        for (long v : porTipo) mx = Math.max(mx, v);
        for (int i = 0; i < porTipo.length; i++) {
            if (porTipo[i] == 0) continue;
            String tipo = Dados.TIPOS[i];
            View b = barra(Dados.EMOJI_TIPOS[i] + " " + Dados.NOMES_TIPOS[i], porTipo[i], porTipo[i] / (float) mx, Dados.COR_TIPOS[i],
                    Math.round(100f * porTipo[i] / total) + "%");
            b.setOnClickListener(v -> {
                categoria = tipo;
                pintar();
            });
            s2.addView(b);
        }
        // por clínica
        if (!porClinica.isEmpty()) {
            LinearLayout s3 = cartao("🏥  Por clínica / pet shop");
            List<Map.Entry<String, Long>> es = new ArrayList<>(porClinica.entrySet());
            es.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            long m2 = es.get(0).getValue();
            for (int i = 0; i < Math.min(6, es.size()); i++)
                s3.addView(barra(es.get(i).getKey(), es.get(i).getValue(), es.get(i).getValue() / (float) m2, 0xFF8B5CF6, ""));
        }

        // lista completa
        TextView t = Ui.texto(this, "Todos os lançamentos (" + l.size() + ")", 17, k.texto, true);
        t.setPadding(dp(4), dp(20), 0, 0);
        corpo.addView(t);
        String mesAtual = "";
        for (Dados.Registro r : l) {
            String mes = r.data.substring(0, 7);
            if (!mes.equals(mesAtual)) {
                mesAtual = mes;
                long soma = 0;
                for (Dados.Registro x : l) if (x.data.startsWith(mes)) soma += x.valor;
                Calendar kk = Datas.cal(r.data);
                LinearLayout cab = Ui.linha(this);
                cab.setPadding(dp(4), dp(16), dp(4), dp(2));
                String nm = Datas.MESES_LONGOS[kk.get(Calendar.MONTH)];
                cab.addView(Ui.texto(this, nm.substring(0, 1).toUpperCase() + nm.substring(1) + " de " + kk.get(Calendar.YEAR), 13, k.destaque, true), Ui.lp(0, -2, 1f));
                cab.addView(Ui.texto(this, Dados.reais(soma), 13, k.destaque, true));
                corpo.addView(cab);
            }
            corpo.addView(itemGasto(r));
        }
    }

    private View itemGasto(Dados.Registro r) {
        LinearLayout c = Ui.linha(this);
        c.setGravity(Gravity.TOP);
        c.setBackground(Ui.clicavel(k.superficie, dp(18), 0x22000000));
        c.setPadding(dp(12), dp(12), dp(14), dp(12));
        Dados.Pet p = Dados.pet(this, r.petId);
        android.widget.FrameLayout av = new android.widget.FrameLayout(this);
        av.addView(Fotos.avatar(this, p, dp(42)));
        TextView em = Ui.texto(this, r.emoji(), 13, 0, false);
        em.setGravity(Gravity.CENTER);
        em.setBackground(Ui.fundo(k.superficie, dp(10)));
        android.widget.FrameLayout.LayoutParams ep = new android.widget.FrameLayout.LayoutParams(dp(22), dp(22), Gravity.BOTTOM | Gravity.END);
        av.addView(em, ep);
        c.addView(av, Ui.lp(dp(48), dp(48)));
        LinearLayout t = Ui.coluna(this);
        t.setPadding(dp(10), 0, dp(8), 0);
        TextView ti = Ui.texto(this, r.resumo(), 15, k.texto, true);
        ti.setMaxLines(2);
        t.addView(ti);
        t.addView(Ui.texto(this, Relatorios.juntar(" · ", p != null ? p.nome : "", Datas.br(r.data), r.clinica), 12, k.sub, false));
        if (!r.procedimento.isEmpty()) t.addView(Ui.texto(this, "🏥 " + r.procedimento, 12, k.texto, false));
        if (!r.descricao.isEmpty()) {
            TextView d = Ui.texto(this, r.descricao, 12, k.sub, false);
            d.setMaxLines(3);
            d.setEllipsize(TextUtils.TruncateAt.END);
            t.addView(d);
        }
        c.addView(t, Ui.lp(0, -2, 1f));
        LinearLayout vv = Ui.coluna(this);
        vv.setGravity(Gravity.END);
        vv.addView(Ui.texto(this, Dados.reais(r.valor), 15, k.texto, true));
        if (r.plano) vv.addView(Ui.texto(this, "🛡️ plano", 11, 0xFF10B981, true));
        c.addView(vv);
        c.setOnClickListener(v -> Detalhes.registro(this, r, this::pintar));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout cartao(String titulo) {
        LinearLayout c = Ui.coluna(this);
        c.setBackground(Ui.fundo(k.superficie, dp(20)));
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(12);
        corpo.addView(c, lp);
        c.addView(Ui.texto(this, titulo, 16, k.texto, true));
        return c;
    }

    private void mini(LinearLayout l, String r, String v) {
        LinearLayout c = Ui.coluna(this);
        c.addView(Ui.texto(this, r, 11, 0xAAFFFFFF, false));
        c.addView(Ui.texto(this, v, 14, 0xFFFFFFFF, true));
        l.addView(c, Ui.lp(0, -2, 1f));
    }

    private View barra(String rot, long valor, float frac, int cor, String pct) {
        LinearLayout c = Ui.coluna(this);
        c.setPadding(0, dp(10), 0, dp(2));
        LinearLayout l = Ui.linha(this);
        TextView r = Ui.texto(this, rot, 13, k.texto, false);
        r.setMaxLines(1);
        r.setEllipsize(TextUtils.TruncateAt.END);
        l.addView(r, Ui.lp(0, -2, 1f));
        l.addView(Ui.texto(this, Dados.reais(valor) + (pct.isEmpty() ? "" : "  ·  " + pct), 13, k.sub, false));
        c.addView(l);
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Ui.fundo(k.linha, dp(5)));
        View b = new View(this);
        b.setBackground(Ui.fundo(cor, dp(5)));
        f.addView(b, new FrameLayout.LayoutParams(0, -1));
        f.post(() -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) b.getLayoutParams();
            lp.width = Math.max(dp(6), (int) (f.getWidth() * frac));
            b.setLayoutParams(lp);
        });
        LinearLayout.LayoutParams fl = Ui.lp(-1, dp(10));
        fl.topMargin = dp(4);
        c.addView(f, fl);
        return c;
    }

    private void exportarCsv() {
        List<Dados.Registro> l = gastosFiltrados();
        if (l.isEmpty()) {
            Toast.makeText(this, "Nenhum gasto para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder s = new StringBuilder("﻿Data;Dog;Tipo;Descrição;Clínica;Procedimento;Valor;Plano\n");
        for (Dados.Registro r : l) {
            s.append(Datas.br(r.data)).append(';').append(csv(nomePet(r.petId))).append(';').append(r.nomeTipo()).append(';')
                    .append(csv(Relatorios.juntar(" - ", r.titulo, r.descricao))).append(';').append(csv(r.clinica)).append(';').append(csv(r.procedimento))
                    .append(';').append(String.format(new Locale("pt", "BR"), "%.2f", r.valor / 100.0)).append(';').append(r.plano ? "Sim" : "Não").append('\n');
        }
        try {
            File f = new File(Arquivos.pasta(this), "Gastos_AnjosDeFocinho.csv");
            try (FileOutputStream o = new FileOutputStream(f)) {
                o.write(s.toString().getBytes(StandardCharsets.UTF_8));
            }
            android.net.Uri u = Arquivos.uri(f);
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setClipData(android.content.ClipData.newRawUri(f.getName(), u));
            startActivity(Intent.createChooser(i, "Exportar gastos (abre no Excel / Planilhas)"));
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui exportar", Toast.LENGTH_SHORT).show();
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\"", "'");
        return s.contains(";") ? "\"" + s + "\"" : s;
    }

    // ================================================================== LEMBRETES

    private void lembretes() {
        int n = 0;
        boolean feitosTitulo = false;
        for (Dados.Lembrete m : Dados.lembretes(this, petFiltro)) {
            if (Dados.pet(this, m.petId) == null) continue;
            int d = Datas.diasAte(m.data);
            boolean ok;
            switch (situacao) {
                case "atrasados":
                    ok = !m.feito && d < 0;
                    break;
                case "semana":
                    ok = !m.feito && d >= 0 && d <= 7;
                    break;
                case "concluidos":
                    ok = m.feito;
                    break;
                case "todos":
                    ok = true;
                    break;
                default:
                    ok = !m.feito;
            }
            if (!ok) continue;
            if (!busca.isEmpty() && !(m.titulo + " " + nomePet(m.petId)).toLowerCase().contains(busca)) continue;
            if (m.feito && !feitosTitulo && situacao.equals("todos")) {
                TextView t = Ui.texto(this, "Concluídos", 15, k.sub, true);
                t.setPadding(dp(4), dp(18), 0, 0);
                corpo.addView(t);
                feitosTitulo = true;
            }
            corpo.addView(Detalhes.itemLembrete(this, m, true, this::pintarCorpo));
            n++;
        }
        if (n == 0) corpo.addView(vazio(situacao.equals("atrasados") ? "Nada atrasado 🎉" : "Nenhum lembrete aqui.\nToque em ＋ para criar 🔔"));
    }

    // ================================================================== VACINAS

    private void vacinas() {
        int n = 0;
        for (Dados.Pet p : Dados.pets(this)) {
            if (petFiltro != 0 && p.id != petFiltro) continue;
            List<Dados.Registro> vs = Relatorios.vacinasAtuais(this, p.id);
            LinearLayout grupo = null;
            for (Dados.Registro v : vs) {
                int d = v.proxima.isEmpty() ? 9999 : Datas.diasAte(v.proxima);
                boolean ok = situacao.equals("atrasadas") ? d < 0 : situacao.equals("breve") ? d >= 0 && d <= 30 : situacao.equals("emdia") ? d > 30 : true;
                if (!ok) continue;
                if (!busca.isEmpty() && !(v.titulo + " " + p.nome).toLowerCase().contains(busca)) continue;
                if (grupo == null) {
                    grupo = Ui.coluna(this);
                    LinearLayout cab = Ui.linha(this);
                    cab.setPadding(0, dp(18), 0, dp(4));
                    cab.addView(Fotos.avatar(this, p, dp(30)));
                    TextView nm = Ui.texto(this, "  " + p.nome, 16, k.texto, true);
                    cab.addView(nm, Ui.lp(0, -2, 1f));
                    TextView reg = Ui.texto(this, "+ Vacina", 13, k.destaque, true);
                    reg.setOnClickListener(z -> startActivity(new Intent(this, RegistroActivity.class).putExtra("pet", p.id).putExtra("tipo", "vacina")));
                    cab.addView(reg);
                    grupo.addView(cab);
                    corpo.addView(grupo);
                }
                grupo.addView(itemVacina(v, d));
                n++;
            }
        }
        if (n == 0) corpo.addView(vazio(situacao.equals("atrasadas") ? "Nenhuma vacina atrasada 🎉" : "Nenhuma vacina neste filtro 💉"));
    }

    private View itemVacina(Dados.Registro v, int d) {
        LinearLayout c = Ui.linha(this);
        c.setBackground(Ui.clicavel(k.superficie, dp(18), 0x22000000));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout t = Ui.coluna(this);
        t.addView(Ui.texto(this, "💉 " + (v.titulo.isEmpty() ? "Vacina" : v.titulo), 15, k.texto, true));
        t.addView(Ui.texto(this, "Aplicada em " + Datas.br(v.data) + (v.proxima.isEmpty() ? "" : " · próxima " + Datas.br(v.proxima)), 12, k.sub, false));
        c.addView(t, Ui.lp(0, -2, 1f));
        String st = v.proxima.isEmpty() ? "Sem reforço" : d < 0 ? "Atrasada" : d <= 30 ? Datas.relativo(v.proxima) : "Em dia";
        int cor = v.proxima.isEmpty() ? k.sub : d < 0 ? 0xFFEF4444 : d <= 30 ? 0xFFF59E0B : 0xFF10B981;
        TextView s = Ui.texto(this, st, 12, cor, true);
        s.setPadding(dp(10), dp(4), dp(10), dp(4));
        s.setBackground(Ui.fundo((cor & 0x00FFFFFF) | 0x22000000, dp(10)));
        c.addView(s);
        c.setOnClickListener(z -> Detalhes.registro(this, v, this::pintarCorpo));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);
        return c;
    }
}
