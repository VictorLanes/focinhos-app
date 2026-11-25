package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tela do pet: resumo, histórico, vacinas, saúde, gastos, lembretes e ficha. */
public class PetActivity extends Activity {

    private static final String[] ABAS = {"historico", "vacinas", "saude", "gastos", "lembretes", "ficha"};
    private static final String[] NOMES = {"📋 Histórico", "💉 Vacinas", "❤️ Saúde", "💰 Gastos", "🔔 Lembretes", "🪪 Ficha"};

    private Ui.Cores k;
    private Dados.Pet pet;
    private String aba = "historico";
    private String filtro = "todos";
    private LinearLayout corpo;
    private ScrollView rolagem;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Visual.preparar(this, true);
        k = Ui.cores(this);
        String a = getIntent().getStringExtra("aba");
        if (a != null) aba = a;
        if (b != null) aba = b.getString("aba", aba);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        String a = i.getStringExtra("aba");
        if (a != null) aba = a;
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        o.putString("aba", aba);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pet = Dados.pet(this, getIntent().getLongExtra("pet", 0));
        if (pet == null) {
            finish();
            return;
        }
        int y = rolagem != null ? rolagem.getScrollY() : 0;
        montar();
        rolagem.post(() -> rolagem.scrollTo(0, y));
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    // ================================================================== estrutura

    private void montar() {
        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(k.fundo);
        setContentView(raiz);
        rolagem = new ScrollView(this);
        LinearLayout col = Ui.coluna(this);
        col.setPadding(0, 0, 0, dp(110));
        rolagem.addView(col);
        raiz.addView(rolagem, new FrameLayout.LayoutParams(-1, -1));
        Ui.aplicarInsets(raiz);

        col.addView(cabecalho());
        LinearLayout pad = Ui.coluna(this);
        pad.setPadding(dp(14), 0, dp(14), 0);
        col.addView(pad);
        pad.addView(painel());
        Ui.Chips abas = new Ui.Chips(this, ABAS, NOMES, aba, id -> {
            aba = id;
            pintarAba();
        });
        abas.view.setPadding(0, dp(6), 0, 0);
        pad.addView(abas.view);
        corpo = Ui.coluna(this);
        pad.addView(corpo);
        pintarAba();

        TextView fab = Ui.texto(this, "＋", 30, 0xFFFFFFFF, true);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, Ui.GRADIENTE);
        g.setCornerRadius(dp(22));
        fab.setBackground(g);
        fab.setElevation(dp(6));
        fab.setOnClickListener(v -> acaoMais());
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM | Gravity.END);
        fl.setMargins(0, 0, dp(20), dp(24));
        raiz.addView(fab, fl);
    }

    private void acaoMais() {
        switch (aba) {
            case "vacinas":
                novoRegistro("vacina");
                break;
            case "lembretes":
                editarLembrete(null);
                break;
            case "saude":
                novoRegistro("peso");
                break;
            case "ficha":
                startActivity(new Intent(this, PetFormActivity.class).putExtra("pet", pet.id));
                break;
            default:
                String[] op = new String[Dados.TIPOS.length];
                for (int i = 0; i < op.length; i++) op[i] = Dados.EMOJI_TIPOS[i] + "  " + Dados.NOMES_TIPOS[i];
                new AlertDialog.Builder(this).setTitle("Adicionar ao histórico").setItems(op, (d, w) -> novoRegistro(Dados.TIPOS[w])).show();
        }
    }

    private void novoRegistro(String tipo) {
        startActivity(new Intent(this, RegistroActivity.class).putExtra("pet", pet.id).putExtra("tipo", tipo));
    }

    private View cabecalho() {
        LinearLayout h = Ui.coluna(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFFF9A5C, 0xFFFF6A8E});
        g.setCornerRadii(new float[]{0, 0, 0, 0, dp(32), dp(32), dp(32), dp(32)});
        h.setBackground(g);
        h.setPadding(dp(10), dp(6), dp(10), dp(22));

        LinearLayout barra = Ui.linha(this);
        TextView voltar = Ui.texto(this, "←", 26, 0xFFFFFFFF, false);
        voltar.setPadding(dp(10), dp(6), dp(10), dp(6));
        voltar.setOnClickListener(v -> finish());
        barra.addView(voltar);
        barra.addView(new View(this), Ui.lp(0, 1, 1f));
        TextView qrb = Ui.texto(this, "▦", 24, 0xFFFFFFFF, true);
        qrb.setPadding(dp(10), dp(4), dp(10), dp(6));
        qrb.setOnClickListener(v -> qrColeira());
        barra.addView(qrb);
        TextView comp = Ui.texto(this, "📤", 22, 0xFFFFFFFF, false);
        comp.setPadding(dp(10), dp(6), dp(10), dp(6));
        comp.setOnClickListener(v -> compartilhar());
        barra.addView(comp);
        TextView ed = Ui.texto(this, "✏️", 22, 0xFFFFFFFF, false);
        ed.setPadding(dp(10), dp(6), dp(10), dp(6));
        ed.setOnClickListener(v -> startActivity(new Intent(this, PetFormActivity.class).putExtra("pet", pet.id)));
        barra.addView(ed);
        h.addView(barra);

        LinearLayout l = Ui.linha(this);
        l.setPadding(dp(10), dp(4), dp(10), 0);
        FrameLayout borda = new FrameLayout(this);
        GradientDrawable bo = new GradientDrawable();
        bo.setShape(GradientDrawable.OVAL);
        bo.setColor(0xFFFFFFFF);
        borda.setBackground(bo);
        borda.setPadding(dp(3), dp(3), dp(3), dp(3));
        borda.addView(Fotos.avatar(this, pet, dp(92)), new FrameLayout.LayoutParams(dp(92), dp(92)));
        l.addView(borda);
        LinearLayout tx = Ui.coluna(this);
        tx.setPadding(dp(16), 0, 0, 0);
        TextView nm = Ui.texto(this, pet.nome, 28, 0xFFFFFFFF, true);
        nm.setMaxLines(1);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        tx.addView(nm);
        String sub = Relatorios.juntar(" · ", pet.raca, pet.sexo.isEmpty() ? "" : (pet.sexo.equals("Macho") ? "♂ " : "♀ ") + pet.sexo, pet.idadeTexto());
        tx.addView(Ui.texto(this, sub, 14, 0xEEFFFFFF, false));
        LinearLayout tags = Ui.linha(this);
        tags.setPadding(0, dp(8), 0, 0);
        if (!pet.plano.isEmpty()) tag(tags, "🛡️ " + Relatorios.juntar(" ", pet.plano, pet.categoriaPlano));
        if (pet.castrado) tag(tags, "✂️ Castrado");
        if (!pet.nascimento.isEmpty()) {
            int d = Datas.diasAte(Datas.proximoAniversario(pet.nascimento));
            if (d <= 30) tag(tags, d == 0 ? "🎂 Hoje é o aniversário!" : "🎂 em " + d + " dias");
        }
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(tags);
        tx.addView(hs);
        l.addView(tx, Ui.lp(0, -2, 1f));
        h.addView(l);
        return h;
    }

    private void tag(LinearLayout l, String s) {
        TextView t = Ui.texto(this, s, 12, 0xFFFFFFFF, true);
        t.setPadding(dp(10), dp(4), dp(10), dp(4));
        t.setBackground(Ui.fundo(0x33FFFFFF, dp(12)));
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = dp(6);
        l.addView(t, lp);
    }

    /** 4 informações rápidas */
    private View painel() {
        List<Dados.Registro> rs = Dados.registros(this, pet.id);
        Dados.Registro vet = null, pesoR = null;
        long ano = 0;
        String anoAtual = Datas.hoje().substring(0, 4);
        for (Dados.Registro r : rs) {
            if (vet == null && r.foiAoVet() && Datas.diasAte(r.data) <= 0) vet = r;
            if (pesoR == null && r.peso > 0) pesoR = r;
            if (r.data.startsWith(anoAtual)) ano += r.valor;
        }
        String prox = "";
        for (Dados.Registro v : Relatorios.vacinasAtuais(this, pet.id))
            if (!v.proxima.isEmpty() && (prox.isEmpty() || v.proxima.compareTo(prox) < 0)) prox = v.proxima;

        LinearLayout grade = Ui.coluna(this);
        grade.setPadding(0, dp(14), 0, 0);
        LinearLayout l1 = Ui.linha(this), l2 = Ui.linha(this);
        grade.addView(l1);
        LinearLayout.LayoutParams lp2 = Ui.lp(-1, -2);
        lp2.topMargin = dp(10);
        grade.addView(l2, lp2);
        l1.addView(info("🩺", "Última ida ao vet", vet == null ? "—" : Datas.br(vet.data), vet == null ? "Nenhum registro" : Datas.relativo(vet.data), 0, v -> {
            aba = "historico";
            filtro = "todos";
            pintarAba();
        }), Ui.lp(0, -2, 1f));
        int corVac = prox.isEmpty() ? 0 : Datas.diasAte(prox) < 0 ? 0xFFEF4444 : Datas.diasAte(prox) <= 15 ? 0xFFF59E0B : 0xFF10B981;
        espaco(l1);
        l1.addView(info("💉", "Próxima vacina", prox.isEmpty() ? "—" : Datas.br(prox), prox.isEmpty() ? "Sem dose marcada" :
                (Datas.diasAte(prox) < 0 ? "Atrasada " + Datas.relativo(prox).replace("há ", "há ") : Datas.relativo(prox)), corVac, v -> {
            aba = "vacinas";
            pintarAba();
        }), Ui.lp(0, -2, 1f));
        l2.addView(info("⚖️", "Peso atual", pesoR == null ? "—" : Datas.kg(pesoR.peso), pesoR == null ? "Toque para pesar" : "em " + Datas.br(pesoR.data), 0, v -> {
            aba = "saude";
            pintarAba();
        }), Ui.lp(0, -2, 1f));
        espaco(l2);
        l2.addView(info("💰", "Gastos em " + anoAtual, Dados.reais(ano), rs.isEmpty() ? "Nada registrado" : "Ver detalhes", 0, v -> {
            aba = "gastos";
            pintarAba();
        }), Ui.lp(0, -2, 1f));
        return grade;
    }

    private void espaco(LinearLayout l) {
        l.addView(new View(this), Ui.lp(dp(10), 1));
    }

    private View info(String emoji, String rot, String valor, String sub, int corSub, View.OnClickListener cl) {
        LinearLayout c = Ui.coluna(this);
        c.setBackground(Ui.clicavel(k.superficie, dp(20), 0x22000000));
        c.setPadding(dp(14), dp(12), dp(12), dp(12));
        c.setElevation(dp(1));
        c.addView(Ui.texto(this, emoji + "  " + rot, 12, k.sub, true));
        TextView v = Ui.texto(this, valor, 19, k.texto, true);
        v.setPadding(0, dp(4), 0, 0);
        v.setMaxLines(1);
        v.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(v);
        TextView s = Ui.texto(this, sub, 12, corSub != 0 ? corSub : k.sub, corSub != 0);
        s.setMaxLines(1);
        s.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(s);
        c.setOnClickListener(cl);
        return c;
    }

    private void pintarAba() {
        corpo.removeAllViews();
        switch (aba) {
            case "vacinas":
                abaVacinas();
                break;
            case "saude":
                abaSaude();
                break;
            case "gastos":
                abaGastos();
                break;
            case "lembretes":
                abaLembretes();
                break;
            case "ficha":
                abaFicha();
                break;
            default:
                abaHistorico();
        }
    }

    private TextView vazio(String s) {
        TextView t = Ui.texto(this, s, 15, k.sub, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(20), dp(40), dp(20), dp(20));
        return t;
    }

    private LinearLayout cartao() {
        LinearLayout c = Ui.coluna(this);
        c.setBackground(Ui.fundo(k.superficie, dp(20)));
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView titulo(String s) {
        TextView t = Ui.texto(this, s, 17, k.texto, true);
        t.setPadding(dp(4), dp(18), 0, 0);
        return t;
    }

    // ================================================================== HISTÓRICO

    private void abaHistorico() {
        List<Dados.Registro> todos = Dados.registros(this, pet.id);
        if (todos.isEmpty()) {
            corpo.addView(vazio("Nenhum registro ainda.\nToque em ＋ para adicionar a primeira consulta, vacina ou banho do " + pet.nome + " 🐾"));
            return;
        }
        List<String> ids = new ArrayList<>(), nomes = new ArrayList<>();
        ids.add("todos");
        nomes.add("Todos (" + todos.size() + ")");
        for (int i = 0; i < Dados.TIPOS.length; i++) {
            int n = 0;
            for (Dados.Registro r : todos) if (r.tipo.equals(Dados.TIPOS[i])) n++;
            if (n > 0) {
                ids.add(Dados.TIPOS[i]);
                nomes.add(Dados.NOMES_TIPOS[i] + " (" + n + ")");
            }
        }
        if (!ids.contains(filtro)) filtro = "todos";
        LinearLayout lista = Ui.coluna(this);
        Ui.Chips f = new Ui.Chips(this, ids.toArray(new String[0]), nomes.toArray(new String[0]), filtro, id -> {
            filtro = id;
            pintarLinhaDoTempo(lista, todos);
        });
        corpo.addView(f.view);
        corpo.addView(lista);
        pintarLinhaDoTempo(lista, todos);
    }

    private void pintarLinhaDoTempo(LinearLayout lista, List<Dados.Registro> todos) {
        lista.removeAllViews();
        String mesAtual = "";
        for (Dados.Registro r : todos) {
            if (!filtro.equals("todos") && !r.tipo.equals(filtro)) continue;
            String mes = r.data.length() >= 7 ? r.data.substring(0, 7) : "";
            if (!mes.equals(mesAtual)) {
                mesAtual = mes;
                Calendar k2 = Datas.cal(r.data);
                if (k2 != null) {
                    TextView m = Ui.texto(this, Datas.MESES_LONGOS[k2.get(Calendar.MONTH)].substring(0, 1).toUpperCase() +
                            Datas.MESES_LONGOS[k2.get(Calendar.MONTH)].substring(1) + " de " + k2.get(Calendar.YEAR), 13, k.destaque, true);
                    m.setPadding(dp(4), dp(18), 0, dp(2));
                    lista.addView(m);
                }
            }
            lista.addView(itemTempo(r));
        }
    }

    private View itemTempo(Dados.Registro r) {
        LinearLayout l = Ui.linha(this);
        l.setGravity(Gravity.TOP);
        // marcador
        FrameLayout marca = new FrameLayout(this);
        View linhaV = new View(this);
        linhaV.setBackgroundColor(k.linha);
        marca.addView(linhaV, new FrameLayout.LayoutParams(dp(2), -1, Gravity.CENTER_HORIZONTAL));
        TextView em = Ui.texto(this, r.emoji(), 18, 0, false);
        em.setGravity(Gravity.CENTER);
        GradientDrawable bol = new GradientDrawable();
        bol.setShape(GradientDrawable.OVAL);
        bol.setColor((r.cor() & 0x00FFFFFF) | 0x33000000);
        bol.setStroke(dp(2), r.cor());
        em.setBackground(bol);
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        ep.topMargin = dp(12);
        marca.addView(em, ep);
        l.addView(marca, Ui.lp(dp(48), -1));

        LinearLayout c = Ui.coluna(this);
        c.setBackground(Ui.clicavel(k.superficie, dp(18), 0x22000000));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout top = Ui.linha(this);
        TextView t = Ui.texto(this, r.resumo(), 15, k.texto, true);
        top.addView(t, Ui.lp(0, -2, 1f));
        if (r.valor > 0) {
            TextView v = Ui.texto(this, Dados.reais(r.valor), 13, r.plano ? 0xFF10B981 : k.texto, true);
            v.setPadding(dp(8), 0, 0, 0);
            top.addView(v);
        }
        c.addView(top);
        String sub = Relatorios.juntar(" · ", Datas.curta(r.data), r.clinica, r.plano ? "🛡️ plano" : "");
        c.addView(Ui.texto(this, sub, 12, k.sub, false));
        if (!r.procedimento.isEmpty()) c.addView(Ui.texto(this, "🏥 Procedimento: " + r.procedimento, 13, k.texto, false));
        if (!r.descricao.isEmpty()) {
            TextView d = Ui.texto(this, r.descricao, 13, k.sub, false);
            d.setMaxLines(2);
            d.setEllipsize(TextUtils.TruncateAt.END);
            d.setPadding(0, dp(4), 0, 0);
            c.addView(d);
        }
        if (!r.proxima.isEmpty()) {
            int dd = Datas.diasAte(r.proxima);
            TextView p = Ui.texto(this, (r.tipo.equals("vacina") ? "⏭ Próxima dose: " : "⏭ Próxima: ") + Datas.br(r.proxima) + " (" + Datas.relativo(r.proxima) + ")",
                    12, dd < 0 ? 0xFFEF4444 : k.destaque, true);
            p.setPadding(0, dp(4), 0, 0);
            c.addView(p);
        }
        c.setOnClickListener(v -> detalhes(r));
        LinearLayout.LayoutParams cl = Ui.lp(0, -2, 1f);
        cl.topMargin = dp(8);
        l.addView(c, cl);
        return l;
    }

    /** detalhes do registro com lápis (editar) e X (excluir) */
    private void detalhes(Dados.Registro r) {
        Detalhes.registro(this, r, this::montar);
    }

    // ================================================================== VACINAS

    private void abaVacinas() {
        List<Dados.Registro> atuais = Relatorios.vacinasAtuais(this, pet.id);
        if (atuais.isEmpty()) {
            corpo.addView(vazio("Nenhuma vacina registrada.\nToque em ＋ para montar a carteira de vacinação do " + pet.nome + " 💉"));
            return;
        }
        corpo.addView(titulo("Carteira de vacinação"));
        for (Dados.Registro v : atuais) {
            LinearLayout c = cartao();
            c.setBackground(Ui.clicavel(k.superficie, dp(20), 0x22000000));
            LinearLayout l = Ui.linha(this);
            l.addView(Ui.texto(this, "💉  " + (v.titulo.isEmpty() ? "Vacina" : v.titulo), 16, k.texto, true), Ui.lp(0, -2, 1f));
            String st;
            int cor;
            if (v.proxima.isEmpty()) {
                st = "Sem reforço marcado";
                cor = k.sub;
            } else {
                int d = Datas.diasAte(v.proxima);
                st = d < 0 ? "Atrasada" : d <= 30 ? "Vence " + Datas.relativo(v.proxima) : "Em dia";
                cor = d < 0 ? 0xFFEF4444 : d <= 30 ? 0xFFF59E0B : 0xFF10B981;
            }
            TextView s = Ui.texto(this, st, 12, cor, true);
            s.setPadding(dp(10), dp(4), dp(10), dp(4));
            s.setBackground(Ui.fundo((cor & 0x00FFFFFF) | 0x22000000, dp(10)));
            l.addView(s);
            c.addView(l);
            c.addView(Ui.texto(this, "Aplicada em " + Datas.br(v.data) + (v.clinica.isEmpty() ? "" : " · " + v.clinica), 13, k.sub, false));
            if (!v.proxima.isEmpty())
                c.addView(Ui.texto(this, "Próxima dose: " + Datas.br(v.proxima) + " (" + Datas.relativo(v.proxima) + ")", 13, k.texto, false));
            TextView reg = Ui.texto(this, "Registrar nova dose →", 13, k.destaque, true);
            reg.setPadding(0, dp(8), 0, 0);
            c.addView(reg);
            reg.setOnClickListener(z -> startActivity(new Intent(this, RegistroActivity.class).putExtra("pet", pet.id).putExtra("tipo", "vacina")));
            c.setOnClickListener(z -> detalhes(v));
            corpo.addView(c);
        }
        corpo.addView(titulo("Todas as aplicações"));
        for (Dados.Registro r : Dados.registros(this, pet.id)) if (r.tipo.equals("vacina")) corpo.addView(itemTempo(r));
    }

    // ================================================================== SAÚDE

    private void abaSaude() {
        List<Dados.Registro> rs = Dados.registros(this, pet.id);
        List<Dados.Registro> pesos = new ArrayList<>();
        for (int i = rs.size() - 1; i >= 0; i--) if (rs.get(i).peso > 0) pesos.add(rs.get(i));

        LinearLayout c = cartao();
        LinearLayout l = Ui.linha(this);
        l.addView(Ui.texto(this, "⚖️  Evolução do peso", 16, k.texto, true), Ui.lp(0, -2, 1f));
        TextView add = Ui.texto(this, "+ Pesar", 14, k.destaque, true);
        add.setOnClickListener(v -> novoRegistro("peso"));
        l.addView(add);
        c.addView(l);
        if (pesos.size() == 0) c.addView(Ui.texto(this, "Registre o peso para acompanhar a evolução 📈", 13, k.sub, false));
        else {
            Dados.Registro ult = pesos.get(pesos.size() - 1);
            String var = "";
            if (pesos.size() > 1) {
                double d = ult.peso - pesos.get(pesos.size() - 2).peso;
                var = String.format(new Locale("pt", "BR"), "  (%s%.1f kg)", d >= 0 ? "+" : "", d);
            }
            c.addView(Ui.texto(this, Datas.kg(ult.peso) + var + " em " + Datas.br(ult.data), 13, k.sub, false));
            if (pesos.size() > 1) {
                GraficoPeso g = new GraficoPeso(this, pesos, k);
                LinearLayout.LayoutParams gl = Ui.lp(-1, dp(170));
                gl.topMargin = dp(10);
                c.addView(g, gl);
            }
        }
        corpo.addView(c);

        if (!pet.obs.isEmpty()) {
            LinearLayout o = cartao();
            o.addView(Ui.texto(this, "📝  Observações / cuidados", 16, k.texto, true));
            TextView t = Ui.texto(this, pet.obs, 14, k.texto, false);
            t.setPadding(0, dp(6), 0, 0);
            o.addView(t);
            corpo.addView(o);
        }

        secaoTipo(rs, "medicamento", "💊  Medicamentos");
        secaoTipo(rs, "procedimento", "🏥  Procedimentos");
        secaoTipo(rs, "exame", "🔬  Exames");

        int consultas = 0;
        for (Dados.Registro r : rs) if (r.tipo.equals("consulta")) consultas++;
        LinearLayout n = cartao();
        n.addView(Ui.texto(this, "📊  Em números", 16, k.texto, true));
        n.addView(Ui.texto(this, consultas + " consultas · " + contar(rs, "vacina") + " vacinas · " + contar(rs, "procedimento") + " procedimentos · "
                + contar(rs, "exame") + " exames · " + contar(rs, "banho") + " banhos", 13, k.sub, false));
        corpo.addView(n);
    }

    private int contar(List<Dados.Registro> rs, String t) {
        int n = 0;
        for (Dados.Registro r : rs) if (r.tipo.equals(t)) n++;
        return n;
    }

    private void secaoTipo(List<Dados.Registro> rs, String tipo, String titulo) {
        List<Dados.Registro> l = new ArrayList<>();
        for (Dados.Registro r : rs) if (r.tipo.equals(tipo)) l.add(r);
        if (tipo.equals("procedimento")) for (Dados.Registro r : rs) if (r.tipo.equals("consulta") && !r.procedimento.isEmpty()) l.add(r);
        if (l.isEmpty()) return;
        LinearLayout c = cartao();
        c.addView(Ui.texto(this, titulo, 16, k.texto, true));
        for (int i = 0; i < Math.min(5, l.size()); i++) {
            Dados.Registro r = l.get(i);
            LinearLayout li = Ui.linha(this);
            li.setPadding(0, dp(8), 0, 0);
            String nome = r.tipo.equals("consulta") ? r.procedimento : r.titulo;
            li.addView(Ui.texto(this, nome, 14, k.texto, false), Ui.lp(0, -2, 1f));
            li.addView(Ui.texto(this, Datas.br(r.data), 13, k.sub, false));
            li.setOnClickListener(v -> detalhes(r));
            c.addView(li);
        }
        corpo.addView(c);
    }

    static final class GraficoPeso extends View {
        private final List<Dados.Registro> pts;
        private final Ui.Cores k;
        private final Paint linha = new Paint(Paint.ANTI_ALIAS_FLAG), area = new Paint(Paint.ANTI_ALIAS_FLAG), ponto = new Paint(Paint.ANTI_ALIAS_FLAG),
                txt = new Paint(Paint.ANTI_ALIAS_FLAG), grade = new Paint();

        GraficoPeso(Context c, List<Dados.Registro> pts, Ui.Cores k) {
            super(c);
            this.pts = pts;
            this.k = k;
            float d = c.getResources().getDisplayMetrics().density;
            linha.setStyle(Paint.Style.STROKE);
            linha.setStrokeWidth(3 * d);
            linha.setColor(k.destaque);
            linha.setStrokeJoin(Paint.Join.ROUND);
            ponto.setColor(k.destaque);
            txt.setColor(k.sub);
            txt.setTextSize(11 * d);
            grade.setColor(k.linha);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = getResources().getDisplayMetrics().density;
            float w = getWidth(), h = getHeight(), top = 18 * d, bot = h - 20 * d, esq = 8 * d, dir = w - 8 * d;
            double mn = Double.MAX_VALUE, mx = 0;
            for (Dados.Registro r : pts) {
                mn = Math.min(mn, r.peso);
                mx = Math.max(mx, r.peso);
            }
            double folga = Math.max(0.5, (mx - mn) * 0.2);
            mn -= folga;
            mx += folga;
            for (int i = 0; i < 3; i++) {
                float y = top + (bot - top) * i / 2f;
                c.drawLine(esq, y, dir, y, grade);
            }
            Path p = new Path(), a = new Path();
            int n = pts.size();
            float[] xs = new float[n], ys = new float[n];
            for (int i = 0; i < n; i++) {
                xs[i] = esq + (dir - esq) * (n == 1 ? 0.5f : i / (float) (n - 1));
                ys[i] = (float) (bot - (bot - top) * (pts.get(i).peso - mn) / (mx - mn));
                if (i == 0) {
                    p.moveTo(xs[i], ys[i]);
                    a.moveTo(xs[i], bot);
                    a.lineTo(xs[i], ys[i]);
                } else {
                    p.lineTo(xs[i], ys[i]);
                    a.lineTo(xs[i], ys[i]);
                }
            }
            a.lineTo(xs[n - 1], bot);
            a.close();
            area.setShader(new LinearGradient(0, top, 0, bot, (k.destaque & 0x00FFFFFF) | 0x55000000, (k.destaque & 0x00FFFFFF), Shader.TileMode.CLAMP));
            c.drawPath(a, area);
            c.drawPath(p, linha);
            for (int i = 0; i < n; i++) {
                c.drawCircle(xs[i], ys[i], 4.5f * d, ponto);
                if (i == 0 || i == n - 1 || n <= 6) {
                    String s = Datas.kg(pts.get(i).peso);
                    float tw = txt.measureText(s);
                    c.drawText(s, Math.max(0, Math.min(w - tw, xs[i] - tw / 2)), ys[i] - 8 * d, txt);
                    String dt = Datas.br(pts.get(i).data).substring(0, 5);
                    float dw = txt.measureText(dt);
                    c.drawText(dt, Math.max(0, Math.min(w - dw, xs[i] - dw / 2)), h - 4 * d, txt);
                }
            }
        }
    }

    // ================================================================== GASTOS

    private void abaGastos() {
        List<Dados.Registro> rs = Dados.registros(this, pet.id);
        long total = 0, mes = 0, ano = 0, plano = 0;
        String hoje = Datas.hoje();
        long[] porTipo = new long[Dados.TIPOS.length];
        Map<String, Long> porMes = new LinkedHashMap<>();
        Calendar kk = Calendar.getInstance();
        kk.add(Calendar.MONTH, -11);
        for (int i = 0; i < 12; i++) {
            porMes.put(String.format(Locale.US, "%04d-%02d", kk.get(Calendar.YEAR), kk.get(Calendar.MONTH) + 1), 0L);
            kk.add(Calendar.MONTH, 1);
        }
        for (Dados.Registro r : rs) {
            if (r.valor <= 0) continue;
            total += r.valor;
            if (r.data.startsWith(hoje.substring(0, 7))) mes += r.valor;
            if (r.data.startsWith(hoje.substring(0, 4))) ano += r.valor;
            if (r.plano) plano += r.valor;
            porTipo[Dados.indiceTipo(r.tipo)] += r.valor;
            String m = r.data.length() >= 7 ? r.data.substring(0, 7) : "";
            if (porMes.containsKey(m)) porMes.put(m, porMes.get(m) + r.valor);
        }
        if (total == 0) {
            corpo.addView(vazio("Nenhum valor registrado ainda.\nAo lançar consultas, vacinas e banhos com o valor cobrado, os gastos aparecem aqui 💰"));
            return;
        }
        LinearLayout c = Ui.coluna(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF2B1D14, 0xFF5A2E18});
        g.setCornerRadius(dp(22));
        c.setBackground(g);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cl = Ui.lp(-1, -2);
        cl.topMargin = dp(12);
        corpo.addView(c, cl);
        c.addView(Ui.texto(this, "Total investido no " + pet.nome, 13, 0xCCFFFFFF, false));
        c.addView(Ui.texto(this, Dados.reais(total), 32, 0xFFFFFFFF, true));
        LinearLayout l = Ui.linha(this);
        l.setPadding(0, dp(10), 0, 0);
        miniTotal(l, "Este mês", mes);
        miniTotal(l, "Este ano", ano);
        miniTotal(l, "Pelo plano", plano);
        c.addView(l);
        long meses = 0;
        for (long v : porMes.values()) if (v > 0) meses++;
        long media = 0;
        for (long v : porMes.values()) media += v;
        media = meses == 0 ? 0 : media / 12;
        c.addView(Ui.texto(this, "Média mensal (12 meses): " + Dados.reais(media), 12, 0xCCFFFFFF, false));
        TextView tudo = Ui.texto(this, "Ver todos os lançamentos com detalhes →", 14, 0xFFFFD2BA, true);
        tudo.setPadding(0, dp(10), 0, 0);
        tudo.setOnClickListener(v -> startActivity(new Intent(this, ListaActivity.class).putExtra("modo", "gastos").putExtra("pet", pet.id).putExtra("periodo", "tudo")));
        c.addView(tudo);

        LinearLayout t = cartao();
        t.addView(Ui.texto(this, "Por categoria", 16, k.texto, true));
        long maior = 1;
        for (long v : porTipo) maior = Math.max(maior, v);
        for (int i = 0; i < porTipo.length; i++) {
            if (porTipo[i] == 0) continue;
            t.addView(barra(Dados.EMOJI_TIPOS[i] + " " + Dados.NOMES_TIPOS[i], porTipo[i], porTipo[i] / (float) maior, Dados.COR_TIPOS[i],
                    Math.round(100f * porTipo[i] / total) + "%"));
        }
        corpo.addView(t);

        LinearLayout m = cartao();
        m.addView(Ui.texto(this, "Últimos 12 meses", 16, k.texto, true));
        LinearLayout colunas = Ui.linha(this);
        colunas.setGravity(Gravity.BOTTOM);
        long maxMes = 1;
        for (long v : porMes.values()) maxMes = Math.max(maxMes, v);
        for (Map.Entry<String, Long> e : porMes.entrySet()) {
            LinearLayout cc = Ui.coluna(this);
            cc.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            View b = new View(this);
            b.setBackground(Ui.fundo(e.getValue() > 0 ? k.destaque : k.linha, dp(6)));
            int alt = (int) (dp(110) * (e.getValue() / (float) maxMes));
            cc.addView(b, Ui.lp(dp(14), Math.max(dp(4), alt)));
            int mi = Integer.parseInt(e.getKey().substring(5)) - 1;
            cc.addView(Ui.texto(this, Datas.MESES[mi].substring(0, 1).toUpperCase(), 10, k.sub, false));
            long val = e.getValue();
            String nm = Datas.MESES_LONGOS[mi] + "/" + e.getKey().substring(0, 4);
            cc.setOnClickListener(v -> Toast.makeText(this, nm + ": " + Dados.reais(val), Toast.LENGTH_SHORT).show());
            colunas.addView(cc, Ui.lp(0, dp(130), 1f));
        }
        LinearLayout.LayoutParams ml = Ui.lp(-1, -2);
        ml.topMargin = dp(10);
        m.addView(colunas, ml);
        corpo.addView(m);
    }

    private void miniTotal(LinearLayout l, String r, long v) {
        LinearLayout c = Ui.coluna(this);
        c.addView(Ui.texto(this, r, 11, 0xAAFFFFFF, false));
        c.addView(Ui.texto(this, Dados.reais(v), 14, 0xFFFFFFFF, true));
        l.addView(c, Ui.lp(0, -2, 1f));
    }

    private View barra(String rot, long valor, float frac, int cor, String pct) {
        LinearLayout c = Ui.coluna(this);
        c.setPadding(0, dp(10), 0, 0);
        LinearLayout l = Ui.linha(this);
        l.addView(Ui.texto(this, rot, 13, k.texto, false), Ui.lp(0, -2, 1f));
        l.addView(Ui.texto(this, Dados.reais(valor) + "  ·  " + pct, 13, k.sub, false));
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

    // ================================================================== LEMBRETES

    private void abaLembretes() {
        List<Dados.Lembrete> l = Dados.lembretes(this, pet.id);
        if (l.isEmpty()) {
            corpo.addView(vazio("Nenhum lembrete.\nToque em ＋ para lembrar do vermífugo, antipulgas, banho ou remédio 🔔"));
            return;
        }
        boolean feitosTitulo = false;
        for (Dados.Lembrete m : l) {
            if (m.feito && !feitosTitulo) {
                corpo.addView(titulo("Concluídos"));
                feitosTitulo = true;
            }
            corpo.addView(itemLembrete(m));
        }
    }

    private View itemLembrete(Dados.Lembrete m) {
        return Detalhes.itemLembrete(this, m, false, this::pintarAba);
    }

    private void editarLembrete(Dados.Lembrete orig) {
        if (orig == null) Detalhes.lembrete(this, null, pet.id, () -> {
            aba = "lembretes";
            pintarAba();
        });
        else Detalhes.lembrete(this, orig, pet.id, this::pintarAba);
    }

    // ================================================================== FICHA

    private void abaFicha() {
        LinearLayout c = cartao();
        c.addView(Ui.texto(this, "🐶  Dados do dog", 16, k.destaque, true));
        campo(c, "Nome do dog", pet.nome);
        campo(c, "Idade", pet.idadeTexto());
        campo(c, "Data de nascimento", Datas.br(pet.nascimento));
        campo(c, "Raça", pet.raca);
        campo(c, "Sexo", pet.sexo);
        campo(c, "Porte", pet.porte);
        campo(c, "Pelagem / cor", pet.cor);
        campo(c, "Castrado", pet.castrado ? "Sim" : "Não");
        campo(c, "Microchip", pet.microchip);
        campo(c, "Plano de saúde", pet.plano.isEmpty() ? "Não tem" : pet.plano);
        campo(c, "Categoria do plano", pet.categoriaPlano);
        campo(c, "Observação", pet.obs);
        corpo.addView(c);

        LinearLayout d = cartao();
        d.addView(Ui.texto(this, "👤  Proprietário", 16, k.destaque, true));
        campo(d, "Nome do proprietário", pet.donoNome);
        campo(d, "Data de nascimento", Datas.br(pet.donoNasc));
        campo(d, "RG", pet.rg);
        campo(d, "CPF", pet.cpf);
        campo(d, "Telefone", pet.telefone);
        campo(d, "Endereço", pet.endereco);
        campo(d, "Bairro", pet.bairro);
        campo(d, "Cidade", pet.cidade);
        campo(d, "Estado", pet.estado);
        corpo.addView(d);

        LinearLayout b = Ui.coluna(this);
        b.setPadding(0, dp(14), 0, 0);
        android.widget.Button car = Ui.botaoGradiente(this, "🪪  Compartilhar carteirinha (com QR)");
        car.setOnClickListener(v -> enviarCarteirinha());
        b.addView(car, Ui.lp(-1, dp(54)));
        android.widget.Button qrb = Ui.botao(this, "🏷️  QR code para a coleira", false);
        qrb.setOnClickListener(v -> qrColeira());
        LinearLayout.LayoutParams ql = Ui.lp(-1, dp(52));
        ql.topMargin = dp(10);
        b.addView(qrb, ql);
        android.widget.Button wg = Ui.botao(this, "🏠  Colocar widget na tela inicial", false);
        wg.setOnClickListener(v -> widget());
        LinearLayout.LayoutParams wl = Ui.lp(-1, dp(52));
        wl.topMargin = dp(10);
        b.addView(wg, wl);
        android.widget.Button pdf = Ui.botao(this, "📄  Ficha completa em PDF (para o veterinário)", false);
        pdf.setOnClickListener(v -> enviarPdf());
        LinearLayout.LayoutParams pl = Ui.lp(-1, dp(52));
        pl.topMargin = dp(10);
        b.addView(pdf, pl);
        corpo.addView(b);
    }

    private void campo(LinearLayout c, String r, String v) {
        if (v == null || v.isEmpty()) return;
        LinearLayout l = Ui.linha(this);
        l.setGravity(Gravity.TOP);
        l.setPadding(0, dp(10), 0, 0);
        l.addView(Ui.texto(this, r, 13, k.sub, false), Ui.lp(0, -2, 1f));
        TextView t = Ui.texto(this, v, 14, k.texto, true);
        t.setGravity(Gravity.END);
        t.setTextIsSelectable(true);
        l.addView(t, Ui.lp(0, -2, 1.3f));
        c.addView(l);
    }

    // ================================================================== compartilhar

    private void compartilhar() {
        String[] op = {"🪪  Carteirinha com QR code (imagem)", "🏷️  QR code para a coleira", "🏠  Widget na tela inicial",
                "📄  Ficha completa em PDF", "💬  Resumo em texto (WhatsApp)"};
        new AlertDialog.Builder(this).setTitle(pet.nome).setItems(op, (d, w) -> {
            if (w == 0) enviarCarteirinha();
            else if (w == 1) qrColeira();
            else if (w == 2) widget();
            else if (w == 3) enviarPdf();
            else enviarTexto();
        }).show();
    }

    private void widget() {
        new AlertDialog.Builder(this).setTitle("Widget do " + pet.nome)
                .setMessage("Mostra a foto, a próxima vacina e os lembretes na tela inicial. Quer incluir o QR code da coleira no widget?")
                .setPositiveButton("Com QR code", (d, w) -> WidgetPet.fixar(this, pet.id, true))
                .setNegativeButton("Sem QR code", (d, w) -> WidgetPet.fixar(this, pet.id, false))
                .show();
    }

    /** QR code da coleira: escolha o que aparece para quem encontrar o dog */
    private void qrColeira() {
        Qr.Opcoes o = Qr.opcoes(this, pet.id);
        ScrollView sv = new ScrollView(this);
        LinearLayout v = Ui.coluna(this);
        v.setPadding(dp(20), dp(8), dp(20), dp(4));
        sv.addView(v);
        v.addView(Ui.texto(this, "Quem encontrar o " + pet.nome + " aponta a câmera do celular e vê estas informações (funciona sem internet).", 13, k.sub, false));
        android.widget.ImageView img = new android.widget.ImageView(this);
        img.setBackground(Ui.fundo(0xFFFFFFFF, dp(16)));
        img.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams il = Ui.lp(dp(230), dp(230));
        il.gravity = Gravity.CENTER_HORIZONTAL;
        il.topMargin = dp(12);
        v.addView(img, il);
        TextView previa = Ui.texto(this, "", 12, k.sub, false);
        previa.setPadding(dp(12), dp(10), dp(12), dp(10));
        previa.setBackground(Ui.fundo(k.chip, dp(12)));
        LinearLayout.LayoutParams pl = Ui.lp(-1, -2);
        pl.topMargin = dp(10);
        v.addView(previa, pl);
        Runnable pintar = () -> {
            String t = Qr.textoColeira(this, pet, o);
            img.setImageBitmap(Qr.gerar(t, 600, 0xFF000000, 0xFFFFFFFF));
            previa.setText(t);
        };
        EditText msg = Campos.multilinha(this, v, "Mensagem", o.mensagem, "Ex.: Estou perdido, ligue para minha família", true);
        msg.setMinLines(2);
        msg.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(android.text.Editable s) {
                o.mensagem = s.toString();
                pintar.run();
            }
        });
        opcao(v, "👤 Nome do tutor", o.dono, x -> o.dono = x, pintar);
        opcao(v, "📞 Telefone" + (pet.telefone.isEmpty() ? " (cadastre no pet)" : ""), o.telefone, x -> o.telefone = x, pintar);
        opcao(v, "📍 Bairro e cidade", o.local, x -> o.local = x, pintar);
        opcao(v, "⚠️ Observações / cuidados de saúde", o.saude, x -> o.saude = x, pintar);
        opcao(v, "🛡️ Plano de saúde", o.plano, x -> o.plano = x, pintar);
        opcao(v, "💉 Vacinas", o.vacinas, x -> o.vacinas = x, pintar);
        TextView aviso = Ui.texto(this, "Por segurança, CPF, RG e endereço completo nunca entram no QR da coleira.", 12, k.sub, false);
        aviso.setPadding(0, dp(10), 0, 0);
        v.addView(aviso);
        pintar.run();
        new AlertDialog.Builder(this).setTitle("🏷️ QR code da coleira").setView(sv)
                .setPositiveButton("Compartilhar / imprimir", (d, w) -> {
                    Qr.salvar(this, pet.id, o);
                    WidgetPet.atualizarTodos(this);
                    try {
                        enviarArquivo(Relatorios.etiquetaColeira(this, pet, Qr.textoColeira(this, pet, o)), "image/png", "QR da coleira do " + pet.nome);
                    } catch (Exception e) {
                        Toast.makeText(this, "Não consegui gerar a imagem", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Salvar", (d, w) -> {
                    Qr.salvar(this, pet.id, o);
                    WidgetPet.atualizarTodos(this);
                    Toast.makeText(this, "QR atualizado (também no widget)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Fechar", null).show();
    }

    interface AoBool {
        void v(boolean b);
    }

    private void opcao(LinearLayout v, String t, boolean on, AoBool cb, Runnable depois) {
        android.widget.Switch s = new android.widget.Switch(this);
        s.setText(t);
        s.setTextColor(k.texto);
        s.setTextSize(14);
        s.setChecked(on);
        s.setPadding(0, dp(10), 0, dp(2));
        s.setOnCheckedChangeListener((x, b) -> {
            cb.v(b);
            depois.run();
        });
        v.addView(s);
    }

    private void enviarArquivo(File f, String mime, String titulo) {
        android.net.Uri u = Arquivos.uri(f);
        Intent i = new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, u)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.setClipData(ClipData.newRawUri(f.getName(), u));
        startActivity(Intent.createChooser(i, titulo));
    }

    private void enviarCarteirinha() {
        try {
            enviarArquivo(Relatorios.carteirinha(this, pet), "image/png", "Carteirinha do " + pet.nome);
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui gerar a carteirinha", Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarPdf() {
        try {
            enviarArquivo(Relatorios.fichaPdf(this, pet), "application/pdf", "Ficha do " + pet.nome);
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui gerar o PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarTexto() {
        StringBuilder s = new StringBuilder("🐾 *" + pet.nome + "*\n");
        String l = Relatorios.juntar(" · ", pet.raca, pet.sexo, pet.idadeTexto());
        if (!l.isEmpty()) s.append(l).append("\n");
        if (!pet.plano.isEmpty()) s.append("🛡️ Plano: ").append(Relatorios.juntar(" - ", pet.plano, pet.categoriaPlano)).append("\n");
        List<Dados.Registro> vs = Relatorios.vacinasAtuais(this, pet.id);
        if (!vs.isEmpty()) {
            s.append("\n💉 *Vacinas*\n");
            for (Dados.Registro v : vs)
                s.append("• ").append(v.titulo).append(": ").append(Datas.br(v.data)).append(v.proxima.isEmpty() ? "" : " (próxima " + Datas.br(v.proxima) + ")").append("\n");
        }
        List<Dados.Registro> rs = Dados.registros(this, pet.id);
        int n = 0;
        for (Dados.Registro r : rs) {
            if (!r.foiAoVet() || r.tipo.equals("vacina")) continue;
            if (n == 0) s.append("\n🩺 *Últimos atendimentos*\n");
            s.append("• ").append(Datas.br(r.data)).append(" — ").append(r.resumo()).append(r.descricao.isEmpty() ? "" : ": " + r.descricao).append("\n");
            if (++n == 5) break;
        }
        if (!pet.obs.isEmpty()) s.append("\n📝 ").append(pet.obs).append("\n");
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s.toString()), "Enviar resumo"));
    }
}
