package com.victorlanes.focinhos;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Tela de PIN/digital. Modos: "abrir" (destrava o app), "criar" (novo PIN com confirmação), "conferir" (confirma antes de mudar ajustes).
 */
public class BloqueioActivity extends Activity {

    private String modo;
    private PinPad pad;
    private TextView titulo, sub;
    private String primeiro;
    private int erros;
    private final Handler h = new Handler(Looper.getMainLooper());
    private CancellationSignal cancelar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        modo = getIntent().getStringExtra("modo");
        if (modo == null) modo = "abrir";
        getWindow().setStatusBarColor(0);
        getWindow().setNavigationBarColor(0);

        LinearLayout col = Ui.coluna(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF3A1A0C, 0xFF1C0F0A, 0xFF2A0F1C}));
        col.setPadding(dp(24), dp(56), dp(24), dp(30));
        setContentView(col);
        Ui.aplicarInsets(col);

        TextView logo = Ui.texto(this, "🐾", 52, 0, false);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, Ui.GRADIENTE);
        g.setShape(GradientDrawable.OVAL);
        logo.setBackground(g);
        col.addView(logo, Ui.lp(dp(96), dp(96)));
        titulo = Ui.texto(this, "", 22, 0xFFFFFFFF, true);
        titulo.setGravity(Gravity.CENTER);
        titulo.setPadding(0, dp(18), 0, dp(4));
        col.addView(titulo);
        sub = Ui.texto(this, "", 14, 0xBBFFFFFF, false);
        sub.setGravity(Gravity.CENTER);
        col.addView(sub);
        col.addView(new android.view.View(this), Ui.lp(1, 0, 1f));

        int tam = "criar".equals(modo) ? getIntent().getIntExtra("tamanho", 4) : Trava.tamanho(this);
        pad = new PinPad(this, tam, this::digitou);
        col.addView(pad);
        if ("abrir".equals(modo) && Trava.digital(this) && Trava.temBiometria(this)) {
            pad.setAcaoEsquerda("👆", this::biometria);
        } else if (!"abrir".equals(modo)) {
            pad.setAcaoEsquerda("Cancelar", () -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
        textos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ("abrir".equals(modo) && Trava.digital(this) && Trava.temBiometria(this)) h.postDelayed(this::biometria, 250);
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void textos() {
        if ("criar".equals(modo)) {
            titulo.setText(primeiro == null ? "Crie um PIN" : "Confirme o PIN");
            sub.setText(primeiro == null ? "Com " + Trava.tamanho(this) + " números… escolha um que você não esqueça" : "Digite o mesmo PIN de novo");
            if (primeiro == null) sub.setText("Escolha " + getIntent().getIntExtra("tamanho", 4) + " números que você não vá esquecer");
        } else if ("conferir".equals(modo)) {
            titulo.setText("Digite seu PIN atual");
            sub.setText("Para alterar o bloqueio");
        } else {
            titulo.setText("Anjos de Focinho");
            sub.setText(Trava.digital(this) && Trava.temBiometria(this) ? "Use a digital ou digite o PIN" : "Digite o PIN para entrar");
        }
    }

    private void digitou(String pin) {
        if ("criar".equals(modo)) {
            if (primeiro == null) {
                primeiro = pin;
                pad.limpar();
                textos();
            } else if (primeiro.equals(pin)) {
                Trava.definirPin(this, pin);
                pad.sucesso();
                setResult(RESULT_OK);
                h.postDelayed(this::finish, 250);
            } else {
                primeiro = null;
                pad.erro();
                textos();
                sub.setText("Os PINs não eram iguais. Comece de novo.");
            }
            return;
        }
        if (Trava.confere(this, pin)) {
            pad.sucesso();
            liberar();
        } else {
            erros++;
            pad.erro();
            if (erros >= 5) {
                pad.setTravado(true);
                final int[] s = {30};
                Runnable r = new Runnable() {
                    public void run() {
                        if (s[0] <= 0) {
                            pad.setTravado(false);
                            erros = 3;
                            textos();
                            return;
                        }
                        sub.setText("Muitas tentativas. Aguarde " + s[0]-- + " s");
                        h.postDelayed(this, 1000);
                    }
                };
                h.post(r);
            } else sub.setText("PIN incorreto");
        }
    }

    private void liberar() {
        if ("abrir".equals(modo)) Trava.liberado = true;
        setResult(RESULT_OK);
        h.postDelayed(this::finish, 150);
    }

    private void biometria() {
        if (Build.VERSION.SDK_INT < 28 || isFinishing()) return;
        if (cancelar != null) cancelar.cancel();
        cancelar = new CancellationSignal();
        android.hardware.biometrics.BiometricPrompt.Builder b = new android.hardware.biometrics.BiometricPrompt.Builder(this)
                .setTitle("Anjos de Focinho")
                .setSubtitle("Toque no sensor para entrar")
                .setNegativeButton("Usar PIN", getMainExecutor(), (d, w) -> {
                });
        try {
            b.build().authenticate(cancelar, getMainExecutor(), new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult r) {
                    pad.sucesso();
                    liberar();
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        if ("abrir".equals(modo)) moveTaskToBack(true); // não deixa passar pelo bloqueio com o voltar
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (cancelar != null) cancelar.cancel();
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
