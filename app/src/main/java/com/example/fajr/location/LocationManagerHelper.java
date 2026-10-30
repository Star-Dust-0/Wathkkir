package com.example.fajr.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;

/* compiled from: LocationManagerHelper.kt */
/* loaded from: classes10.dex */
public final class LocationManagerHelper {
    public static final int $stable = 8;
    private final Context context;

    public LocationManagerHelper(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        this.context = context;
    }

    public final boolean hasLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(this.context, "android.permission.ACCESS_FINE_LOCATION") == 0;
        boolean coarse = ContextCompat.checkSelfPermission(this.context, "android.permission.ACCESS_COARSE_LOCATION") == 0;
        return fine || coarse;
    }

    public final boolean isGpsEnabled() {
        Object systemService = this.context.getSystemService("location");
        LocationManager locationManager = systemService instanceof LocationManager ? (LocationManager) systemService : null;
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled("gps") || locationManager.isProviderEnabled("network");
    }

    public final Location getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }
        Object systemService = this.context.getSystemService("location");
        LocationManager locationManager = systemService instanceof LocationManager ? (LocationManager) systemService : null;
        if (locationManager == null) {
            return null;
        }
        Location location = null;
        try {
            if (locationManager.isProviderEnabled("gps")) {
                location = locationManager.getLastKnownLocation("gps");
            }
            if (location == null && locationManager.isProviderEnabled("network")) {
                location = locationManager.getLastKnownLocation("network");
            }
            if (location == null && locationManager.isProviderEnabled("passive")) {
                return locationManager.getLastKnownLocation("passive");
            }
            return location;
        } catch (SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0038 A[Catch: Exception -> 0x00ca, TryCatch #1 {Exception -> 0x00ca, blocks: (B:6:0x000f, B:8:0x002c, B:13:0x0038, B:15:0x0044, B:17:0x004a, B:18:0x004e, B:20:0x0057, B:25:0x0063, B:27:0x0068, B:32:0x0074, B:44:0x0097), top: B:5:0x000f }] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0063 A[Catch: Exception -> 0x00ca, TryCatch #1 {Exception -> 0x00ca, blocks: (B:6:0x000f, B:8:0x002c, B:13:0x0038, B:15:0x0044, B:17:0x004a, B:18:0x004e, B:20:0x0057, B:25:0x0063, B:27:0x0068, B:32:0x0074, B:44:0x0097), top: B:5:0x000f }] */
    /* JADX WARN: Removed duplicated region for block: B:32:0x0074 A[Catch: Exception -> 0x00ca, TryCatch #1 {Exception -> 0x00ca, blocks: (B:6:0x000f, B:8:0x002c, B:13:0x0038, B:15:0x0044, B:17:0x004a, B:18:0x004e, B:20:0x0057, B:25:0x0063, B:27:0x0068, B:32:0x0074, B:44:0x0097), top: B:5:0x000f }] */
    /* JADX WARN: Removed duplicated region for block: B:37:0x008f  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0095  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0097 A[Catch: Exception -> 0x00ca, TRY_LEAVE, TryCatch #1 {Exception -> 0x00ca, blocks: (B:6:0x000f, B:8:0x002c, B:13:0x0038, B:15:0x0044, B:17:0x004a, B:18:0x004e, B:20:0x0057, B:25:0x0063, B:27:0x0068, B:32:0x0074, B:44:0x0097), top: B:5:0x000f }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final String getCityNameFromCoordinates(double latitude, double longitude) {
        return getCityNameFromCoordinates(latitude, longitude, new Locale("ar"));
    }

    public final String getCityNameFromCoordinates(double latitude, double longitude, Locale locale) {
        boolean ar = "ar".equals(locale.getLanguage());
        String unknown = ar ? "\u0645\u0648\u0642\u0639\u0643 \u0627\u0644\u062d\u0627\u0644\u064a" : "Current location";
        try {
            Geocoder geocoder = new Geocoder(this.context, locale);
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                if (city == null) {
                    city = address.getSubAdminArea();
                }
                if (city == null) {
                    city = address.getAdminArea();
                }
                String country = address.getCountryName();
                if (city != null && city.length() != 0 && country != null && country.length() != 0) {
                    return ar ? city + "\u060c " + country : city + ", " + country;
                }
                if (country == null) {
                    return city == null ? unknown : city;
                }
                return country;
            }
            return unknown + " (" + String.format(Locale.US, "%.2f, %.2f", Double.valueOf(latitude), Double.valueOf(longitude)) + ")";
        } catch (Exception e) {
            e.printStackTrace();
            return unknown + " (" + String.format(Locale.US, "%.2f, %.2f", Double.valueOf(latitude), Double.valueOf(longitude)) + ")";
        }
    }

    // Uncovered later helper recovered from APK v6.d, matching Oct23 alias key rule.
    public final String getPlaceKeyFromCoordinates(double latitude, double longitude) {
        try {
            List<Address> addresses = new Geocoder(this.context, Locale.ENGLISH).getFromLocation(latitude, longitude, 1);
            if (addresses == null || addresses.isEmpty()) return null;
            Address address = addresses.get(0);
            String city = address.getLocality();
            if (city == null) city = address.getSubAdminArea();
            if (city == null) city = address.getAdminArea();
            String country = address.getCountryCode();
            if (country == null && city == null) return null;
            return "g|" + (country == null ? "" : country.trim().toLowerCase(Locale.US).replaceAll("\\s+", " "))
                + "|" + (city == null ? "" : city.trim().toLowerCase(Locale.US).replaceAll("\\s+", " "));
        } catch (Exception e) { return null; }
    }
}
