package com.example.lockwidgets;

import android.os.BaseBundle;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.reflect.Constructor;
import android.icu.util.IslamicCalendar;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Paint;
import android.graphics.BitmapFactory;
import android.content.res.AssetManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import com.example.fajr.calculation.CalculationMethod;
import com.example.fajr.calculation.PrayerAdjustments;
import com.example.fajr.data.FajrPreferences;
import com.example.lockwidgets.LockWidgets;
import com.example.widget.WidgetUpdater;
import com.example.utils.Str;
import com.example.utils.Digits;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public class LockWidgets extends AppWidgetProvider {
    private static SharedPreferences.OnSharedPreferenceChangeListener languageListener;
    private static final Class<?>[] TYPES = {Day.class, Tasbih.class, Hijri.class, Gregorian.class, Next.class, Bar.class, Countdown.class, CountdownClean.class};
    private static long lastDayKey = -1;
    private static int lastPhase = Integer.MIN_VALUE;
    private static boolean lastIqama = false;
    private static long tcDay = -1;
    private static double tcLat = Double.NaN;
    private static double tcLng = Double.NaN;
    private static int tcMethod = -1;
    private static long[] tcVal = null;

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Bar extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Countdown extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class CountdownClean extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Day extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Gregorian extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Hijri extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Next extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Prayers extends LockWidgets {
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static class Tasbih extends LockWidgets {
    }

    private static String action(Context context, String str) {
        return context.getPackageName() + ".lockwidgets." + str;
    }

    private static boolean active(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (Class<?> cls : TYPES) {
            if (appWidgetManager.getAppWidgetIds(new ComponentName(context, cls)).length > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyActive(Context context) {
        boolean active = active(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        String[] strArr = {"Hero", "NextPrayer", "PrayerBar", "Clock", "Pill", "DayPrayer", "OneUI", "Gregorian", "Hijri", "Iqama", "IqamaSmall", "Schedule", "WideBar"};
        for (int i = 0; i < 13; i++) {
            active |= appWidgetManager.getAppWidgetIds(new ComponentName(context, Str.V("com.example.widget.", strArr[i], "WidgetProvider"))).length > 0;
        }
        return active;
    }

    private static boolean arabic(Context context) {
        return lang(context).startsWith("ar");
    }

    private static PendingIntent boundaryPi(Context context) {
        return PendingIntent.getBroadcast(context, 700002, new Intent(context, (Class<?>) LockWidgets.class).setAction(action(context, "BOUNDARY")), 201326592);
    }

    private static void centered(Canvas canvas, String str, float f, float f2, TextPaint textPaint, float f3) {
        if (HomeWidgets.capture(str, f, f2, f3, textPaint.getTextSize(), textPaint.getColor())) {
            return;
        }
        String b = Digits.forWidget(str);
        float measureText = textPaint.measureText(b);
        if (measureText > f3) {
            textPaint.setTextSize((textPaint.getTextSize() * f3) / measureText);
        }
        StaticLayout build = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, (int) f3).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build();
        canvas.save();
        canvas.translate(f - (f3 / 2.0f), f2 - (build.getHeight() / 2.0f));
        build.draw(canvas);
        canvas.restore();
    }

    public static String errText(String str) {
        return str.startsWith("ar") ? "تعذر حساب الصلاة" : str.startsWith("fr") ? "Heure de prière indisponible" : str.startsWith("de") ? "Gebetszeit nicht verfügbar" : str.startsWith("es") ? "Hora de oración no disponible" : str.startsWith("it") ? "Ora della preghiera non disponibile" : str.startsWith("tr") ? "Namaz vakti hesaplanamadı" : str.startsWith("ur") ? "نماز کا وقت دستیاب نہیں" : str.startsWith("id") ? "Waktu salat tidak tersedia" : str.startsWith("ru") ? "Время молитвы недоступно" : str.startsWith("pt") ? "Hora de oração indisponível" : "Prayer time unavailable";
    }

    private static Typeface font(Context context, String str) {
        return Typeface.createFromAsset(context.getAssets(), "lockfonts/" + str + ".ttf");
    }

    private static int ink(Context context) {
        return night(context) ? -1 : -14935010;
    }

    private static int inkSoft(Context context) {
        return night(context) ? -419430401 : -434365410;
    }

    public static long[] iqamaWindow(long[] jArr, long[] jArr2, long j) {
        return PrayerPhase.window(jArr, jArr2, j, Calendar.getInstance().get(7) == 6);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$watchLanguage$0(Context context, SharedPreferences sharedPreferences, String str) {
        if ("language".equals(str)) {
            updateAll(context);
        } else if ("digits_style".equals(str)) {
            updateAll(context);
            HomeWidgets.updateAll(context);
        }
    }

    private static String lang(Context context) {
        String string = context.getSharedPreferences("app_prefs", 0).getString("language", "ar");
        return string == null ? "ar" : string;
    }

    private static void lines(Canvas canvas, int i, int i2, float f, String[] strArr, float[] fArr, Typeface[] typefaceArr, int i3, int i4) {
        int length = strArr.length;
        StaticLayout[] staticLayoutArr = new StaticLayout[length];
        String[] strArr2 = new String[strArr.length];
        int i5 = 0;
        for (int i6 = 0; i6 < strArr.length; i6++) {
            strArr2[i6] = Digits.forWidget(strArr[i6]);
        }
        int i7 = 0;
        float f2 = 0.0f;
        while (i7 < strArr.length) {
            TextPaint paint = paint(typefaceArr[i7], fArr[i7], f, i7 == 0 ? i3 : i4);
            float measureText = paint.measureText(strArr2[i7]);
            float f3 = i - (4.0f * f);
            if (measureText > f3) {
                paint.setTextSize((paint.getTextSize() * f3) / measureText);
            }
            String str = strArr2[i7];
            staticLayoutArr[i7] = StaticLayout.Builder.obtain(str, 0, str.length(), paint, i).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();
            f2 += staticLayoutArr[i7].getHeight();
            i7++;
        }
        float max = Math.max(0.0f, (i2 - ((((strArr.length - 1) * 2) * f) + f2)) / 2.0f);
        while (i5 < length) {
            StaticLayout staticLayout = staticLayoutArr[i5];
            float f4 = i;
            if (!HomeWidgets.capture(strArr[i5], f4 / 2.0f, (staticLayout.getHeight() / 2.0f) + max, f4 - (f * 4.0f), fArr[i5] * f, i5 == 0 ? i3 : i4)) {
                canvas.save();
                canvas.translate(0.0f, max);
                staticLayout.draw(canvas);
                canvas.restore();
            }
            max += (f * 2.0f) + staticLayout.getHeight();
            i5++;
        }
    }

    private static Locale locale(Context context) {
        return new Locale(lang(context));
    }

    private static boolean night(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    private static String number(long j, Locale locale) {
        NumberFormat integerInstance = NumberFormat.getIntegerInstance(locale);
        integerInstance.setGroupingUsed(false);
        return integerInstance.format(j);
    }

    private static TextPaint paint(Typeface typeface, float f, float f2, int i) {
        TextPaint textPaint = new TextPaint(1);
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(f * f2);
        textPaint.setColor(i);
        textPaint.setShadowLayer(3.0f * f2, 0.0f, f2, Color.blue(i) + (Color.green(i) + Color.red(i)) > 384 ? 1711276032 : 1728053247);
        return textPaint;
    }

    private static synchronized boolean phaseChanged(Context context) {
        boolean z;
        synchronized (LockWidgets.class) {
            try {
                try {
                    Calendar calendar = Calendar.getInstance();
                    long[] timesCached = timesCached(context, calendar);
                    long currentTimeMillis = System.currentTimeMillis();
                    Calendar calendar2 = (Calendar) calendar.clone();
                    int i = -1;
                    calendar2.add(5, -1);
                    boolean z2 = iqamaWindow(timesCached, timesCached(context, calendar2), currentTimeMillis) != null;
                    int i2 = 0;
                    while (true) {
                        if (i2 < 6) {
                            if (i2 != 1 && timesCached[i2] > currentTimeMillis) {
                                i = i2;
                                break;
                            }
                            i2++;
                        } else {
                            break;
                        }
                    }
                    if (i < 0) {
                        i = 6;
                    }
                    long j = (calendar.get(1) * 1000) + calendar.get(6);
                    int i3 = lastPhase;
                    z = (i3 != Integer.MIN_VALUE && j == lastDayKey && i == i3 && z2 == lastIqama) ? false : true;
                    lastDayKey = j;
                    lastPhase = i;
                    lastIqama = z2;
                } catch (Exception e) {
                    Log.w("WathakkerLock", "phase tracking failed - full update", e);
                    return true;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return z;
    }

    private static PendingIntent pulse(Context context) {
        return PendingIntent.getBroadcast(context, 700001, new Intent(context, (Class<?>) LockWidgets.class).setAction(action(context, "TICK")), 201326592);
    }

    public static Bitmap render(Context context, Class<?> cls, int i) {
        return renderSize(context, cls, i, false);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:144:0x04b0 A[Catch: Exception -> 0x03d8, TryCatch #5 {Exception -> 0x03d8, blocks: (B:340:0x03c6, B:123:0x040d, B:127:0x041c, B:129:0x0455, B:142:0x04ab, B:144:0x04b0, B:326:0x04bd, B:327:0x04c4, B:328:0x047c, B:330:0x0495, B:117:0x03e6, B:122:0x03f3, B:335:0x03f1, B:336:0x03fa), top: B:115:0x03ba }] */
    /* JADX WARN: Removed duplicated region for block: B:151:0x04cf  */
    /* JADX WARN: Removed duplicated region for block: B:180:0x086b  */
    /* JADX WARN: Removed duplicated region for block: B:183:0x0565  */
    /* JADX WARN: Removed duplicated region for block: B:327:0x04c4 A[Catch: Exception -> 0x03d8, TryCatch #5 {Exception -> 0x03d8, blocks: (B:340:0x03c6, B:123:0x040d, B:127:0x041c, B:129:0x0455, B:142:0x04ab, B:144:0x04b0, B:326:0x04bd, B:327:0x04c4, B:328:0x047c, B:330:0x0495, B:117:0x03e6, B:122:0x03f3, B:335:0x03f1, B:336:0x03fa), top: B:115:0x03ba }] */
    /* JADX WARN: Type inference failed for: r14v4, types: [android.graphics.Typeface] */
    /* JADX WARN: Type inference failed for: r28v0, types: [float] */
    /* JADX WARN: Type inference failed for: r28v14 */
    /* JADX WARN: Type inference failed for: r28v16 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
   public static Bitmap renderSize(Context var0, Class<?> var1, int var2, boolean var3) {
      RectF var104 = null;
      TextPaint var106 = null;
      Paint var107 = null;
      TextPaint var110 = null;
      Paint var113 = null;
      Bitmap var120 = null;
      Bitmap var126 = null;
      Canvas var127 = null;
      int[] var128 = null;
      TextPaint var133 = null;
      String var134 = null;
      RectF var135 = null;
      Paint var143 = null;
      Paint var147 = null;
      TextPaint var202 = null;
      Paint var203 = null;
      RectF var208 = null;
      RectF var210 = null;
      RectF var218 = null;
      StringBuilder var220 = null;
      String var221 = null;
      Calendar var224 = null;
      Calendar var225 = null;
      String var231 = null;
      TextPaint var234 = null;
      Typeface[] varTfa = null;
      Exception var101 = null;
      long[] var102 = null;
      float var4 = var0.getResources().getDisplayMetrics().density;
      int var12;
      if (var3) {
         var12 = 54;
      } else {
         var12 = 123;
      }

      var12 = (int)(var12 * var4);
      float var6 = 54.0F * var4;
      Bitmap var36 = Bitmap.createBitmap(var12, (int)var6, Config.ARGB_8888);
      Canvas var29 = new Canvas(var36);
      try {
      int var14 = var36.getWidth();
      int var16 = var36.getHeight();
      Locale var37 = locale(var0);
      var12 = ink(var0);
      int var15 = inkSoft(var0);
      int var17 = track(var0);
      boolean var18 = arabic(var0);
      boolean var19 = rtl(var0);
      String var32 = lang(var0);
      Typeface var28 = Typeface.create("sans-serif-medium", 0);
      Typeface var26 = Typeface.create("sans-serif", 1);
      com.example.ui.i18n.L10n.INSTANCE.init(var0);
      Typeface var31 = font(var0, "amiri_bold");
      Typeface var34 = Typeface.createFromAsset(var0.getAssets(), "lockpersonal/expoarabic_medium.ttf");
      Typeface var30 = var34;
      Typeface var27 = Typeface.createFromAsset(var0.getAssets(), "lockpersonal/expoarabic_semibold.ttf");
      int var13;
      if (var3) {
         var13 = 6;
      } else {
         var13 = 12;
      }

      float var8 = var13 * var4;
      if (var1 == LockWidgets.Day.class) {
         if (var18) {
AssetManager var125 = var0.getAssets();
               StringBuilder var146 = new StringBuilder("lockpersonal/day_");
               var146.append(Calendar.getInstance().get(7));
               var146.append(".png");
               var126 = BitmapFactory.decodeStream(var125.open(var146.toString()));


            float var162 = var14;

            float var179;
var6 = Math.min((var162 - var8 * 2.0F) / var126.getWidth(), var4 * 34.0F / var126.getHeight());
               var4 = var126.getWidth() * var6;
               var179 = var126.getHeight() * var6;
               var147 = new Paint(3);
               PorterDuffColorFilter var209 = new PorterDuffColorFilter(var12, Mode.SRC_IN);
               var147.setColorFilter(var209);
               


            var8 = (var162 - var4) / 2.0F;
            var6 = var16;
var210 = new RectF(var8, (var6 - var179) / 2.0F, (var162 + var4) / 2.0F, (var6 + var179) / 2.0F);
               var29.drawBitmap(var126, null, var210, var147);
               return var36;

         } else {
            String var124 = new SimpleDateFormat("EEEE", var37).format(new Date());
            float var161 = var14;
            var6 = var161 / 2.0F;
            float var178 = var16 / 2.0F;
            if (var19) {
               var26 = var31;
            }

            centered(var29, var124, var6, var178, paint(var26, 29.0F, var4, var12), var161 - var8 * 2.0F);
            return var36;
         }
      } else if (var1 == LockWidgets.Gregorian.class) {
         Date var228 = new Date();
         String var121;
         if (var3) {
            var121 = "M/yyyy";
         } else {
            var121 = "MMMM";
         }

         String var122 = new SimpleDateFormat(var121, var37).format(var228);
         String var144 = new SimpleDateFormat("d", var37).format(var228);
         if (var3) {
            if (var19) {
               var26 = var27;
            }

            if (var19) {
               var28 = var34;
            }

            lines(var29, var14, var16, var4, new String[]{var144, var122}, new float[]{18.0F, 9.0F}, new Typeface[]{var26, var28}, var12, var15);
            return var36;
         } else {
            String var145 = var144 + " " + var122;
            String var216 = new SimpleDateFormat("yyyy", var37).format(var228);
            Typeface var123;
            if (var19) {
               var123 = var27;
            } else {
               var123 = var26;
            }

            if (var19) {
               var26 = var27;
            }

            lines(var29, var14, var16, var4, new String[]{var145, var216}, new float[]{18.0F, 16.0F}, new Typeface[]{var123, var26}, var12, var15);
            return var36;
         }
      } else if (var1 == LockWidgets.Hijri.class) {
         IslamicCalendar var140 = new IslamicCalendar();
         var140.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
         var140.setTimeInMillis(System.currentTimeMillis());
         var2 = var0.getSharedPreferences("fajr_prefs", 0).getInt("hijri_offset", 0);
         if (var2 != 0) {
            var140.add(5, var2);
         }
         String var223 = com.example.prayer.HijriDate.INSTANCE.monthName(var140.get(2) + 1, var32);
         String var214 = number(var140.get(5), var37);
         var32 = number(var140.get(1), var37);
         if (var3 && var18) {
            float var160 = var14;
            var6 = var160 / 2.0F;
            TextPaint var205 = paint(var27, 18.0F, var4, var12);
            float var176 = var160 - var8 * 2.0F;
            centered(var29, var214, var6, var4 * 16.0F, var205, var176);

            float var187;
AssetManager var206 = var0.getAssets();
               StringBuilder var119 = new StringBuilder("lockpersonal/hijri_");
               var119.append(var140.get(2) + 1);
               var119.append(".png");
               var120 = BitmapFactory.decodeStream(var206.open(var119.toString()));
               var187 = Math.min(var176 / var120.getWidth(), var4 * 18.0F / var120.getHeight());
               var176 = var120.getWidth() * var187;
               var8 = var120.getHeight();
               var143 = new Paint(3);
               PorterDuffColorFilter var207 = new PorterDuffColorFilter(var12, Mode.SRC_IN);
               var143.setColorFilter(var207);
               


            var6 = (var160 - var176) / 2.0F;
            var4 *= 34.0F;
            var8 = var8 * var187 / 2.0F;
var208 = new RectF(var6, var4 - var8, (var160 + var176) / 2.0F, var4 + var8);
               var29.drawBitmap(var120, null, var208, var143);
               return var36;

         } else {
            String[] var118;
            if (var3) {
               var118 = new String[]{var214, var223};
            } else {
               var118 = new String[]{var214 + " " + var223, var32};
            }

            float[] var215 = new float[2];
            if (var3) {
               var215[0] = 18.0F;
               var215[1] = 10.0F;
            } else {
               var215[0] = 17.0F;
               var215[1] = 16.0F;
            }

            Typeface[] var141;
            if (var3) {
               if (!var19) {
                  var27 = var26;
               }

               if (var19) {
                  var26 = var31;
               }

               var141 = new Typeface[]{var27, var26};
            } else {
               Typeface var142;
               if (var19) {
                  var142 = var27;
               } else {
                  var142 = var26;
               }

               if (var19) {
                  var26 = var27;
               }

               var141 = new Typeface[]{var142, var26};
            }

            lines(var29, var14, var16, var4, var118, var215, var141, var12, var15);
            return var36;
         }
      } else if (var1 == LockWidgets.Tasbih.class) {
         SharedPreferences var115 = var0.getSharedPreferences("lock_tasbih", 0);
         StringBuilder var137 = new StringBuilder("count_");
         var137.append(var2);
         var2 = var115.getInt(var137.toString(), 0);
         String[] var116;
         if (var19) {
            var116 = new String[]{"سُبْحَانَ اللَّه", "الْحَمْدُ لِلَّه", "اللَّهُ أَكْبَر"};
         } else {
            var116 = new String[]{"SubhanAllah", "Alhamdulillah", "Allahu Akbar"};
         }

         String var117 = var116[var2 / 33 % 3];
         StringBuilder var138 = new StringBuilder();
         var138.append(number(var2 % 33, var37));
         var138.append(" / ");
         var138.append(number(33L, var37));
         String var139 = var138.toString();
         if (var19) {
            var26 = var31;
         }

         if (!var19) {
            var30 = var28;
         }

         lines(var29, var14, var16, var4, new String[]{var117, var139}, new float[]{17.0F, 13.0F}, new Typeface[]{var26, var30}, var12, var15);
         return var36;
      } else {
         label723: {
            long[] var33;
            Calendar var38;
var38 = Calendar.getInstance();
               var33 = timesCached(var0, var38);


            long var24;
var24 = System.currentTimeMillis();


            var2 = 0;

            while (true) {
               if (var2 >= 6) {
                  var2 = -1;
                  break;
               }

               if (var2 != 1) {
                  long var20 = var33[var2];
                  if (var20 > var24) {
                     break;
                  }
               }

               var2++;
            }

            label744: {
               long var22;
               long var197;
               if (var2 < 0) {
var224 = (Calendar)var38.clone();

var224.add(5, 1);
                     var22 = timesCached(var0, var224)[0];


                  var197 = var33[5];
                  var2 = 0;
               } else {
                  var22 = var33[var2];
                  if (var2 > 0) {
                     if (var2 == 2) {
                        var13 = 0;
                     } else {
                        var13 = var2 - 1;
                     }

                     var197 = var33[var13];
                  } else {
var225 = (Calendar)var38.clone();

var225.add(5, -1);
                        var197 = timesCached(var0, var225)[5];

                  }
               }

               Canvas var226 = var29;

               String var39;
               String[] var232;

                  var232 = new String[6];
                  var39 = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerFajr);


               var232[0] = var39;
var232[1] = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerShuruq);
                  var232[2] = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerDhuhr);
                  var232[3] = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerAsr);
                  var232[4] = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerMaghrib);
                  var232[5] = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerIsha);
                  if (var38.get(7) == 6) {
                     var232[2] = com.example.ui.i18n.L10n.INSTANCE.t(com.example.ui.i18n.S.PrayerJumuah);
                  }


               label750: {
                  if (var1 == LockWidgets.Bar.class
                     || var1 == LockWidgets.Next.class
                     || var1 == LockWidgets.Countdown.class
                     || var1 == LockWidgets.CountdownClean.class) {
var38 = (Calendar)var38.clone();
                        var38.add(5, -1);
                        var102 = iqamaWindow(var33, timesCached(var0, var38), var24);


                     if (var102 != null) {
                        var2 = (int)var102[0];
                        var197 = var102[1];
                        var22 = var102[2];
                        var18 = true;
                        break label750;
                     }
                  }

                  var18 = false;
               }
HomeWidgets.ovArm(var18, var22, var24);


               String var103;
               if (!var18) {
                  var103 = var232[var2];
               } else {
                  label742: {
                     label681: {
if (var32.startsWith("ar")) {
                              break label681;
                           }

var103 = PrayerPhase.iqamaFor(var32, var232[var2]);
                           break label742;

                     }

                     var103 = "إقامة الصلاة";
                  }
               }

               if (var1 != LockWidgets.Prayers.class) {
                  label751: {
                     Canvas var230 = var29;
                     label665:
                     if (var1 != LockWidgets.Countdown.class && var1 != LockWidgets.CountdownClean.class) {
                        if (var1 == LockWidgets.Bar.class) {
                           var6 = var14;
                           float var158 = var6 / 2.0F;
                           if (!var19) {
                              var27 = var26;
                           }
var133 = paint(var27, 13.0F, var4, var12);


                           float var185 = var6 - var8 * 2.0F;
centered(var230, var103, var158, var4 * 13.0F, var133, var185);
                              var134 = time(var22, var37);


                           Typeface var112;
                           if (var19) {
                              var112 = var34;
                           } else {
                              var112 = var28;
                           }
centered(var230, var134, var158, var4 * 42.0F, paint(var112, 12.0F, var4, var12), var185);
                              var113 = new Paint(1);


                           var158 = var4 * 26.0F;
                           float var188 = 4.0F * var4;
var113.setColor(var17);
                              


                           float var175 = var158 + var188;
var135 = new RectF(var8, var158, var8 + var185, var175);


                           var6 = var188 / 2.0F;
var230.drawRoundRect(var135, var6, var6, var113);


                           float var190 = (float)(var24 - var197);
var185 = Math.max(var188, Math.max(0.0F, Math.min(1.0F, var190 / (float)Math.max(1L, var22 - var197))) * var185);
                              var113.setColor(var12);
                              RectF var136 = new RectF(var8, var158, var8 + var185, var175);
                              var230.drawRoundRect(var136, var6, var6, var113);
                              return var36;

                        }

                        var197 = var22 - var24;
var24 = Math.max(0L, var197 / 60000L);


                        String var130;
                        if (var18) {
var130 = HomeWidgets.mmss(var197);

                        } else {
var130 = HomeWidgets.durText(var24, var32);

                        }

                        String var236;
                        if (var18) {
StringBuilder var235 = new StringBuilder();
                              var235.append(HomeWidgets.remPrefix(var32, " "));
                              var235.append(HomeWidgets.mmss(var197));
                              var236 = var235.toString();

                        } else {
var236 = HomeWidgets.remaining(var24, var32);

                        }

                        String[] var111;
                        if (var3) {
var111 = new String[]{var103, time(var22, var37), var130};

                        } else {
StringBuilder var131 = new StringBuilder();
                              var131.append(var103);
                              var131.append(" ");
                              var131.append(time(var22, var37));
                              var111 = new String[]{var131.toString(), var236};

                        }

                        float[] var132;
                        if (var3) {
var132 = new float[]{12.0F, 11.0F, 9.0F};

                        } else {
var132 = new float[]{13.0F, 9.0F};

                        }

                        Typeface[] var204;
                        if (var3) {
                           Typeface var237;
                           if (var19) {
                              var237 = var27;
                           } else {
                              var237 = var26;
                           }

                           if (!var19) {
                              var30 = var28;
                           }

                           if (var19) {
                              var26 = var27;
                           }
varTfa = new Typeface[3];


                           varTfa[0] = var237;
                           varTfa[1] = var30;
                           varTfa[2] = var26;
                           var204 = varTfa;
                        } else {
                           if (var19) {
                              var30 = var27;
                           } else {
                              var30 = var26;
                           }

                           if (var19) {
                              var26 = var27;
                           }
varTfa = new Typeface[2];


                           varTfa[0] = var30;
                           varTfa[1] = var26;
                           var204 = varTfa;
                        }
lines(var230, var14, var16, var4, var111, var132, var204, var12, var15);
                           return var36;
                        
                     } else {
                        label748: {
                           float var154;
                           if (var19) {
                              var154 = var14 - var8 - var4 * 25.0F;
                           } else {
                              var154 = var4 * 25.0F + var8;
                           }

                           float var172 = var4 * 12.0F;
                           Typeface var233;
                           if (var19) {
                              var233 = var27;
                           } else {
                              var233 = var26;
                           }
var234 = paint(var233, 13.0F, var4, var12);


                           String var227 = var32;
                           var29 = var29;
centered(var29, var103, var154, var172, var234, var4 * 48.0F);
                              var231 = time(var22, var37);


                           if (var19) {
                              var154 = var4 * 26.0F + var8;
                           } else {
                              var154 = var14 - var8 - var4 * 26.0F;
                           }

                           Typeface var105;
                           if (var19) {
                              var105 = var34;
                           } else {
                              var105 = var28;
                           }
var106 = paint(var105, 12.0F, var4, var12);


                           var2 = var12;

                           label569: {
                              label733: {
centered(var29, var231, var154, var172, var106, var6);
                                    var107 = new Paint(1);


                                 var154 = var4 * 25.0F;
                                 float var184 = var14;
                                 var6 = var184 - var8 * 2.0F;
                                 float var11 = 3.0F * var4;
var107.setColor(var17);
                                    


                                 float var10 = var154 + var11;
var218 = new RectF(var8, var154, var8 + var6, var10);


                                 var172 = var11 / 2.0F;
var29.drawRoundRect(var218, var172, var172, var107);
                                    var11 = Math.max(
                                       var11, Math.max(0.0F, Math.min(1.0F, (float)(var24 - var197) / (float)Math.max(1L, var22 - var197))) * var6
                                    );
                                    var107.setColor(var2);
                                    RectF var219 = new RectF(var8, var154, var8 + var11, var10);
                                    var29.drawRoundRect(var219, var172, var172, var107);
                                    HomeWidgets.ovSemi.set(Boolean.TRUE);


                                 var197 = var22 - var24;
var22 = Math.max(0L, var197 / 60000L);


                                 String var108;
                                 if (var18) {
var108 = HomeWidgets.mmss(var197);

                                 } else {
var108 = HomeWidgets.ovToken.get() != null ? HomeWidgets.ovToken.get() : String.format(Locale.ENGLISH, "%02d:%02d", var22 / 60L, var22 % 60L);

                                 }
var220 = new StringBuilder();


                                 String var129;
                                 if (var1 == LockWidgets.Countdown.class) {
var129 = HomeWidgets.remPrefix(var227, ": ");

                                 } else {
                                    var129 = "";
                                 }
var220.append(var129);
                                    var220.append("\u2066");
                                    var220.append(var108);
                                    var220.append("\u2069");
                                    var221 = var220.toString();


                                 var172 = var184 / 2.0F;
                                 Typeface var109;
                                 if (var19) {
                                    var109 = var27;
                                 } else {
                                    var109 = var26;
                                 }

                                 var154 = var4;
var110 = paint(var109, 12.0F, var154, var2);
                                    break label569;
                                 
                              }

                           }

                           var127 = var29;
centered(var127, var221, var172, var4 * 41.0F, var110, var6);
                              return var36;
                           
                        }
                     }

                  }

               }
var128 = new int[]{0, 2, 3, 4, 5};
               

               float var5 = var14;
               var6 = (var5 - var8 * 2.0F) / 5.0F;
               var13 = 0;

               while (true) {
                  if (var13 >= 5) {
                     return var36;
                  }

                  var17 = var128[var13];
                  float var7;
                  if (var19) {
                     var7 = var5 - var8 - (var13 + 0.5F) * var6;
                  } else {
                     var7 = (var13 + 0.5F) * var6 + var8;
                  }

                  String var239 = var232[var17];
var202 = paint(var30, 7.0F, var4, var12);
                  

                  float var9 = var6 - var4 * 1.0F;
                  var29 = var226;
centered(var226, var239, var7, 20.0F * var4, var202, var9);


                  var29 = var226;
centered(var226, time(var33[var17], var37), var7, var4 * 34.0F, paint(var27, 8.0F, var4, var12), var9);


                  if (var17 == var2) {
                     var29 = var226;



                     var29 = var226;
var203 = new Paint(1);


                     var29 = var226;
var203.setColor(var12);


                     var29 = var226;



                     var9 = 0.35F * var6;
                     var29 = var226;
var104 = new RectF(var7 - var9, var4 * 41.0F, var7 + var9, var4 * 42.5F);


                     var29 = var226;
var226.drawRoundRect(var104, var4, var4, var203);

                  }

                  var13++;
               }
            }

         }

      }
      } catch (Exception failure) {
          Log.e("WathakkerLock", "Render failed", failure);
          return var36;
      }
   }

    private static int res(Context context, String str, String str2) {
        return context.getResources().getIdentifier(str2, str, context.getPackageName());
    }

    private static boolean rtl(Context context) {
        String lang = lang(context);
        return lang.startsWith("ar") || lang.startsWith("ur");
    }

    /* JADX WARN: Removed duplicated region for block: B:55:0x00ca A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:67:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void schedule(Context context) {
        watchLanguage(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (!anyActive(context)) {
            alarmManager.cancel(pulse(context));
            alarmManager.cancel(boundaryPi(context));
            return;
        }
        long now = System.currentTimeMillis();
        long next = ((now / 60000) + 1) * 60000;
        long boundary = -1;
        try {
            Calendar calendar = Calendar.getInstance();
            for (int day = 0; day < 2; day++) {
                long[] times = timesCached(context, calendar);
                boolean friday = calendar.get(7) == 6;
                for (int i = 0; i < 6; i++) {
                    if (i == 1) {
                        continue;
                    }
                    long t = times[i];
                    if (t > now && (boundary < 0 || t < boundary)) {
                        boundary = t;
                    }
                    if (friday && i == 2) {
                        continue;
                    }
                    long iqama = (PrayerPhase.iqamaMinutes(i) * 60000) + t;
                    if (iqama > now && (boundary < 0 || iqama < boundary)) {
                        boundary = iqama;
                    }
                }
                calendar.add(5, 1);
            }
        } catch (Exception e) {
            Log.w("WathakkerLock", "Boundary scheduling fallback", e);
        }
        Log.i("WathakkerLock", "pulse arm next=" + next + " boundaryTarget=" + boundary);
        try {
            alarmManager.setExactAndAllowWhileIdle(1, next, pulse(context));
        } catch (SecurityException unused) {
            alarmManager.setAndAllowWhileIdle(1, next, pulse(context));
        }
        if (boundary > 0) {
            try {
                alarmManager.setExactAndAllowWhileIdle(0, boundary, boundaryPi(context));
            } catch (SecurityException unused2) {
                alarmManager.setAndAllowWhileIdle(0, boundary, boundaryPi(context));
            }
        }
    }

    private static String time(long j, Locale locale) {
        return HomeWidgets.tm(j, locale.getLanguage());
    }

    private static long[] times(Context context, Calendar calendar) {
        try {
            return times0(context, calendar);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    private static long[] times0(Context context, Calendar calendar) throws Exception {
        Object obj;
        Object newInstance = FajrPreferences.class.getConstructor(Context.class).newInstance(context);
        Double d = (Double) FajrPreferences.class.getMethod("getLatitude", null).invoke(newInstance, null);
        d.doubleValue();
        Double d2 = (Double) FajrPreferences.class.getMethod("getLongitude", null).invoke(newInstance, null);
        d2.doubleValue();
        Integer num = (Integer) FajrPreferences.class.getMethod("getCalculationMethodId", null).invoke(newInstance, null);
        num.intValue();
        CalculationMethod calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE;
        try {
            obj = CalculationMethod.class.getField("Companion").get(null);
        } catch (NoSuchFieldException unused) {
            obj = CalculationMethod.class.getField("INSTANCE").get(null);
        }
        Object invoke = obj.getClass().getMethod("fromId", Integer.TYPE).invoke(obj, num);
        PrayerAdjustments prayerAdjustments = PrayerAdjustments.INSTANCE;
        Class cls = Double.TYPE;
        Object invoke2 = PrayerAdjustments.class.getMethod("calculateAll", Context.class, Calendar.class, cls, cls, CalculationMethod.class).invoke(PrayerAdjustments.class.getField("INSTANCE").get(null), context, calendar, d, d2, invoke);
        String[] strArr = {"getFajr", "getSunrise", "getDhuhr", "getAsr", "getMaghrib", "getIsha"};
        long[] jArr = new long[6];
        for (int i = 0; i < 6; i++) {
            jArr[i] = ((Long) invoke2.getClass().getMethod(strArr[i], null).invoke(invoke2, null)).longValue();
        }
        return jArr;
    }

    public static synchronized long[] timesCached(Context context, Calendar calendar) {
        try {
            return timesCached0(context, calendar);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    private static synchronized long[] timesCached0(Context context, Calendar calendar) throws Exception {
        synchronized (LockWidgets.class) {
            Object newInstance = FajrPreferences.class.getConstructor(Context.class).newInstance(context);
            double doubleValue = ((Double) FajrPreferences.class.getMethod("getLatitude", null).invoke(newInstance, null)).doubleValue();
            double doubleValue2 = ((Double) FajrPreferences.class.getMethod("getLongitude", null).invoke(newInstance, null)).doubleValue();
            int intValue = ((Integer) FajrPreferences.class.getMethod("getCalculationMethodId", null).invoke(newInstance, null)).intValue();
            long j = (calendar.get(1) * 1000) + calendar.get(6);
            long[] jArr = tcVal;
            if (jArr != null && j == tcDay && doubleValue == tcLat && doubleValue2 == tcLng && intValue == tcMethod) {
                return (long[]) jArr.clone();
            }
            long[] times = times0(context, calendar);
            tcDay = j;
            tcLat = doubleValue;
            tcLng = doubleValue2;
            tcMethod = intValue;
            tcVal = times;
            return (long[]) times.clone();
        }
    }

    private static int track(Context context) {
        return night(context) ? 1090519039 : 1073741824;
    }

    private static void update(Context context, AppWidgetManager appWidgetManager, int i, Class<?> cls) {
        Digits.refresh(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), res(context, "layout", "lock_widget_image"));
        Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(i);
        int i2 = appWidgetOptions.getInt("appWidgetMinWidth", 123);
        boolean z = i2 > 0 && appWidgetOptions.getInt("appWidgetMaxWidth", i2) <= 80 && (cls == Hijri.class || cls == Gregorian.class || cls == Next.class);
        HomeWidgets.ovBegin();
        HomeWidgets.langForNative.set(lang(context));
        Bitmap renderSize = renderSize(context, cls, i, z);
        remoteViews.setImageViewBitmap(res(context, "id", "lock_widget_art"), renderSize);
        HomeWidgets.ovApply(context, remoteViews, renderSize.getWidth(), renderSize.getHeight());
        remoteViews.setInt(res(context, "id", "lock_widget_art"), "setLayoutDirection", rtl(context) ? 1 : 0);
        remoteViews.setOnClickPendingIntent(res(context, "id", "lock_widget_art"), null);
        appWidgetManager.updateAppWidget(i, remoteViews);
    }

    public static void updateAll(Context context) {
        Digits.refresh(context);
        watchLanguage(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (Class<?> cls : TYPES) {
            for (int i : appWidgetManager.getAppWidgetIds(new ComponentName(context, cls))) {
                update(context, appWidgetManager, i, cls);
            }
        }
    }

    public static void updateAllTick(Context context, boolean z) {
        Digits.refresh(context);
        watchLanguage(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (Class<?> cls : TYPES) {
            if ((cls != Day.class && cls != Tasbih.class && cls != Hijri.class && cls != Gregorian.class && cls != Prayers.class) || z) {
                for (int i : appWidgetManager.getAppWidgetIds(new ComponentName(context, cls))) {
                    update(context, appWidgetManager, i, cls);
                }
            }
        }
    }

    private static void watchLanguage(Context context) {
        if (languageListener == null) {
            final Context applicationContext = context.getApplicationContext();
            languageListener = new SharedPreferences.OnSharedPreferenceChangeListener() { // from class: qs0
                @Override // android.content.SharedPreferences.OnSharedPreferenceChangeListener
                public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
                    LockWidgets.lambda$watchLanguage$0(applicationContext, sharedPreferences, str);
                }
            };
            applicationContext.getSharedPreferences("app_prefs", 0).registerOnSharedPreferenceChangeListener(languageListener);
        }
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle) {
        update(context, appWidgetManager, i, getClass());
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onDeleted(Context context, int[] iArr) {
        for (int i : iArr) {
            context.getSharedPreferences("lock_tasbih", 0).edit().remove("count_" + i).apply();
        }
        schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onDisabled(Context context) {
        schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onEnabled(Context context) {
        schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider, android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action(context, "TAP").equals(action)) {
            int intExtra = intent.getIntExtra("appWidgetId", -1);
            AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(intExtra);
            if (appWidgetInfo == null || !appWidgetInfo.provider.equals(new ComponentName(context, (Class<?>) Tasbih.class))) {
                return;
            }
            SharedPreferences sharedPreferences = context.getSharedPreferences("lock_tasbih", 0);
            String K = Str.K(intExtra, "count_");
            sharedPreferences.edit().putInt(K, sharedPreferences.getInt(K, 0) + 1).apply();
            update(context, AppWidgetManager.getInstance(context), intExtra, Tasbih.class);
            return;
        }
        if (action(context, "TICK").equals(action) || action(context, "BOUNDARY").equals(action)) {
            Log.i("WathakkerLock", action(context, "BOUNDARY").equals(action) ? "boundary fire" : "tick fire");
            boolean z = action(context, "BOUNDARY").equals(action) || phaseChanged(context);
            updateAllTick(context, z);
            HomeWidgets.updateAll(context, !z);
            try {
                WidgetUpdater.INSTANCE.updateFace(context);
            } catch (Throwable th) {
                Log.w("WathakkerLock", "face widget update failed", th);
            }
            schedule(context);
            return;
        }
        if (!"android.intent.action.BOOT_COMPLETED".equals(action) && !"android.intent.action.TIME_SET".equals(action) && !"android.intent.action.TIMEZONE_CHANGED".equals(action) && !"android.intent.action.DATE_CHANGED".equals(action) && !"android.intent.action.MY_PACKAGE_REPLACED".equals(action)) {
            super.onReceive(context, intent);
            return;
        }
        updateAll(context);
        HomeWidgets.updateAll(context);
        try {
            WidgetUpdater.INSTANCE.updateAll(context);
        } catch (Throwable th2) {
            Log.w("WathakkerLock", "home widget updater failed", th2);
        }
        schedule(context);
    }

    @Override // android.appwidget.AppWidgetProvider
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        for (int i : iArr) {
            update(context, appWidgetManager, i, getClass());
        }
        schedule(context);
    }
}
