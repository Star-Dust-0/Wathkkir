package com.example.utils;

/** String helpers that R8 had extracted from string-concatenation sites in the shipped build. */
public final class Str {
    private Str() {}
    public static int coerce(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }
    public static String V(String a, String b, String c) { return a + b + c; }
    public static String L(String a, String b, String c) { return a + b + c; }
    public static String K(int i, String s) { return s + i; }
    public static String O(StringBuilder sb, String a, String b) { sb.append(a); sb.append(b); return sb.toString(); }
    public static java.util.Map X(java.util.Map.Entry... e) {
        java.util.LinkedHashMap m = new java.util.LinkedHashMap();
        for (java.util.Map.Entry x : e) m.put(x.getKey(), x.getValue());
        return m;
    }
    public static int hash(int i, int i2, String s) { return (s.hashCode() + i) * i2; }
    public static String cat(int i, String s) { return i + s; }
    public static java.util.List single(Object o) { return java.util.Collections.singletonList(o); }
    public static java.util.ArrayList plus(java.util.Collection a, java.util.List b) {
        java.util.ArrayList r = new java.util.ArrayList(a.size() + b.size());
        r.addAll(a); r.addAll(b); return r;
    }
    public static java.util.List sorted(java.lang.Iterable it, java.util.Comparator c) {
        java.util.ArrayList r = new java.util.ArrayList();
        for (Object o : it) r.add(o);
        java.util.Collections.sort(r, c);
        return r;
    }
    public static boolean isBlank(CharSequence c) {
        for (int i = 0; i < c.length(); i++) if (!Character.isWhitespace(c.charAt(i))) return false;
        return true;
    }
}
