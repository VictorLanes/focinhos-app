package com.victorlanes.focinhos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Armazena pets, registros do histórico e lembretes (tudo no próprio celular). */
public final class Dados {

    private Dados() {
    }

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("focinhos", Context.MODE_PRIVATE);
    }

    public static synchronized long novoId(Context c) {
        long id = Math.max(sp(c).getLong("proxId", 1), System.currentTimeMillis() / 1000);
        sp(c).edit().putLong("proxId", id + 1).apply();
        return id;
    }

    // ================================================================== PET

    public static final class Pet implements Js {
        public long id;
        public String nome = "", idade = "", nascimento = "", raca = "", sexo = "", porte = "", cor = "", microchip = "";
        public boolean castrado;
        public String plano = "", categoriaPlano = "", obs = "", foto = "";
        // proprietário
        public String donoNome = "", donoNasc = "", rg = "", cpf = "", telefone = "", endereco = "", bairro = "", cidade = "", estado = "";
        public long criadoEm;

        public JSONObject json() throws JSONException {
            return new JSONObject().put("id", id).put("nome", nome).put("idade", idade).put("nascimento", nascimento).put("raca", raca)
                    .put("sexo", sexo).put("porte", porte).put("cor", cor).put("microchip", microchip).put("castrado", castrado)
                    .put("plano", plano).put("categoriaPlano", categoriaPlano).put("obs", obs).put("foto", foto)
                    .put("donoNome", donoNome).put("donoNasc", donoNasc).put("rg", rg).put("cpf", cpf).put("telefone", telefone)
                    .put("endereco", endereco).put("bairro", bairro).put("cidade", cidade).put("estado", estado).put("criadoEm", criadoEm);
        }

        public static Pet de(JSONObject o) {
            Pet p = new Pet();
            p.id = o.optLong("id");
            p.nome = o.optString("nome");
            p.idade = o.optString("idade");
            p.nascimento = o.optString("nascimento");
            p.raca = o.optString("raca");
            p.sexo = o.optString("sexo");
            p.porte = o.optString("porte");
            p.cor = o.optString("cor");
            p.microchip = o.optString("microchip");
            p.castrado = o.optBoolean("castrado");
            p.plano = o.optString("plano");
            p.categoriaPlano = o.optString("categoriaPlano");
            p.obs = o.optString("obs");
            p.foto = o.optString("foto");
            p.donoNome = o.optString("donoNome");
            p.donoNasc = o.optString("donoNasc");
            p.rg = o.optString("rg");
            p.cpf = o.optString("cpf");
            p.telefone = o.optString("telefone");
            p.endereco = o.optString("endereco");
            p.bairro = o.optString("bairro");
            p.cidade = o.optString("cidade");
            p.estado = o.optString("estado");
            p.criadoEm = o.optLong("criadoEm");
            return p;
        }

        /** idade calculada pela data de nascimento (se houver) ou o texto digitado */
        public String idadeTexto() {
            if (!nascimento.isEmpty()) {
                String t = Datas.idade(nascimento);
                if (!t.isEmpty()) return t;
            }
            if (idade.isEmpty()) return "";
            return idade.matches("\\d+") ? idade + (idade.equals("1") ? " ano" : " anos") : idade;
        }
    }

    private static <T> List<T> lista(Context c, String chave, Conv<T> conv) {
        List<T> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(chave, "[]"));
            for (int i = 0; i < a.length(); i++) l.add(conv.de(a.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
        return l;
    }

    interface Conv<T> {
        T de(JSONObject o) throws JSONException;
    }

    interface Js {
        JSONObject json() throws JSONException;
    }

    private static void gravar(Context c, String chave, List<? extends Js> l) {
        JSONArray a = new JSONArray();
        try {
            for (Js x : l) a.put(x.json());
        } catch (JSONException ignored) {
        }
        sp(c).edit().putString(chave, a.toString()).apply();
    }

    public static synchronized List<Pet> pets(Context c) {
        List<Pet> l = lista(c, "pets", Pet::de);
        Collections.sort(l, (a, b) -> a.nome.compareToIgnoreCase(b.nome));
        return l;
    }

    public static Pet pet(Context c, long id) {
        for (Pet p : pets(c)) if (p.id == id) return p;
        return null;
    }

    public static synchronized void salvarPet(Context c, Pet p) {
        List<Pet> l = lista(c, "pets", Pet::de);
        if (p.id == 0) {
            p.id = novoId(c);
            p.criadoEm = System.currentTimeMillis();
        }
        boolean achou = false;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id == p.id) {
            l.set(i, p);
            achou = true;
        }
        if (!achou) l.add(p);
        gravar(c, "pets", l);
    }

    public static synchronized void removerPet(Context c, long id) {
        Pet p = pet(c, id);
        if (p != null && !p.foto.isEmpty()) new File(c.getFilesDir(), p.foto).delete();
        List<Pet> l = lista(c, "pets", Pet::de);
        l.removeIf(x -> x.id == id);
        gravar(c, "pets", l);
        List<Registro> r = lista(c, "registros", Registro::de);
        r.removeIf(x -> x.petId == id);
        gravar(c, "registros", r);
        List<Lembrete> m = lista(c, "lembretes", Lembrete::de);
        m.removeIf(x -> x.petId == id);
        gravar(c, "lembretes", m);
    }

    // ================================================================== REGISTRO DO HISTÓRICO

    public static final String[] TIPOS = {"consulta", "vacina", "procedimento", "exame", "medicamento", "banho", "peso", "outro"};
    public static final String[] NOMES_TIPOS = {"Consulta", "Vacina", "Procedimento", "Exame", "Medicamento", "Banho e tosa", "Peso", "Outro"};
    public static final String[] EMOJI_TIPOS = {"🩺", "💉", "🏥", "🔬", "💊", "🛁", "⚖️", "📝"};
    public static final int[] COR_TIPOS = {0xFF3B82F6, 0xFF10B981, 0xFFEF4444, 0xFF8B5CF6, 0xFFF59E0B, 0xFF06B6D4, 0xFF64748B, 0xFFA16207};

    public static int indiceTipo(String t) {
        for (int i = 0; i < TIPOS.length; i++) if (TIPOS[i].equals(t)) return i;
        return TIPOS.length - 1;
    }

    public static final class Registro implements Js {
        public long id, petId;
        public String tipo = "consulta";
        public String data = "";        // yyyy-MM-dd
        public String titulo = "";      // qual vacina / qual procedimento / motivo
        public String clinica = "";     // veterinário ou clínica
        public String procedimento = ""; // procedimento feito na consulta (opcional)
        public String descricao = "";
        public long valor;              // centavos
        public boolean plano;           // coberto pelo plano de saúde
        public String proxima = "";     // próxima dose / retorno (yyyy-MM-dd)
        public double peso;             // kg

        public JSONObject json() throws JSONException {
            return new JSONObject().put("id", id).put("petId", petId).put("tipo", tipo).put("data", data).put("titulo", titulo)
                    .put("clinica", clinica).put("procedimento", procedimento).put("descricao", descricao).put("valor", valor)
                    .put("plano", plano).put("proxima", proxima).put("peso", peso);
        }

        public static Registro de(JSONObject o) {
            Registro r = new Registro();
            r.id = o.optLong("id");
            r.petId = o.optLong("petId");
            r.tipo = o.optString("tipo", "consulta");
            r.data = o.optString("data");
            r.titulo = o.optString("titulo");
            r.clinica = o.optString("clinica");
            r.procedimento = o.optString("procedimento");
            r.descricao = o.optString("descricao");
            r.valor = o.optLong("valor");
            r.plano = o.optBoolean("plano");
            r.proxima = o.optString("proxima");
            r.peso = o.optDouble("peso", 0);
            return r;
        }

        public String nomeTipo() {
            return NOMES_TIPOS[indiceTipo(tipo)];
        }

        public String emoji() {
            return EMOJI_TIPOS[indiceTipo(tipo)];
        }

        public int cor() {
            return COR_TIPOS[indiceTipo(tipo)];
        }

        /** título que aparece na linha do tempo */
        public String resumo() {
            switch (tipo) {
                case "vacina":
                    return titulo.isEmpty() ? "Vacina" : "Vacina " + titulo;
                case "peso":
                    return "Pesagem: " + Datas.kg(peso);
                default:
                    return titulo.isEmpty() ? nomeTipo() : nomeTipo() + " · " + titulo;
            }
        }

        /** é uma ida ao veterinário (para "última ida ao vet") */
        public boolean foiAoVet() {
            return tipo.equals("consulta") || tipo.equals("vacina") || tipo.equals("procedimento") || tipo.equals("exame");
        }
    }

    /** registros de um pet, do mais recente para o mais antigo (petId 0 = todos) */
    public static synchronized List<Registro> registros(Context c, long petId) {
        List<Registro> l = lista(c, "registros", Registro::de);
        if (petId != 0) l.removeIf(r -> r.petId != petId);
        Collections.sort(l, (a, b) -> {
            int x = b.data.compareTo(a.data);
            return x != 0 ? x : Long.compare(b.id, a.id);
        });
        return l;
    }

    public static Registro registro(Context c, long id) {
        for (Registro r : lista(c, "registros", Registro::de)) if (r.id == id) return r;
        return null;
    }

    public static synchronized void salvarRegistro(Context c, Registro r) {
        List<Registro> l = lista(c, "registros", Registro::de);
        if (r.id == 0) r.id = novoId(c);
        boolean achou = false;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id == r.id) {
            l.set(i, r);
            achou = true;
        }
        if (!achou) l.add(r);
        gravar(c, "registros", l);
    }

    public static synchronized void removerRegistro(Context c, long id) {
        List<Registro> l = lista(c, "registros", Registro::de);
        l.removeIf(x -> x.id == id);
        gravar(c, "registros", l);
        List<Lembrete> m = lista(c, "lembretes", Lembrete::de);
        m.removeIf(x -> x.registroId == id && !x.feito);
        gravar(c, "lembretes", m);
    }

    // ================================================================== LEMBRETES

    public static final class Lembrete implements Js {
        public long id, petId, registroId;
        public String titulo = "";
        public String data = "";   // yyyy-MM-dd
        public int hora = 9, minuto = 0;
        public int repetirDias;    // 0 = não repete
        public boolean feito;

        public JSONObject json() throws JSONException {
            return new JSONObject().put("id", id).put("petId", petId).put("registroId", registroId).put("titulo", titulo).put("data", data)
                    .put("hora", hora).put("minuto", minuto).put("repetirDias", repetirDias).put("feito", feito);
        }

        public static Lembrete de(JSONObject o) {
            Lembrete l = new Lembrete();
            l.id = o.optLong("id");
            l.petId = o.optLong("petId");
            l.registroId = o.optLong("registroId");
            l.titulo = o.optString("titulo");
            l.data = o.optString("data");
            l.hora = o.optInt("hora", 9);
            l.minuto = o.optInt("minuto");
            l.repetirDias = o.optInt("repetirDias");
            l.feito = o.optBoolean("feito");
            return l;
        }

        public long quando() {
            Calendar k = Datas.cal(data);
            if (k == null) return 0;
            k.set(Calendar.HOUR_OF_DAY, hora);
            k.set(Calendar.MINUTE, minuto);
            return k.getTimeInMillis();
        }
    }

    /** lembretes pendentes primeiro (por data), depois os feitos (petId 0 = todos) */
    public static synchronized List<Lembrete> lembretes(Context c, long petId) {
        List<Lembrete> l = lista(c, "lembretes", Lembrete::de);
        if (petId != 0) l.removeIf(r -> r.petId != petId);
        Collections.sort(l, (a, b) -> {
            if (a.feito != b.feito) return a.feito ? 1 : -1;
            return a.feito ? b.data.compareTo(a.data) : Long.compare(a.quando(), b.quando());
        });
        return l;
    }

    public static Lembrete lembrete(Context c, long id) {
        for (Lembrete r : lista(c, "lembretes", Lembrete::de)) if (r.id == id) return r;
        return null;
    }

    public static Lembrete lembreteDoRegistro(Context c, long registroId) {
        for (Lembrete r : lista(c, "lembretes", Lembrete::de)) if (r.registroId == registroId && !r.feito) return r;
        return null;
    }

    public static synchronized void salvarLembrete(Context c, Lembrete r) {
        List<Lembrete> l = lista(c, "lembretes", Lembrete::de);
        if (r.id == 0) r.id = novoId(c);
        boolean achou = false;
        for (int i = 0; i < l.size(); i++) if (l.get(i).id == r.id) {
            l.set(i, r);
            achou = true;
        }
        if (!achou) l.add(r);
        gravar(c, "lembretes", l);
    }

    public static synchronized void removerLembrete(Context c, long id) {
        List<Lembrete> l = lista(c, "lembretes", Lembrete::de);
        l.removeIf(x -> x.id == id);
        gravar(c, "lembretes", l);
    }

    // ================================================================== backup

    public static JSONObject exportar(Context c) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("app", "AnjosDeFocinho").put("versao", 1);
        o.put("pets", new JSONArray(sp(c).getString("pets", "[]")));
        o.put("registros", new JSONArray(sp(c).getString("registros", "[]")));
        o.put("lembretes", new JSONArray(sp(c).getString("lembretes", "[]")));
        JSONObject fotos = new JSONObject();
        for (Pet p : pets(c)) {
            if (p.foto.isEmpty()) continue;
            File f = new File(c.getFilesDir(), p.foto);
            if (!f.exists()) continue;
            try {
                fotos.put(p.foto, android.util.Base64.encodeToString(java.nio.file.Files.readAllBytes(f.toPath()), android.util.Base64.NO_WRAP));
            } catch (Exception ignored) {
            }
        }
        o.put("fotos", fotos);
        return o;
    }

    public static void importar(Context c, JSONObject o) throws Exception {
        if (!o.has("pets")) throw new Exception("Arquivo não é um backup do Anjos de Focinho");
        JSONObject fotos = o.optJSONObject("fotos");
        if (fotos != null) {
            File dir = new File(c.getFilesDir(), "fotos");
            dir.mkdirs();
            java.util.Iterator<String> it = fotos.keys();
            while (it.hasNext()) {
                String nome = it.next();
                if (!nome.startsWith("fotos/") || nome.contains("..")) continue;
                byte[] b = android.util.Base64.decode(fotos.getString(nome), android.util.Base64.NO_WRAP);
                java.nio.file.Files.write(new File(c.getFilesDir(), nome).toPath(), b);
            }
        }
        sp(c).edit().putString("pets", o.getJSONArray("pets").toString())
                .putString("registros", o.optJSONArray("registros") != null ? o.getJSONArray("registros").toString() : "[]")
                .putString("lembretes", o.optJSONArray("lembretes") != null ? o.getJSONArray("lembretes").toString() : "[]").apply();
    }

    public static String tema(Context c) {
        return sp(c).getString("tema", "sistema");
    }

    public static void salvarTema(Context c, String t) {
        sp(c).edit().putString("tema", t).apply();
    }

    // ================================================================== dinheiro

    private static final NumberFormat REAL = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public static String reais(long centavos) {
        return REAL.format(centavos / 100.0).replace(' ', ' ');
    }

    /** "1.234,56" / "R$ 80" / "80.5" → centavos (-1 se inválido) */
    public static long centavos(String s) {
        s = s.replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (s.isEmpty()) return 0;
        if (s.contains(",")) s = s.replace(".", "").replace(',', '.');
        try {
            return Math.round(Double.parseDouble(s) * 100);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String valorEditavel(long centavos) {
        if (centavos == 0) return "";
        return String.format(new Locale("pt", "BR"), "%.2f", centavos / 100.0);
    }
}
