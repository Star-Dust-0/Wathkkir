package com.example.widget;



/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class W {
    private final long adhan;
    private final long iqama;
    private final String name;

    public W(String str, long j, long j2) {
        str.getClass();
        this.name = str;
        this.adhan = j;
        this.iqama = j2;
    }

    public static W copy$default(W w, String str, long j, long j2, int i, Object obj) {
        if ((i & 1) != 0) {
            str = w.name;
        }
        if ((i & 2) != 0) {
            j = w.adhan;
        }
        if ((i & 4) != 0) {
            j2 = w.iqama;
        }
        return w.copy(str, j, j2);
    }

    public final W copy(String str, long j, long j2) {
        str.getClass();
        return new W(str, j, j2);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof W)) {
            return false;
        }
        W w = (W) obj;
        return java.util.Objects.equals(this.name, w.name) && this.adhan == w.adhan && this.iqama == w.iqama;
    }

    public final long getAdhan() {
        return this.adhan;
    }

    public final long getIqama() {
        return this.iqama;
    }

    public final String getName() {
        return this.name;
    }

    public int hashCode() {
        return Long.hashCode(this.iqama) + ((Long.hashCode(this.adhan) + (this.name.hashCode() * 31)) * 31);
    }

    public String toString() {
        return "W(name=" + this.name + ", adhan=" + this.adhan + ", iqama=" + this.iqama + ")";
    }
}
