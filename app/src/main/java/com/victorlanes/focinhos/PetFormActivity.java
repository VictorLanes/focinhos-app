package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Cadastro / edição do pet e do proprietário. */
public class PetFormActivity extends Activity {

    private static final int REQ_FOTO = 21;
    static final String[] RACAS = {"SRD (vira-lata)", "Shih Tzu", "Yorkshire", "Poodle", "Lhasa Apso", "Spitz Alemão (Lulu)", "Pinscher",
            "Maltês", "Bulldog Francês", "Bulldog Inglês", "Golden Retriever", "Labrador", "Pastor Alemão", "Border Collie", "Beagle",
            "Dachshund (Salsicha)", "Pug", "Rottweiler", "Husky Siberiano", "Chihuahua", "Pit Bull", "American Bully", "Boxer",
            "Cocker Spaniel", "Schnauzer", "Dálmata", "Chow Chow", "Akita", "Shiba Inu", "Doberman", "Pastor Belga", "Bernese",
            "Cavalier King Charles", "Bichon Frisé", "Jack Russell", "Weimaraner", "Fila Brasileiro", "Basset Hound", "Corgi", "Samoieda"};
    static final String[] UFS = {"AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE",
            "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"};

    private Ui.Cores k;
    private Dados.Pet pet;
    private boolean novo;
    private String foto = "";
    private FrameLayout avatar;
    private String estado = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Visual.preparar(this, false);
        k = Ui.cores(this);
        long id = getIntent().getLongExtra("pet", 0);
        pet = id != 0 ? Dados.pet(this, id) : null;
        novo = pet == null;
        if (novo) pet = new Dados.Pet();
        foto = pet.foto;
        estado = pet.estado.isEmpty() ? "RJ" : pet.estado;
        if (b != null) foto = b.getString("foto", foto);
        montar();
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        o.putString("foto", foto);
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private EditText nome, idade, cor, microchip, plano, catPlano, obs, dono, rg, cpf, tel, end, bairro, cidade;
    private EditText raca;
    private Campos.Data nasc, donoNasc;
    private Ui.Chips sexo, porte;
    private Switch castrado;

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
        topo.addView(Ui.texto(this, novo ? "Novo focinho" : "Editar " + pet.nome, 22, k.texto, true), Ui.lp(0, -2, 1f));
        raiz.addView(topo);

        ScrollView sv = new ScrollView(this);
        LinearLayout col = Ui.coluna(this);
        col.setPadding(dp(16), dp(4), dp(16), dp(24));
        sv.addView(col);
        raiz.addView(sv, Ui.lp(-1, 0, 1f));

        // foto
        LinearLayout ft = Ui.coluna(this);
        ft.setGravity(Gravity.CENTER_HORIZONTAL);
        ft.setPadding(0, dp(8), 0, dp(4));
        avatar = new FrameLayout(this);
        ft.addView(avatar, Ui.lp(dp(120), dp(120)));
        TextView tf = Ui.texto(this, "📷  Escolher foto", 14, k.destaque, true);
        tf.setPadding(dp(14), dp(8), dp(14), dp(8));
        ft.addView(tf);
        View.OnClickListener escolher = v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "Foto do pet"), REQ_FOTO);
        };
        avatar.setOnClickListener(escolher);
        tf.setOnClickListener(escolher);
        col.addView(ft);
        pintarFoto();

        LinearLayout s1 = Ui.secao(this, col, "🐶  Dados do dog", null);
        nome = Campos.texto(this, s1, "Nome do dog", pet.nome, "Ex.: Thor", false);
        idade = Campos.texto(this, s1, "Idade", pet.idade, "Ex.: 3 anos", false);
        nasc = Campos.Data.criar(this, s1, "Data de nascimento do dog", pet.nascimento, true, "Toque para escolher (calcula a idade)");
        nasc.aoMudar = () -> {
            if (!nasc.valor().isEmpty()) idade.setText(Datas.idade(nasc.valor()));
        };
        raca = Campos.sugestoes(this, s1, "Raça", pet.raca, "Ex.: Shih Tzu", false, RACAS);
        Campos.rotulo(this, s1, "Sexo", false);
        sexo = new Ui.Chips(this, new String[]{"Macho", "Fêmea"}, new String[]{"♂ Macho", "♀ Fêmea"}, pet.sexo, null);
        s1.addView(sexo.view);
        Campos.rotulo(this, s1, "Porte", true);
        porte = new Ui.Chips(this, new String[]{"Mini", "Pequeno", "Médio", "Grande", "Gigante"},
                new String[]{"Mini", "Pequeno", "Médio", "Grande", "Gigante"}, pet.porte, null);
        s1.addView(porte.view);
        cor = Campos.texto(this, s1, "Pelagem / cor", pet.cor, "Ex.: Caramelo", true);
        castrado = new Switch(this);
        castrado.setText("Castrado(a)");
        castrado.setTextColor(k.texto);
        castrado.setTextSize(15);
        castrado.setChecked(pet.castrado);
        castrado.setPadding(dp(2), dp(16), 0, dp(4));
        s1.addView(castrado);
        microchip = Campos.texto(this, s1, "Microchip", pet.microchip, "Número do chip", true);
        microchip.setInputType(InputType.TYPE_CLASS_NUMBER);
        plano = Campos.texto(this, s1, "Plano de saúde", pet.plano, "Nome do plano", true);
        catPlano = Campos.texto(this, s1, "Categoria do plano", pet.categoriaPlano, "Ex.: Essencial, Plus, Premium", true);
        obs = Campos.multilinha(this, s1, "Observação", pet.obs, "Alergias, comportamento, cuidados especiais…", true);

        LinearLayout s2 = Ui.secao(this, col, "👤  Proprietário", null);
        dono = Campos.texto(this, s2, "Nome do proprietário", pet.donoNome, "Nome completo", false);
        donoNasc = Campos.Data.criar(this, s2, "Data de nascimento", pet.donoNasc, false, "Toque para escolher");
        rg = Campos.texto(this, s2, "RG", pet.rg, "00.000.000-0", false);
        rg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        cpf = Campos.cpf(this, s2, pet.cpf);
        tel = Campos.telefone(this, s2, pet.telefone);
        end = Campos.texto(this, s2, "Endereço", pet.endereco, "Rua, número, complemento", false);
        bairro = Campos.texto(this, s2, "Bairro", pet.bairro, "Ex.: Méier", false);
        cidade = Campos.texto(this, s2, "Cidade", pet.cidade, "Ex.: Rio de Janeiro", true);
        Campos.rotulo(this, s2, "Estado", false);
        s2.addView(new Ui.Chips(this, UFS, UFS, estado, id -> estado = id).view);

        LinearLayout barra = Ui.linha(this);
        barra.setPadding(dp(16), dp(10), dp(16), dp(12));
        if (!novo) {
            Button ex = Ui.botao(this, "Excluir", false);
            ex.setTextColor(0xFFEF4444);
            ex.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Excluir " + pet.nome + "?")
                    .setMessage("O cadastro, o histórico e os lembretes deste pet serão apagados.")
                    .setPositiveButton("Excluir", (d, w) -> {
                        Dados.removerPet(this, pet.id);
                        Avisos.agendar(this);
                        setResult(RESULT_FIRST_USER);
                        finish();
                    }).setNegativeButton("Cancelar", null).show());
            barra.addView(ex, Ui.lp(0, dp(54), 1f));
        }
        Button salvar = Ui.botaoGradiente(this, novo ? "Cadastrar 🐾" : "Salvar");
        salvar.setOnClickListener(v -> salvar());
        LinearLayout.LayoutParams lp = Ui.lp(0, dp(54), 2f);
        if (!novo) lp.leftMargin = dp(10);
        barra.addView(salvar, lp);
        raiz.addView(barra);
    }

    private void pintarFoto() {
        avatar.removeAllViews();
        Dados.Pet t = new Dados.Pet();
        t.foto = foto;
        t.nome = pet.nome;
        View a = Fotos.avatar(this, t, dp(120));
        avatar.addView(a, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == REQ_FOTO && res == RESULT_OK && d != null && d.getData() != null) {
            try {
                foto = Fotos.importar(this, d.getData());
                pintarFoto();
            } catch (Exception e) {
                Toast.makeText(this, "Não consegui abrir essa foto", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String t(EditText e) {
        return e.getText().toString().trim();
    }

    private void salvar() {
        if (t(nome).isEmpty()) {
            erro(nome, "Informe o nome do dog");
            return;
        }
        if (!t(cpf).isEmpty() && !Datas.cpfValido(t(cpf))) {
            new AlertDialog.Builder(this).setTitle("CPF parece inválido").setMessage("Os dígitos do CPF " + t(cpf) + " não conferem. Salvar mesmo assim?")
                    .setPositiveButton("Salvar", (d, w) -> gravar()).setNegativeButton("Corrigir", (d, w) -> cpf.requestFocus()).show();
            return;
        }
        gravar();
    }

    private void erro(EditText e, String msg) {
        e.setError(msg);
        e.requestFocus();
    }

    private void gravar() {
        String fotoAntiga = pet.foto;
        pet.nome = t(nome);
        pet.idade = t(idade);
        pet.nascimento = nasc.valor();
        pet.raca = t(raca);
        pet.sexo = sexo.valor() == null ? "" : sexo.valor();
        pet.porte = porte.valor() == null ? "" : porte.valor();
        pet.cor = t(cor);
        pet.castrado = castrado.isChecked();
        pet.microchip = t(microchip);
        pet.plano = t(plano);
        pet.categoriaPlano = t(catPlano);
        pet.obs = t(obs);
        pet.foto = foto;
        pet.donoNome = t(dono);
        pet.donoNasc = donoNasc.valor();
        pet.rg = t(rg);
        pet.cpf = t(cpf);
        pet.telefone = t(tel);
        pet.endereco = t(end);
        pet.bairro = t(bairro);
        pet.cidade = t(cidade);
        pet.estado = estado;
        Dados.salvarPet(this, pet);
        if (!fotoAntiga.isEmpty() && !fotoAntiga.equals(foto)) new java.io.File(getFilesDir(), fotoAntiga).delete();
        Avisos.agendar(this);
        Toast.makeText(this, novo ? pet.nome + " cadastrado! 🐾" : "Salvo", Toast.LENGTH_SHORT).show();
        if (novo) startActivity(new Intent(this, PetActivity.class).putExtra("pet", pet.id));
        finish();
    }
}
