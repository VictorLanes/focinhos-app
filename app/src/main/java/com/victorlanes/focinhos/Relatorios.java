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
        int W = 1200, H = 760;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fundo.setShader(new LinearGradient(0, 0, W, H, 0xFFFF8A4C, 0xFFFF5E8A, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(0, 0, W, H), 48, 48, fundo);
        // patinhas decorativas
        Paint pata = p(0x22FFFFFF, 120, false);
        cv.drawText("🐾", W - 230, 150, pata);
        cv.drawText("🐾", W - 420, H - 40, pata);
        Paint branco = new Paint(Paint.ANTI_ALIAS_FLAG);
        branco.setColor(0xFFFFFFFF);
        cv.drawRoundRect(new RectF(36, 150, W - 36, H - 36), 36, 36, branco);

        cv.drawText("CARTEIRINHA PET", 60, 80, p(0xFFFFFFFF, 44, true));
        cv.drawText("Meus Focinhos 🐾", 60, 125, p(0xDDFFFFFF, 28, false));

        // foto
        float cx = 210, cy = 360, r = 140;
        Bitmap f = Fotos.carregar(c, pet.foto);
        Paint borda = new Paint(Paint.ANTI_ALIAS_FLAG);
        borda.setColor(0xFFFFB38A);
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
            Paint em = p(0xFF000000, 140, false);
            em.setTextAlign(Paint.Align.CENTER);
            cv.drawText("🐶", cx, cy + 50, em);
        }

        float x = 400, y = 240;
        cv.drawText(pet.nome, x, y, p(0xFF2B1D14, 64, true));
        y += 50;
        String linha = juntar(" · ", pet.raca, pet.sexo, pet.idadeTexto());
        cv.drawText(linha, x, y, p(0xFF7A6558, 30, false));
        y += 60;
        Paint rot = p(0xFFF26B2E, 22, true), val = p(0xFF2B1D14, 30, false);
        y = campo(cv, "NASCIMENTO", Datas.br(pet.nascimento), x, y, rot, val);
        y = campo(cv, "PLANO DE SAÚDE", juntar(" · ", pet.plano, pet.categoriaPlano), x, y, rot, val);
        y = campo(cv, "MICROCHIP", pet.microchip, x, y, rot, val);
        campo(cv, "TUTOR(A)", juntar(" · ", pet.donoNome, pet.telefone), x, y, rot, val);

        // vacinas
        List<Dados.Registro> vs = vacinasAtuais(c, pet.id);
        float vy = 560;
        cv.drawText("💉 VACINAS", 80, vy, rot);
        vy += 40;
        if (vs.isEmpty()) cv.drawText("Nenhuma vacina registrada", 80, vy, p(0xFF7A6558, 26, false));
        Paint vp = p(0xFF2B1D14, 26, false);
        for (int i = 0; i < Math.min(4, vs.size()); i++) {
            Dados.Registro v = vs.get(i);
            float vx = 80 + (i % 2) * 540;
            float yy = vy + (i / 2) * 42;
            String t = (v.titulo.isEmpty() ? "Vacina" : v.titulo) + " · " + Datas.br(v.data) + (v.proxima.isEmpty() ? "" : "  → " + Datas.br(v.proxima));
            cv.drawText(t, vx, yy, vp);
        }
        File out = new File(Arquivos.pasta(c), "Carteirinha_" + nome(pet.nome) + ".png");
        try (FileOutputStream fo = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
        }
        return out;
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
