package com.victorlanes.focinhos;

import java.util.Calendar;
import java.util.Locale;

/** Datas guardadas como "yyyy-MM-dd" e mostradas como "dd/MM/yyyy". */
public final class Datas {
    private Datas() {
    }

    public static final String[] MESES = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};
    public static final String[] MESES_LONGOS = {"janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto",
            "setembro", "outubro", "novembro", "dezembro"};

    public static Calendar cal(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            Calendar k = Calendar.getInstance();
            k.clear();
            k.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1, Integer.parseInt(iso.substring(8, 10)));
            return k;
        } catch (Exception e) {
            return null;
        }
    }

    public static String iso(int ano, int mes0, int dia) {
        return String.format(Locale.US, "%04d-%02d-%02d", ano, mes0 + 1, dia);
    }

    public static String iso(Calendar k) {
        return iso(k.get(Calendar.YEAR), k.get(Calendar.MONTH), k.get(Calendar.DAY_OF_MONTH));
    }

    public static String hoje() {
        return iso(Calendar.getInstance());
    }

    public static String somarDias(String iso, int dias) {
        Calendar k = cal(iso);
        if (k == null) k = Calendar.getInstance();
        k.add(Calendar.DAY_OF_YEAR, dias);
        return iso(k);
    }

    public static String br(String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    /** "25 nov 2026" */
    public static String curta(String iso) {
        Calendar k = cal(iso);
        if (k == null) return "";
        return k.get(Calendar.DAY_OF_MONTH) + " " + MESES[k.get(Calendar.MONTH)] + " " + k.get(Calendar.YEAR);
    }

    /** dias de hoje até a data (negativo = já passou) */
    public static int diasAte(String iso) {
        Calendar k = cal(iso);
        if (k == null) return 0;
        Calendar h = cal(hoje());
        return (int) Math.round((k.getTimeInMillis() - h.getTimeInMillis()) / 86_400_000.0);
    }

    /** "hoje", "amanhã", "em 5 dias", "há 3 meses" ... */
    public static String relativo(String iso) {
        int d = diasAte(iso);
        if (d == 0) return "hoje";
        if (d == 1) return "amanhã";
        if (d == -1) return "ontem";
        int a = Math.abs(d);
        String t;
        if (a < 31) t = a + " dias";
        else if (a < 365) t = (a / 30) + ((a / 30) == 1 ? " mês" : " meses");
        else t = (a / 365) + ((a / 365) == 1 ? " ano" : " anos");
        return d > 0 ? "em " + t : "há " + t;
    }

    /** idade a partir do nascimento: "3 anos e 2 meses", "5 meses", "12 dias" */
    public static String idade(String nascIso) {
        Calendar n = cal(nascIso);
        if (n == null) return "";
        Calendar h = Calendar.getInstance();
        int meses = (h.get(Calendar.YEAR) - n.get(Calendar.YEAR)) * 12 + h.get(Calendar.MONTH) - n.get(Calendar.MONTH);
        if (h.get(Calendar.DAY_OF_MONTH) < n.get(Calendar.DAY_OF_MONTH)) meses--;
        if (meses < 0) return "";
        int anos = meses / 12, m = meses % 12;
        if (anos == 0 && m == 0) {
            int d = -diasAte(nascIso);
            return d + (d == 1 ? " dia" : " dias");
        }
        StringBuilder sb = new StringBuilder();
        if (anos > 0) sb.append(anos).append(anos == 1 ? " ano" : " anos");
        if (m > 0) sb.append(anos > 0 ? " e " : "").append(m).append(m == 1 ? " mês" : " meses");
        return sb.toString();
    }

    /** próximo aniversário (yyyy-MM-dd) */
    public static String proximoAniversario(String nascIso) {
        Calendar n = cal(nascIso);
        if (n == null) return "";
        Calendar h = cal(hoje());
        Calendar a = (Calendar) n.clone();
        a.set(Calendar.YEAR, h.get(Calendar.YEAR));
        if (a.before(h)) a.add(Calendar.YEAR, 1);
        return iso(a);
    }

    public static String kg(double p) {
        if (p <= 0) return "";
        String s = String.format(new Locale("pt", "BR"), "%.1f", p);
        if (s.endsWith(",0")) s = s.substring(0, s.length() - 2);
        return s + " kg";
    }

    // ------------------------------------------------------------------ CPF

    public static String soNumeros(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    public static String mascaraCpf(String s) {
        String n = soNumeros(s);
        if (n.length() > 11) n = n.substring(0, 11);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            if (i == 3 || i == 6) b.append('.');
            if (i == 9) b.append('-');
            b.append(n.charAt(i));
        }
        return b.toString();
    }

    public static boolean cpfValido(String s) {
        String n = soNumeros(s);
        if (n.length() != 11 || n.matches("(\\d)\\1{10}")) return false;
        for (int j = 9; j <= 10; j++) {
            int soma = 0;
            for (int i = 0; i < j; i++) soma += (n.charAt(i) - '0') * (j + 1 - i);
            int dv = (soma * 10) % 11;
            if (dv == 10) dv = 0;
            if (dv != n.charAt(j) - '0') return false;
        }
        return true;
    }

    public static String mascaraTelefone(String s) {
        String n = soNumeros(s);
        if (n.length() > 11) n = n.substring(0, 11);
        if (n.length() <= 2) return n.isEmpty() ? "" : "(" + n;
        String ddd = "(" + n.substring(0, 2) + ") ";
        String r = n.substring(2);
        int corte = r.length() > 8 ? 5 : 4;
        if (r.length() <= corte) return ddd + r;
        return ddd + r.substring(0, corte) + "-" + r.substring(corte);
    }
}
