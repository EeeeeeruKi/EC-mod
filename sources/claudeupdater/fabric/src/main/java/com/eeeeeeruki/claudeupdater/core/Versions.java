package com.eeeeeeruki.claudeupdater.core;

import java.util.ArrayList;
import java.util.List;

/** Compares version strings like "0.2.10", "1.0.0-beta.2" without needing strict semver. */
public final class Versions {
    private Versions() {
    }

    public static int compare(String a, String b) {
        List<String> x = split(a);
        List<String> y = split(b);
        int n = Math.max(x.size(), y.size());
        for (int i = 0; i < n; i++) {
            if (i >= x.size()) return tail(y.get(i)) ? 1 : -1;
            if (i >= y.size()) return tail(x.get(i)) ? -1 : 1;
            String p = x.get(i);
            String q = y.get(i);
            boolean pn = isNum(p);
            boolean qn = isNum(q);
            int c;
            if (pn && qn) {
                c = compareNum(p, q);
            } else if (pn) {
                c = 1; // a number beats a pre-release word: 1.0.1 > 1.0.beta
            } else if (qn) {
                c = -1;
            } else {
                c = p.compareToIgnoreCase(q);
            }
            if (c != 0) return Integer.signum(c);
        }
        return 0;
    }

    /** When one version runs out of parts: a trailing word ("beta") makes the longer one older. */
    private static boolean tail(String extra) {
        return !isNum(extra);
    }

    private static List<String> split(String v) {
        List<String> out = new ArrayList<>();
        if (v == null) return out;
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int plus = s.indexOf('+');
        if (plus >= 0) s = s.substring(0, plus); // build metadata does not count
        StringBuilder cur = new StringBuilder();
        Boolean digit = null;
        for (char ch : s.toCharArray()) {
            if (ch == '.' || ch == '-' || ch == '_') {
                if (cur.length() > 0) out.add(cur.toString());
                cur.setLength(0);
                digit = null;
                continue;
            }
            boolean d = Character.isDigit(ch);
            if (digit != null && digit != d && cur.length() > 0) { // "beta2" -> "beta", "2"
                out.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(ch);
            digit = d;
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static boolean isNum(String s) {
        if (s.isEmpty()) return false;
        for (char c : s.toCharArray()) if (!Character.isDigit(c)) return false;
        return true;
    }

    private static int compareNum(String p, String q) {
        String a = p.replaceFirst("^0+(?=.)", "");
        String b = q.replaceFirst("^0+(?=.)", "");
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }
}
