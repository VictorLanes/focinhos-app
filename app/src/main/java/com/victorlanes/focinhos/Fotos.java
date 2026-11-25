package com.victorlanes.focinhos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Fotos dos pets: recorta em quadrado, guarda no app e mostra em círculo. */
public final class Fotos {
    private Fotos() {
    }

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
        @Override
        protected int sizeOf(String k, Bitmap b) {
            return b.getByteCount();
        }
    };

    /** importa a imagem escolhida e devolve o caminho relativo (fotos/xxx.jpg) */
    public static String importar(Context c, Uri u) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(u)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int lado = Math.min(o.outWidth, o.outHeight), amostra = 1;
        while (lado / (amostra * 2) >= 720) amostra *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = amostra;
        Bitmap b;
        try (InputStream in = c.getContentResolver().openInputStream(u)) {
            b = BitmapFactory.decodeStream(in, null, o2);
        }
        if (b == null) throw new Exception("Imagem inválida");
        int rot = 0;
        try (InputStream in = c.getContentResolver().openInputStream(u)) {
            int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            rot = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90 : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180 : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
        } catch (Exception ignored) {
        }
        int l = Math.min(b.getWidth(), b.getHeight());
        Matrix m = new Matrix();
        float esc = Math.min(1f, 640f / l);
        m.postScale(esc, esc);
        m.postRotate(rot);
        Bitmap q = Bitmap.createBitmap(b, (b.getWidth() - l) / 2, (b.getHeight() - l) / 2, l, l, m, true);
        File dir = new File(c.getFilesDir(), "fotos");
        dir.mkdirs();
        String nome = "fotos/pet_" + System.currentTimeMillis() + ".jpg";
        try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), nome))) {
            q.compress(Bitmap.CompressFormat.JPEG, 88, out);
        }
        return nome;
    }

    public static Bitmap carregar(Context c, String rel) {
        if (rel == null || rel.isEmpty()) return null;
        Bitmap b = CACHE.get(rel);
        if (b != null) return b;
        File f = new File(c.getFilesDir(), rel);
        if (!f.exists()) return null;
        b = BitmapFactory.decodeFile(f.getAbsolutePath());
        if (b != null) CACHE.put(rel, b);
        return b;
    }

    /** avatar redondo: a foto ou uma patinha colorida */
    public static View avatar(Context c, Dados.Pet p, int tamPx) {
        FrameLayout f = new FrameLayout(c);
        f.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        f.setClipToOutline(true);
        Bitmap b = p != null ? carregar(c, p.foto) : null;
        if (b != null) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(b);
            f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            int[][] cores = {{0xFFFFB38A, 0xFFFF7A9C}, {0xFF93C5FD, 0xFFA78BFA}, {0xFF86EFAC, 0xFF2DD4BF}, {0xFFFDE68A, 0xFFFB923C}};
            int[] cs = cores[(int) ((p != null ? Math.abs(p.nome.hashCode()) : 0) % cores.length)];
            f.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, cs));
            TextView t = new TextView(c);
            t.setText("🐶");
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, tamPx * 0.5f);
            t.setGravity(Gravity.CENTER);
            f.addView(t, new FrameLayout.LayoutParams(-1, -1));
        }
        f.setLayoutParams(new android.widget.LinearLayout.LayoutParams(tamPx, tamPx));
        return f;
    }
}
