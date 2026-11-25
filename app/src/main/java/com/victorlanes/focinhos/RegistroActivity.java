package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Novo registro no histórico do dog (consulta, vacina, procedimento, exame, remédio, banho, peso...). */
public class RegistroActivity extends Activity {

    static final String[] VACINAS = {"V8 (Polivalente)", "V10 (Polivalente)", "Antirrábica", "Gripe canina (Tosse dos canis)",
            "Giárdia", "Leishmaniose", "V5", "Reforço anual V10", "Reforço antirrábica"};
    static final String[] PROCEDIMENTOS = {"Castração", "Limpeza de tártaro", "Cirurgia", "Curativo", "Retirada de pontos",
            "Aplicação de medicamento", "Soroterapia", "Microchipagem", "Extração dentária", "Internação"};
    static final String[] EXAMES = {"Hemograma", "Bioquímico", "Ultrassom", "Raio-X", "Urina", "Fezes (coproparasitológico)",
            "Ecocardiograma", "Teste de Leishmaniose", "Teste de Cinomose", "Glicemia"};
    static final String[] MEDICAMENTOS = {"Vermífugo", "Antipulgas", "Carrapaticida", "Antibiótico", "Anti-inflamatório",
            "Vitamina / suplemento", "Colírio", "Pomada"};

    private Ui.Cores k;
    private Dados.Pet pet;
    private Dados.Registro reg;
    private boolean novo;
    private LinearLayout campos;
    private String tipo;

    private Campos.Data data, proxima;
    private EditText titulo, clinica, procedimento, descricao, valor, peso;
    private Switch plano, lembrar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Visual.preparar(this, false);
        k = Ui.cores(this);
        long rid = getIntent().getLongExtra("registro", 0);
        reg = rid != 0 ? Dados.registro(this, rid) : null;
        novo = reg == null;
        if (novo) {
            reg = new Dados.Registro();
            reg.petId = getIntent().getLongExtra("pet", 0);
            reg.data = Datas.hoje();
            String t = getIntent().getStringExtra("tipo");
            if (t != null) reg.tipo = t;
        }
        pet = Dados.pet(this, reg.petId);
        if (pet == null) {
            finish();
            return;
        }
        tipo = reg.tipo;
        montar();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void montar() {
        LinearLayout raiz = Ui.coluna(this);
        raiz.setBackgroundColor(k.fundo);
        setContentView(raiz);
        Ui.aplicarInsets(raiz);
        LinearLayout topo = Ui.linha(this);
        topo.setPadding(dp(8), dp(8), dp(16), dp(4));
        TextView voltar = Ui.texto(this, "←", 26, k.texto, false);
        voltar.setPadding(dp(12), dp(4), dp(12), dp(4));
        voltar.setOnClickListener(v -> finish());
        topo.addView(voltar);
        LinearLayout tt = Ui.coluna(this);
        tt.addView(Ui.texto(this, novo ? "Novo registro" : "Editar registro", 22, k.texto, true));
        tt.addView(Ui.texto(this, "Histórico do " + pet.nome, 13, k.sub, false));
        topo.addView(tt, Ui.lp(0, -2, 1f));
        raiz.addView(topo);

        ScrollView sv = new ScrollView(this);
        LinearLayout col = Ui.coluna(this);
        col.setPadding(dp(16), dp(4), dp(16), dp(24));
        sv.addView(col);
        raiz.addView(sv, Ui.lp(-1, 0, 1f));

        LinearLayout st = Ui.secao(this, col, "O que aconteceu?", null);
        String[] nomes = new String[Dados.TIPOS.length];
        for (int i = 0; i < nomes.length; i++) nomes[i] = Dados.EMOJI_TIPOS[i] + " " + Dados.NOMES_TIPOS[i];
        st.addView(new Ui.Chips(this, Dados.TIPOS, nomes, tipo, id -> {
            guardarDigitado();
            tipo = id;
            pintarCampos();
        }).view);

        campos = Ui.coluna(this);
        col.addView(campos);
        pintarCampos();

        LinearLayout barra = Ui.linha(this);
        barra.setPadding(dp(16), dp(10), dp(16), dp(12));
        if (!novo) {
            Button ex = Ui.botao(this, "✕ Excluir", false);
            ex.setTextColor(0xFFEF4444);
            ex.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Excluir este registro?")
                    .setPositiveButton("Excluir", (d, w) -> {
                        Dados.removerRegistro(this, reg.id);
                        Avisos.agendar(this);
                        finish();
                    }).setNegativeButton("Cancelar", null).show());
            barra.addView(ex, Ui.lp(0, dp(54), 1f));
        }
        Button salvar = Ui.botaoGradiente(this, "Salvar");
        salvar.setOnClickListener(v -> salvar());
        LinearLayout.LayoutParams lp = Ui.lp(0, dp(54), 2f);
        if (!novo) lp.leftMargin = dp(10);
        barra.addView(salvar, lp);
        raiz.addView(barra);
    }

    /** guarda o que já foi digitado antes de trocar o tipo */
    private void guardarDigitado() {
        if (data == null) return;
        reg.data = data.valor();
        reg.titulo = titulo != null ? titulo.getText().toString() : reg.titulo;
        reg.clinica = clinica != null ? clinica.getText().toString() : reg.clinica;
        reg.procedimento = procedimento != null ? procedimento.getText().toString() : reg.procedimento;
        reg.descricao = descricao != null ? descricao.getText().toString() : reg.descricao;
        if (valor != null) reg.valor = Math.max(0, Dados.centavos(valor.getText().toString()));
        if (proxima != null) reg.proxima = proxima.valor();
        if (plano != null) reg.plano = plano.isChecked();
        if (peso != null) reg.peso = lerPeso();
    }

    private double lerPeso() {
        try {
            String s = peso.getText().toString().replace(',', '.').trim();
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void pintarCampos() {
        campos.removeAllViews();
        titulo = clinica = procedimento = descricao = valor = peso = null;
        proxima = null;
        plano = lembrar = null;
        LinearLayout s = Ui.secao(this, campos, Dados.EMOJI_TIPOS[Dados.indiceTipo(tipo)] + "  " + Dados.NOMES_TIPOS[Dados.indiceTipo(tipo)], null);

        String rotData = tipo.equals("consulta") ? "Data da ida ao veterinário" : tipo.equals("vacina") ? "Data da vacina" : "Data";
        data = Campos.Data.criar(this, s, rotData, reg.data, false, "Escolha a data");

        boolean vet = tipo.equals("consulta") || tipo.equals("vacina") || tipo.equals("procedimento") || tipo.equals("exame");
        switch (tipo) {
            case "vacina":
                titulo = Campos.sugestoes(this, s, "Qual vacina", reg.titulo, "Ex.: V10, Antirrábica", false, VACINAS);
                break;
            case "consulta":
                titulo = Campos.texto(this, s, "Motivo da consulta", reg.titulo, "Ex.: Check-up, vômito, coceira", true);
                break;
            case "procedimento":
                titulo = Campos.sugestoes(this, s, "Qual procedimento", reg.titulo, "Ex.: Castração", false, PROCEDIMENTOS);
                break;
            case "exame":
                titulo = Campos.sugestoes(this, s, "Qual exame", reg.titulo, "Ex.: Hemograma", false, EXAMES);
                break;
            case "medicamento":
                titulo = Campos.sugestoes(this, s, "Qual medicamento", reg.titulo, "Ex.: Vermífugo", false, MEDICAMENTOS);
                break;
            case "banho":
                titulo = Campos.sugestoes(this, s, "Serviço", reg.titulo, "Ex.: Banho e tosa higiênica", true,
                        new String[]{"Banho", "Banho e tosa", "Tosa higiênica", "Tosa completa", "Hidratação", "Corte de unhas"});
                break;
            case "peso":
                break;
            default:
                titulo = Campos.texto(this, s, "Título", reg.titulo, "Ex.: Compra de ração", false);
        }
        if (tipo.equals("peso") || tipo.equals("consulta")) {
            peso = Campos.texto(this, s, "Peso (kg)", reg.peso > 0 ? String.valueOf(reg.peso).replace('.', ',') : "", "Ex.: 8,4", !tipo.equals("peso"));
            peso.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            peso.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789,."));
        }
        if (vet || tipo.equals("banho"))
            clinica = Campos.texto(this, s, tipo.equals("banho") ? "Pet shop" : "Veterinário / clínica", reg.clinica, "Nome", true);
        if (tipo.equals("consulta")) {
            procedimento = Campos.sugestoes(this, s, "Teve algum procedimento? Qual", reg.procedimento, "Deixe vazio se não teve", true, PROCEDIMENTOS);
        }
        String rotDesc = tipo.equals("consulta") ? "Descrição da consulta" : tipo.equals("medicamento") ? "Dose e como dar" : "Descrição / observações";
        descricao = Campos.multilinha(this, s, rotDesc, reg.descricao,
                tipo.equals("consulta") ? "O que o veterinário disse, diagnóstico, receita…" : "", true);

        if (!tipo.equals("peso")) {
            valor = Campos.dinheiro(this, s, "Valor cobrado", reg.valor, true);
            if (!pet.plano.isEmpty()) {
                plano = new Switch(this);
                plano.setText("Coberto pelo plano " + pet.plano);
                plano.setTextColor(k.texto);
                plano.setTextSize(15);
                plano.setChecked(reg.plano);
                plano.setPadding(dp(2), dp(14), 0, dp(4));
                s.addView(plano);
            }
        }

        if (tipo.equals("vacina") || tipo.equals("medicamento") || tipo.equals("consulta") || tipo.equals("banho")) {
            String rot = tipo.equals("vacina") ? "Próxima dose" : tipo.equals("medicamento") ? "Próxima aplicação" : tipo.equals("banho") ? "Próximo banho" : "Retorno";
            proxima = Campos.Data.criar(this, s, rot, reg.proxima, true, "Sem data");
            if (tipo.equals("vacina") || tipo.equals("medicamento") || tipo.equals("banho")) {
                LinearLayout at = Ui.linha(this);
                at.setPadding(0, dp(8), 0, 0);
                int[][] op = tipo.equals("vacina") ? new int[][]{{21, 0}, {30, 0}, {365, 0}} : tipo.equals("banho") ? new int[][]{{7, 0}, {15, 0}, {30, 0}} : new int[][]{{30, 0}, {90, 0}, {180, 0}};
                for (int[] o : op) {
                    int dias = o[0];
                    TextView c = Ui.texto(this, "+" + (dias == 365 ? "1 ano" : dias + " dias"), 13, k.destaque, true);
                    c.setPadding(dp(12), dp(7), dp(12), dp(7));
                    c.setBackground(Ui.clicavel(k.chip, dp(14), 0x22000000));
                    c.setOnClickListener(v -> proxima.definir(Datas.somarDias(data.valor().isEmpty() ? Datas.hoje() : data.valor(), dias)));
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = dp(8);
                    at.addView(c, lp);
                }
                s.addView(at);
            }
            lembrar = new Switch(this);
            lembrar.setText("🔔 Me lembrar um dia antes");
            lembrar.setTextColor(k.texto);
            lembrar.setTextSize(15);
            lembrar.setChecked(novo || Dados.lembreteDoRegistro(this, reg.id) != null);
            lembrar.setPadding(dp(2), dp(14), 0, dp(4));
            s.addView(lembrar);
        }
    }

    private void salvar() {
        guardarDigitado();
        reg.tipo = tipo;
        if (reg.data.isEmpty()) {
            Toast.makeText(this, "Escolha a data", Toast.LENGTH_SHORT).show();
            return;
        }
        if (titulo != null && reg.titulo.trim().isEmpty() && (tipo.equals("vacina") || tipo.equals("procedimento") || tipo.equals("exame") || tipo.equals("medicamento"))) {
            titulo.setError("Preencha este campo");
            titulo.requestFocus();
            return;
        }
        if (valor != null && Dados.centavos(valor.getText().toString()) < 0) {
            valor.setError("Valor inválido");
            valor.requestFocus();
            return;
        }
        if (tipo.equals("peso") && reg.peso <= 0) {
            peso.setError("Informe o peso");
            peso.requestFocus();
            return;
        }
        reg.titulo = reg.titulo.trim();
        reg.clinica = reg.clinica.trim();
        reg.procedimento = reg.procedimento.trim();
        reg.descricao = reg.descricao.trim();
        if (proxima == null) reg.proxima = "";
        if (valor == null) reg.valor = 0;
        if (peso == null) reg.peso = 0;
        Dados.salvarRegistro(this, reg);

        // lembrete automático da próxima dose / retorno
        Dados.Lembrete l = Dados.lembreteDoRegistro(this, reg.id);
        boolean quer = lembrar != null && lembrar.isChecked() && !reg.proxima.isEmpty();
        if (quer) {
            if (l == null) l = new Dados.Lembrete();
            l.petId = pet.id;
            l.registroId = reg.id;
            String oq = tipo.equals("vacina") ? "Vacina " + reg.titulo : tipo.equals("medicamento") ? reg.titulo : tipo.equals("banho") ? "Banho" : "Retorno ao veterinário";
            l.titulo = oq.trim() + " amanhã (" + Datas.br(reg.proxima) + ")";
            l.data = Datas.diasAte(reg.proxima) >= 1 ? Datas.somarDias(reg.proxima, -1) : reg.proxima;
            l.hora = 9;
            l.minuto = 0;
            l.repetirDias = 0;
            l.feito = false;
            Dados.salvarLembrete(this, l);
        } else if (l != null) Dados.removerLembrete(this, l.id);
        Avisos.agendar(this);
        Toast.makeText(this, "Registro salvo 🐾", Toast.LENGTH_SHORT).show();
        finish();
    }
}
