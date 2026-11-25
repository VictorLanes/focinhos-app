package com.victorlanes.focinhos;

import android.app.Activity;
import android.view.View;

/** Tema claro/escuro da janela e cor dos ícones da barra de status. */
public final class Visual {
    private Visual() {
    }

    /** chamar no onCreate, antes de montar a tela */
    public static void preparar(Activity a, boolean cabecalhoColorido) {
        Ui.tema = Dados.tema(a);
        boolean escuro = Ui.escuro(a);
        a.setTheme(escuro ? R.style.Tema_Escuro : R.style.Tema_Claro);
        a.getWindow().setStatusBarColor(0);
        a.getWindow().setNavigationBarColor(0);
        View d = a.getWindow().getDecorView();
        int f = d.getSystemUiVisibility();
        f = (!escuro && !cabecalhoColorido) ? f | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : f & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        f = !escuro ? f | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : f & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        d.setSystemUiVisibility(f);
    }
}
