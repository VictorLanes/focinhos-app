package com.victorlanes.focinhos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** QR codes com as informações do dog (lidos por qualquer câmera, sem internet). */
public final class Qr {
    private Qr() {
    }

    public static Bitmap gerar(String texto, int px, int cor, int fundo) {
        try {
            Map<EncodeHintType, Object> h = new EnumMap<>(EncodeHintType.class);
            h.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            h.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            h.put(EncodeHintType.MARGIN, 1);
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, h);
            int n = m.getWidth();
            int esc = Math.max(1, px / n);
            int[] pix = new int[n * esc * n * esc];
            int w = n * esc;
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++) {
                    int c = m.get(x, y) ? cor : fundo;
                    for (int dy = 0; dy < esc; dy++) {
                        int base = (y * esc + dy) * w + x * esc;
                        for (int dx = 0; dx < esc; dx++) pix[base + dx] = c;
                    }
                }
            Bitmap b = Bitmap.createBitmap(pix, w, w, Bitmap.Config.ARGB_8888);
            return w == px ? b : Bitmap.createScaledBitmap(b, px, px, false);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ opções do QR da coleira

    public static final class Opcoes {
        public boolean telefone = true, local = true, saude = true, plano = false, vacinas = false, dono = true;
        public String mensagem = "Se eu estiver sozinho, estou perdido! Por favor, ligue para minha família 🙏";
    }

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("qr", Context.MODE_PRIVATE);
    }

    public static Opcoes opcoes(Context c, long petId) {
        Opcoes o = new Opcoes();
        SharedPreferences p = sp(c);
        String k = "p" + petId + "_";
        o.telefone = p.getBoolean(k + "tel", o.telefone);
        o.local = p.getBoolean(k + "local", o.local);
        o.saude = p.getBoolean(k + "saude", o.saude);
        o.plano = p.getBoolean(k + "plano", o.plano);
        o.vacinas = p.getBoolean(k + "vac", o.vacinas);
        o.dono = p.getBoolean(k + "dono", o.dono);
        o.mensagem = p.getString(k + "msg", o.mensagem);
        return o;
    }

    public static void salvar(Context c, long petId, Opcoes o) {
        String k = "p" + petId + "_";
        sp(c).edit().putBoolean(k + "tel", o.telefone).putBoolean(k + "local", o.local).putBoolean(k + "saude", o.saude)
                .putBoolean(k + "plano", o.plano).putBoolean(k + "vac", o.vacinas).putBoolean(k + "dono", o.dono)
                .putString(k + "msg", o.mensagem).apply();
    }

    /** texto curto da coleira: só o que ajuda quem encontrar o dog (sem CPF/RG/endereço completo) */
    public static String textoColeira(Context c, Dados.Pet p, Opcoes o) {
        StringBuilder s = new StringBuilder();
        s.append("🐾 Olá! Meu nome é ").append(p.nome.toUpperCase()).append("\n");
        String d = Relatorios.juntar(" · ", p.raca, p.sexo, p.idadeTexto());
        if (!d.isEmpty()) s.append(d).append("\n");
        if (!o.mensagem.trim().isEmpty()) s.append("\n").append(o.mensagem.trim()).append("\n");
        if (o.dono && !p.donoNome.isEmpty()) s.append("\n👤 Tutor(a): ").append(p.donoNome).append("\n");
        if (o.telefone && !p.telefone.isEmpty()) s.append("📞 ").append(p.telefone).append("\n");
        if (o.local) {
            String l = Relatorios.juntar(" - ", p.bairro, Relatorios.juntar("/", p.cidade, p.estado));
            if (!l.isEmpty()) s.append("📍 ").append(l).append("\n");
        }
        if (o.saude && !p.obs.isEmpty()) s.append("\n⚠️ Cuidados: ").append(corta(p.obs, 200)).append("\n");
        if (o.plano && !p.plano.isEmpty()) s.append("🛡️ Plano: ").append(Relatorios.juntar(" - ", p.plano, p.categoriaPlano)).append("\n");
        if (!p.microchip.isEmpty()) s.append("🔖 Microchip: ").append(p.microchip).append("\n");
        if (o.vacinas) vacinas(c, p, s, false);
        return s.toString().trim();
    }

    /** texto completo para a carteirinha */
    public static String textoCarteirinha(Context c, Dados.Pet p) {
        StringBuilder s = new StringBuilder();
        s.append("🪪 CARTEIRINHA PET\n🐾 ").append(p.nome.toUpperCase()).append("\n");
        linha(s, "Raça", p.raca);
        linha(s, "Sexo", p.sexo);
        linha(s, "Idade", p.idadeTexto());
        linha(s, "Nascimento", Datas.br(p.nascimento));
        linha(s, "Porte", p.porte);
        linha(s, "Pelagem", p.cor);
        s.append("Castrado: ").append(p.castrado ? "Sim" : "Não").append("\n");
        linha(s, "Microchip", p.microchip);
        linha(s, "Plano", Relatorios.juntar(" - ", p.plano, p.categoriaPlano));
        if (!p.obs.isEmpty()) s.append("⚠️ Obs.: ").append(corta(p.obs, 220)).append("\n");
        s.append("\n👤 Tutor(a)\n");
        linha(s, "Nome", p.donoNome);
        linha(s, "Telefone", p.telefone);
        linha(s, "Local", Relatorios.juntar(" - ", p.bairro, Relatorios.juntar("/", p.cidade, p.estado)));
        vacinas(c, p, s, true);
        Dados.Registro vet = null;
        for (Dados.Registro r : Dados.registros(c, p.id)) if (r.foiAoVet() && !r.tipo.equals("vacina") && Datas.diasAte(r.data) <= 0) {
            vet = r;
            break;
        }
        if (vet != null) s.append("\n🩺 Última consulta: ").append(Datas.br(vet.data)).append(vet.clinica.isEmpty() ? "" : " - " + vet.clinica).append("\n");
        s.append("\nGerado pelo app Meus Focinhos em ").append(Datas.br(Datas.hoje()));
        return s.toString();
    }

    private static void vacinas(Context c, Dados.Pet p, StringBuilder s, boolean comProxima) {
        List<Dados.Registro> vs = Relatorios.vacinasAtuais(c, p.id);
        if (vs.isEmpty()) return;
        s.append("\n💉 Vacinas\n");
        for (int i = 0; i < Math.min(8, vs.size()); i++) {
            Dados.Registro v = vs.get(i);
            s.append("• ").append(v.titulo.isEmpty() ? "Vacina" : v.titulo).append(": ").append(Datas.br(v.data));
            if (comProxima && !v.proxima.isEmpty()) s.append(" (próx. ").append(Datas.br(v.proxima)).append(")");
            s.append("\n");
        }
    }

    private static void linha(StringBuilder s, String r, String v) {
        if (v != null && !v.isEmpty()) s.append(r).append(": ").append(v).append("\n");
    }

    static String corta(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
