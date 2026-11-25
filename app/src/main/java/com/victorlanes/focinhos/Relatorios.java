package com.victorlanes.focinhos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carteirinha do pet (imagem) e ficha completa com histórico (PDF) para compartilhar. */
public final class Relatorios {
    private Relatorios() {
    }

    private static Paint p(int cor, float tam, boolean negrito) {
        Paint x = new Paint(Paint.ANTI_ALIAS_FLAG);
        x.setColor(cor);
        x.setTextSize(tam);
        x.setTypeface(Typeface.create("sans-serif" + (negrito ? "-medium" : ""), negrito ? Typeface.BOLD : Typeface.NORMAL));
        return x;
    }

    private static String nome(String s) {
        return s.replaceAll("[^A-Za-z0-9]", "_");
    }

    /** últimas aplicações de cada vacina */
    public static List<Dados.Registro> vacinasAtuais(Context c, long petId) {
        Map<String, Dados.Registro> m = new LinkedHashMap<>();
        for (Dados.Registro r : Dados.registros(c, petId)) {
            if (!r.tipo.equals("vacina")) continue;
            String k = r.titulo.trim().toLowerCase();
            if (!m.containsKey(k)) m.put(k, r);
        }
        return new ArrayList<>(m.values());
    }

    // ------------------------------------------------------------------ carteirinha

    public static File carteirinha(Context c, Dados.Pet pet) throws Exception {
        int W = 1200, H = 900;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fundo.setShader(new LinearGradient(0, 0, W, H, 0xFFFF8A4C, 0xFFFF5E8A, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(0, 0, W, H), 48, 48, fundo);
        Paint pata = p(0xFFFFFFFF, 120, false);
        pata.setAlpha(60);
        cv.drawText("🐾", W - 220, 135, pata);
        Paint branco = new Paint(Paint.ANTI_ALIAS_FLAG);
        branco.setColor(0xFFFFFFFF);
        cv.drawRoundRect(new RectF(36, 160, W - 36, H - 36), 36, 36, branco);
        cv.drawText("CARTEIRINHA PET", 60, 85, p(0xFFFFFFFF, 46, true));
        cv.drawText("Meus Focinhos 🐾", 60, 130, p(0xDDFFFFFF, 28, false));

        // foto
        float cx = 200, cy = 340, r = 125;
        desenharFoto(c, cv, pet, cx, cy, r);

        // QR com todas as informações
        Bitmap qr = Qr.gerar(Qr.textoCarteirinha(c, pet), 250, 0xFF2B1D14, 0xFFFFFFFF);
        float qx = W - 70 - 250, qy = 190;
        if (qr != null) {
            cv.drawBitmap(qr, qx, qy, null);
            Paint l = p(0xFF7A6558, 20, false);
            l.setTextAlign(Paint.Align.CENTER);
            cv.drawText("Escaneie para ver tudo", qx + 125, qy + 278, l);
        }

        float x = 360, larg = qx - x - 24, y = 250;
        cv.drawText(cabe(pet.nome, p(0xFF2B1D14, 60, true), larg), x, y, p(0xFF2B1D14, 60, true));
        y += 46;
        Paint sp = p(0xFF7A6558, 28, false);
        cv.drawText(cabe(juntar(" · ", pet.raca, pet.sexo, pet.idadeTexto()), sp, larg), x, y, sp);
        y += 58;
        Paint rot = p(0xFFF26B2E, 20, true), val = p(0xFF2B1D14, 27, false);
        float meia = larg / 2;
        String[][] cs = {{"NASCIMENTO", Datas.br(pet.nascimento)}, {"MICROCHIP", pet.microchip},
                {"PLANO DE SAÚDE", juntar(" · ", pet.plano, pet.categoriaPlano)}, {"TUTOR(A)", pet.donoNome}, {"TELEFONE", pet.telefone},
                {"CASTRADO", pet.castrado ? "Sim" : "Não"}};
        int i = 0;
        for (String[] f : cs) {
            if (f[1] == null || f[1].isEmpty() || i >= 6) continue;
            float fx = x + (i % 2) * meia, fy = y + (i / 2) * 74;
            cv.drawText(f[0], fx, fy, rot);
            cv.drawText(cabe(f[1], val, meia - 16), fx, fy + 32, val);
            i++;
        }

        // vacinas
        List<Dados.Registro> vs = vacinasAtuais(c, pet.id);
        Paint sep = new Paint();
        sep.setColor(0xFFF3E6DC);
        cv.drawRect(70, 588, W - 70, 590, sep);
        float vy = 630;
        cv.drawText("💉 VACINAS", 80, vy, rot);
        vy += 40;
        Paint vp = p(0xFF2B1D14, 25, false);
        if (vs.isEmpty()) cv.drawText("Nenhuma vacina registrada", 80, vy, p(0xFF7A6558, 25, false));
        for (int j = 0; j < Math.min(8, vs.size()); j++) {
            Dados.Registro v = vs.get(j);
            float vx = 80 + (j % 2) * 525, yy = vy + (j / 2) * 40;
            String t = (v.titulo.isEmpty() ? "Vacina" : v.titulo) + " · " + Datas.br(v.data) + (v.proxima.isEmpty() ? "" : " → " + Datas.br(v.proxima));
            cv.drawText(cabe(t, vp, 505), vx, yy, vp);
        }
        File out = new File(Arquivos.pasta(c), "Carteirinha_" + nome(pet.nome) + ".png");
        try (FileOutputStream fo = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
        }
        return out;
    }

    /** etiqueta para imprimir e prender na coleira / plaquinha */
    public static File etiquetaColeira(Context c, Dados.Pet pet, String texto) throws Exception {
        int W = 900, H = 1200;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fundo.setShader(new LinearGradient(0, 0, W, H, 0xFFFF8A4C, 0xFFFF5E8A, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(0, 0, W, H), 60, 60, fundo);
        desenharFoto(c, cv, pet, W / 2f, 170, 105);
        Paint nm = p(0xFFFFFFFF, 64, true);
        nm.setTextAlign(Paint.Align.CENTER);
        cv.drawText(cabe(pet.nome, nm, W - 80), W / 2f, 350, nm);
        Paint sub = p(0xEEFFFFFF, 30, false);
        sub.setTextAlign(Paint.Align.CENTER);
        cv.drawText("Se me encontrar, escaneie o código 📷", W / 2f, 400, sub);
        Paint br = new Paint(Paint.ANTI_ALIAS_FLAG);
        br.setColor(0xFFFFFFFF);
        cv.drawRoundRect(new RectF(150, 440, W - 150, H - 180), 40, 40, br);
        Bitmap qr = Qr.gerar(texto, 540, 0xFF000000, 0xFFFFFFFF);
        if (qr != null) cv.drawBitmap(qr, (W - 540) / 2f, 460, null);
        if (!pet.telefone.isEmpty()) {
            Paint t = p(0xFFFFFFFF, 46, true);
            t.setTextAlign(Paint.Align.CENTER);
            cv.drawText("📞 " + pet.telefone, W / 2f, H - 110, t);
        }
        Paint m = p(0xDDFFFFFF, 24, false);
        m.setTextAlign(Paint.Align.CENTER);
        cv.drawText("Meus Focinhos 🐾", W / 2f, H - 50, m);
        File out = new File(Arquivos.pasta(c), "QR_Coleira_" + nome(pet.nome) + ".png");
        try (FileOutputStream fo = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
        }
        return out;
    }

    static void desenharFoto(Context c, Canvas cv, Dados.Pet pet, float cx, float cy, float r) {
        Bitmap f = Fotos.carregar(c, pet.foto);
        Paint borda = new Paint(Paint.ANTI_ALIAS_FLAG);
        borda.setColor(0xFFFFD2BA);
        cv.drawCircle(cx, cy, r + 10, borda);
        if (f != null) {
            Paint ft = new Paint(Paint.ANTI_ALIAS_FLAG);
            BitmapShader sh = new BitmapShader(f, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            float e = 2 * r / Math.min(f.getWidth(), f.getHeight());
            m.setScale(e, e);
            m.postTranslate(cx - r, cy - r);
            sh.setLocalMatrix(m);
            ft.setShader(sh);
            cv.drawCircle(cx, cy, r, ft);
        } else {
            Paint vz = new Paint(Paint.ANTI_ALIAS_FLAG);
            vz.setColor(0xFFFFE6D9);
            cv.drawCircle(cx, cy, r, vz);
            Paint em = p(0xFF000000, r, false);
            em.setTextAlign(Paint.Align.CENTER);
            cv.drawText("🐶", cx, cy + r * 0.36f, em);
        }
    }

    /** corta o texto com "…" para caber na largura */
    static String cabe(String s, Paint p, float larg) {
        if (s == null) return "";
        if (p.measureText(s) <= larg) return s;
        while (s.length() > 1 && p.measureText(s + "…") > larg) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static float campo(Canvas cv, String r, String v, float x, float y, Paint pr, Paint pv) {
        if (v == null || v.isEmpty()) return y;
        cv.drawText(r, x, y, pr);
        cv.drawText(v, x, y + 34, pv);
        return y + 78;
    }

    static String juntar(String sep, String... ps) {
        StringBuilder b = new StringBuilder();
        for (String s : ps) if (s != null && !s.isEmpty()) b.append(b.length() > 0 ? sep : "").append(s);
        return b.toString();
    }

    // ------------------------------------------------------------------ ficha PDF

    private static final class Pdf {
        final PdfDocument doc = new PdfDocument();
        PdfDocument.Page pag;
        Canvas cv;
        float y;
        int n;
        static final int W = 595, H = 842, M = 40;

        void nova() {
            if (pag != null) doc.finishPage(pag);
            pag = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++n).create());
            cv = pag.getCanvas();
            y = M;
        }

        void espaco(float h) {
            if (y + h > H - M) nova();
        }

        void texto(String s, float tam, int cor, boolean negrito) {
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setTextSize(tam);
            tp.setColor(cor);
            tp.setTypeface(Typeface.create("sans-serif" + (negrito ? "-medium" : ""), negrito ? Typeface.BOLD : Typeface.NORMAL));
            StaticLayout sl = StaticLayout.Builder.obtain(s, 0, s.length(), tp, W - 2 * M).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
            espaco(sl.getHeight());
            cv.save();
            cv.translate(M, y);
            sl.draw(cv);
            cv.restore();
            y += sl.getHeight();
        }

        void linha() {
            espaco(10);
            Paint l = new Paint();
            l.setColor(0xFFEFE2D8);
            cv.drawRect(M, y + 4, W - M, y + 5, l);
            y += 10;
        }
    }

    public static File fichaPdf(Context c, Dados.Pet pet) throws Exception {
        Pdf d = new Pdf();
        d.nova();
        Paint faixa = new Paint();
        faixa.setShader(new LinearGradient(0, 0, Pdf.W, 0, 0xFFFF8A4C, 0xFFFF5E8A, Shader.TileMode.CLAMP));
        d.cv.drawRect(0, 0, Pdf.W, 90, faixa);
        Bitmap f = Fotos.carregar(c, pet.foto);
        if (f != null) d.cv.drawBitmap(Bitmap.createScaledBitmap(f, 70, 70, true), Pdf.W - Pdf.M - 70, 10, null);
        d.cv.drawText("Ficha do pet: " + pet.nome, Pdf.M, 45, p(0xFFFFFFFF, 22, true));
        d.cv.drawText("Gerada em " + Datas.br(Datas.hoje()) + " pelo app Meus Focinhos", Pdf.M, 70, p(0xEEFFFFFF, 11, false));
        d.y = 110;

        d.texto("Dados do pet", 15, 0xFFF26B2E, true);
        d.texto(juntar("\n",
                "Nome: " + pet.nome,
                rotulo("Idade", pet.idadeTexto()), rotulo("Nascimento", Datas.br(pet.nascimento)), rotulo("Raça", pet.raca),
                rotulo("Sexo", pet.sexo), rotulo("Porte", pet.porte), rotulo("Pelagem", pet.cor), "Castrado: " + (pet.castrado ? "Sim" : "Não"),
                rotulo("Microchip", pet.microchip), rotulo("Plano de saúde", pet.plano), rotulo("Categoria do plano", pet.categoriaPlano),
                rotulo("Observação", pet.obs)), 11, 0xFF2B1D14, false);
        d.linha();
        d.texto("Proprietário", 15, 0xFFF26B2E, true);
        d.texto(juntar("\n", rotulo("Nome", pet.donoNome), rotulo("Data de nascimento", Datas.br(pet.donoNasc)), rotulo("RG", pet.rg),
                rotulo("CPF", pet.cpf), rotulo("Telefone", pet.telefone), rotulo("Endereço", pet.endereco), rotulo("Bairro", pet.bairro),
                rotulo("Cidade", juntar(" - ", pet.cidade, pet.estado))), 11, 0xFF2B1D14, false);
        d.linha();

        List<Dados.Registro> vs = vacinasAtuais(c, pet.id);
        d.texto("Carteira de vacinação", 15, 0xFFF26B2E, true);
        if (vs.isEmpty()) d.texto("Nenhuma vacina registrada.", 11, 0xFF7A6558, false);
        for (Dados.Registro v : vs)
            d.texto("• " + (v.titulo.isEmpty() ? "Vacina" : v.titulo) + " — aplicada em " + Datas.br(v.data)
                    + (v.proxima.isEmpty() ? "" : " · próxima dose " + Datas.br(v.proxima)), 11, 0xFF2B1D14, false);
        d.linha();

        List<Dados.Registro> rs = Dados.registros(c, pet.id);
        long total = 0;
        d.texto("Histórico completo (" + rs.size() + " registros)", 15, 0xFFF26B2E, true);
        for (Dados.Registro r : rs) {
            total += r.valor;
            d.espaco(40);
            d.texto(Datas.br(r.data) + "  ·  " + r.resumo() + (r.valor > 0 ? "  ·  " + Dados.reais(r.valor) + (r.plano ? " (plano)" : "") : ""),
                    11.5f, 0xFF2B1D14, true);
            String det = juntar("\n", rotulo("Veterinário/clínica", r.clinica), rotulo("Procedimento", r.procedimento),
                    r.descricao, rotulo(r.tipo.equals("vacina") ? "Próxima dose" : "Retorno", Datas.br(r.proxima)),
                    r.peso > 0 && !r.tipo.equals("peso") ? "Peso: " + Datas.kg(r.peso) : "");
            if (!det.isEmpty()) d.texto(det, 10.5f, 0xFF5B4A3F, false);
            d.y += 6;
        }
        d.linha();
        d.texto("Total gasto registrado: " + Dados.reais(total), 12, 0xFF2B1D14, true);
        d.doc.finishPage(d.pag);
        File out = new File(Arquivos.pasta(c), "Ficha_" + nome(pet.nome) + ".pdf");
        try (FileOutputStream fo = new FileOutputStream(out)) {
            d.doc.writeTo(fo);
        }
        d.doc.close();
        return out;
    }

    private static String rotulo(String r, String v) {
        return v == null || v.isEmpty() ? "" : r + ": " + v;
    }
}
