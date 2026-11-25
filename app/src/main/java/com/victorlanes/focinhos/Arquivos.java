package com.victorlanes.focinhos;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Entrega arquivos da pasta cache/compartilhar para o WhatsApp, e-mail etc. */
public class Arquivos extends ContentProvider {

    public static final String AUTORIDADE = "com.victorlanes.focinhos.arquivos";

    public static File pasta(Context c) {
        File d = new File(c.getCacheDir(), "compartilhar");
        d.mkdirs();
        return d;
    }

    public static Uri uri(File f) {
        return Uri.parse("content://" + AUTORIDADE + "/" + Uri.encode(f.getName()));
    }

    private File arquivo(Uri u) throws FileNotFoundException {
        String nome = u.getLastPathSegment();
        if (nome == null || nome.contains("/") || nome.contains("..")) throw new FileNotFoundException();
        File f = new File(pasta(getContext()), nome);
        if (!f.exists()) throw new FileNotFoundException();
        return f;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri u, String modo) throws FileNotFoundException {
        return ParcelFileDescriptor.open(arquivo(u), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri u, String[] proj, String sel, String[] args, String ordem) {
        try {
            File f = arquivo(u);
            MatrixCursor m = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
            m.addRow(new Object[]{f.getName(), f.length()});
            return m;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public String getType(Uri u) {
        String n = u.getLastPathSegment();
        if (n == null) return null;
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg")) return "image/jpeg";
        if (n.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri u, ContentValues v) {
        return null;
    }

    @Override
    public int delete(Uri u, String s, String[] a) {
        return 0;
    }

    @Override
    public int update(Uri u, ContentValues v, String s, String[] a) {
        return 0;
    }
}
