package com.victorlanes.focinhos;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.WindowManager;

import java.security.MessageDigest;
import java.security.SecureRandom;

/** Bloqueio do app com PIN e/ou digital. */
public final class Trava {
    private Trava() {
    }

    /** volta sem pedir PIN se saiu do app há menos disso (ex.: escolher foto, compartilhar) */
    private static final long CARENCIA = 60_000;
    private static int visiveis;
    private static long saiuEm;
    static boolean liberado;

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("trava", Context.MODE_PRIVATE);
    }

    public static boolean ativa(Context c) {
        return !sp(c).getString("hash", "").isEmpty();
    }

    public static int tamanho(Context c) {
        return sp(c).getInt("tamanho", 4);
    }

    public static boolean digital(Context c) {
        return sp(c).getBoolean("digital", false);
    }

    public static void definirDigital(Context c, boolean on) {
        sp(c).edit().putBoolean("digital", on).apply();
    }

    static String hash(String sal, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((sal + ":" + pin).getBytes("UTF-8"));
            for (int i = 0; i < 20000; i++) h = md.digest(h);
            StringBuilder b = new StringBuilder();
            for (byte x : h) b.append(String.format("%02x", x));
            return b.toString();
        } catch (Exception e) {
            return pin;
        }
    }

    public static void definirPin(Context c, String pin) {
        byte[] s = new byte[16];
        new SecureRandom().nextBytes(s);
        StringBuilder sal = new StringBuilder();
        for (byte x : s) sal.append(String.format("%02x", x));
        sp(c).edit().putString("sal", sal.toString()).putString("hash", hash(sal.toString(), pin)).putInt("tamanho", pin.length()).apply();
        liberado = true;
    }

    public static boolean confere(Context c, String pin) {
        return hash(sp(c).getString("sal", ""), pin).equals(sp(c).getString("hash", ""));
    }

    public static void desativar(Context c) {
        sp(c).edit().clear().apply();
    }

    /** o celular tem digital/rosto cadastrado? */
    public static boolean temBiometria(Context c) {
        if (Build.VERSION.SDK_INT < 28) return false;
        if (Build.VERSION.SDK_INT >= 29) {
            android.hardware.biometrics.BiometricManager bm = c.getSystemService(android.hardware.biometrics.BiometricManager.class);
            if (bm == null) return false;
            int r = Build.VERSION.SDK_INT >= 30 ? bm.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK) : bm.canAuthenticate();
            return r == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS;
        }
        return c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }

    /** registrado no App: tranca quando o app volta do segundo plano */
    static void instalar(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity a, Bundle b) {
                if (ativa(a)) a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); // esconde nos recentes/prints
            }

            public void onActivityStarted(Activity a) {
                if (visiveis++ == 0 && !(a instanceof BloqueioActivity)) {
                    if (saiuEm == 0 || SystemClock.elapsedRealtime() - saiuEm > CARENCIA) liberado = false;
                }
                if (ativa(a) && !liberado && !(a instanceof BloqueioActivity)) {
                    a.startActivity(new Intent(a, BloqueioActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                }
            }

            public void onActivityResumed(Activity a) {
            }

            public void onActivityPaused(Activity a) {
            }

            public void onActivityStopped(Activity a) {
                if (--visiveis <= 0) {
                    visiveis = 0;
                    saiuEm = SystemClock.elapsedRealtime();
                }
            }

            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            public void onActivityDestroyed(Activity a) {
            }
        });
    }
}
