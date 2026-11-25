package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

/** Campos de formulário com rótulo, no estilo do app. */
public final class Campos {
    private Campos() {
    }

    static int dp(Activity a, float v) {
        return Ui.dp(a, v);
    }

    static GradientDrawable caixa(Activity a) {
        Ui.Cores k = Ui.cores(a);
        GradientDrawable g = new GradientDrawable();
        g.setColor(k.fundo);
        g.setCornerRadius(dp(a, 14));
        g.setStroke(dp(a, 1), k.linha);
        return g;
    }

    static TextView rotulo(Activity a, ViewGroup pai, String r, boolean opcional) {
        Ui.Cores k = Ui.cores(a);
        TextView t = Ui.texto(a, r + (opcional ? "  (opcional)" : ""), 13, k.sub, true);
        t.setPadding(dp(a, 2), dp(a, 14), 0, dp(a, 6));
        pai.addView(t);
        return t;
    }

    static void estilo(Activity a, EditText e) {
        Ui.Cores k = Ui.cores(a);
        e.setBackground(caixa(a));
        e.setTextColor(k.texto);
        e.setHintTextColor(0x99A3A3AD);
        e.setTextSize(16);
        e.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
    }

    public static EditText texto(Activity a, ViewGroup pai, String r, String valor, String dica, boolean opcional) {
        rotulo(a, pai, r, opcional);
        EditText e = new EditText(a);
        estilo(a, e);
        e.setText(valor);
        e.setHint(dica);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        pai.addView(e, Ui.lp(-1, -2));
        return e;
    }

    public static EditText multilinha(Activity a, ViewGroup pai, String r, String valor, String dica, boolean opcional) {
        EditText e = texto(a, pai, r, valor, dica, opcional);
        e.setSingleLine(false);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setMinLines(3);
        e.setGravity(Gravity.TOP | Gravity.START);
        return e;
    }

    public static AutoCompleteTextView sugestoes(Activity a, ViewGroup pai, String r, String valor, String dica, boolean opcional, String[] lista) {
        rotulo(a, pai, r, opcional);
        AutoCompleteTextView e = new AutoCompleteTextView(a);
        estilo(a, e);
        e.setText(valor);
        e.setHint(dica);
        e.setSingleLine(true);
        e.setThreshold(1);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        e.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_dropdown_item_1line, lista));
        pai.addView(e, Ui.lp(-1, -2));
        return e;
    }

    public static EditText dinheiro(Activity a, ViewGroup pai, String r, long centavos, boolean opcional) {
        EditText e = texto(a, pai, r, Dados.valorEditavel(centavos), "R$ 0,00", opcional);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789,."));
        return e;
    }

    public static EditText cpf(Activity a, ViewGroup pai, String valor) {
        EditText e = texto(a, pai, "CPF", valor, "000.000.000-00", false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        mascara(e, Datas::mascaraCpf);
        return e;
    }

    public static EditText telefone(Activity a, ViewGroup pai, String valor) {
        EditText e = texto(a, pai, "Telefone / WhatsApp", valor, "(21) 99999-9999", true);
        e.setInputType(InputType.TYPE_CLASS_PHONE);
        mascara(e, Datas::mascaraTelefone);
        return e;
    }

    interface Mascara {
        String m(String s);
    }

    static void mascara(EditText e, Mascara m) {
        e.addTextChangedListener(new TextWatcher() {
            boolean mudando;

            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                if (mudando) return;
                String f = m.m(s.toString());
                if (!f.equals(s.toString())) {
                    mudando = true;
                    s.replace(0, s.length(), f);
                    mudando = false;
                }
            }
        });
    }

    /** campo de data: abre o calendário; guarda yyyy-MM-dd na tag */
    public static final class Data {
        public final TextView view;
        private String valor;
        private final Activity a;
        private final String vazio;
        public Runnable aoMudar;

        Data(Activity a, ViewGroup pai, String r, String valor, boolean opcional, String vazio) {
            this.a = a;
            this.valor = valor == null ? "" : valor;
            this.vazio = vazio;
            rotulo(a, pai, r, opcional);
            LinearLayout l = Ui.linha(a);
            l.setBackground(caixa(a));
            l.setPadding(dp(a, 14), dp(a, 13), dp(a, 10), dp(a, 13));
            view = Ui.texto(a, "", 16, Ui.cores(a).texto, false);
            l.addView(view, Ui.lp(0, -2, 1f));
            TextView limpar = Ui.texto(a, "✕", 16, Ui.cores(a).sub, false);
            limpar.setPadding(dp(a, 10), 0, dp(a, 6), 0);
            limpar.setVisibility(opcional ? View.VISIBLE : View.GONE);
            limpar.setOnClickListener(v -> definir(""));
            l.addView(limpar);
            l.addView(Ui.texto(a, "📅", 18, 0, false));
            l.setOnClickListener(v -> abrir());
            pai.addView(l, Ui.lp(-1, -2));
            pintar();
        }

        public static Data criar(Activity a, ViewGroup pai, String r, String valor, boolean opcional, String vazio) {
            return new Data(a, pai, r, valor, opcional, vazio);
        }

        void abrir() {
            Calendar k = Datas.cal(valor);
            if (k == null) k = Calendar.getInstance();
            new DatePickerDialog(a, (dp, y, m, d) -> definir(Datas.iso(y, m, d)), k.get(Calendar.YEAR), k.get(Calendar.MONTH), k.get(Calendar.DAY_OF_MONTH)).show();
        }

        public void definir(String v) {
            valor = v;
            pintar();
            if (aoMudar != null) aoMudar.run();
        }

        void pintar() {
            Ui.Cores k = Ui.cores(a);
            view.setText(valor.isEmpty() ? vazio : Datas.br(valor) + "  ·  " + Datas.relativo(valor));
            view.setTextColor(valor.isEmpty() ? 0x99A3A3AD : k.texto);
        }

        public String valor() {
            return valor;
        }
    }

    public static void escolherHora(Activity a, int h, int m, TimePickerDialog.OnTimeSetListener cb) {
        new TimePickerDialog(a, cb, h, m, true).show();
    }

    public static String hora(int h, int m) {
        return String.format(Locale.US, "%02d:%02d", h, m);
    }
}
