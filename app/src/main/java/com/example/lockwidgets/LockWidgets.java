package com.example.lockwidgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.icu.util.IslamicCalendar;
import android.os.Bundle;
import android.text.*;
import android.widget.RemoteViews;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

/** Native port of Prayer-Sam-Widgets artwork and interactions into the recovered Wathakker app. */
public class LockWidgets extends AppWidgetProvider {
    public static class Day extends LockWidgets {}
    public static class Prayers extends LockWidgets {}
    public static class Tasbih extends LockWidgets {}
    public static class Hijri extends LockWidgets {}
    public static class Gregorian extends LockWidgets {}
    public static class Next extends LockWidgets {}
    public static class Bar extends LockWidgets {}
    public static class Countdown extends LockWidgets {}
    public static class CountdownClean extends LockWidgets {}
    private static final Class<?>[] TYPES = {Day.class,Tasbih.class,Hijri.class,Gregorian.class,Next.class,Bar.class,Countdown.class,CountdownClean.class};
    private static String action(Context c,String suffix){return c.getPackageName()+".lockwidgets."+suffix;}
    private static int res(Context c,String type,String name){return c.getResources().getIdentifier(name,type,c.getPackageName());}
    private static String lang(Context c){String x=c.getSharedPreferences("app_prefs",0).getString("language","ar");return x==null?"ar":x;}
    private static boolean rtl(Context c){String x=lang(c);return x.startsWith("ar")||x.startsWith("ur");}
    private static boolean arabic(Context c){return lang(c).startsWith("ar");}
    private static SharedPreferences.OnSharedPreferenceChangeListener languageListener;
    private static void watchLanguage(Context c){if(languageListener==null){Context app=c.getApplicationContext();languageListener=(prefs,key)->{if("language".equals(key))updateAll(app);else if("digits_style".equals(key)){updateAll(app);HomeWidgets.updateAll(app);}};app.getSharedPreferences("app_prefs",0).registerOnSharedPreferenceChangeListener(languageListener);}}
    private static Locale locale(Context c){return new Locale(lang(c));}
    private static PendingIntent pulse(Context c){return PendingIntent.getBroadcast(c,700001,new Intent(c,LockWidgets.class).setAction(action(c,"TICK")),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static PendingIntent boundaryPi(Context c){return PendingIntent.getBroadcast(c,700002,new Intent(c,LockWidgets.class).setAction(action(c,"BOUNDARY")),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static boolean active(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(Class<?> t:TYPES)if(m.getAppWidgetIds(new ComponentName(c,t)).length>0)return true;return false;}
    /** True while any lock/home widget instance exists; the per-minute pulse is armed only then. */
    public static boolean anyActive(Context c){boolean any=active(c);AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(String k:new String[]{"Hero","NextPrayer","PrayerBar","Clock","Pill","DayPrayer","OneUI","Gregorian","Hijri","Iqama","IqamaSmall","Schedule","WideBar"})any|=m.getAppWidgetIds(new ComponentName(c,"com.example.widget."+k+"WidgetProvider")).length>0;
        return any;}
    public static void schedule(Context c){watchLanguage(c);AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(!anyActive(c)){a.cancel(pulse(c));a.cancel(boundaryPi(c));return;}
        long now=System.currentTimeMillis(),minute=(now/60000+1)*60000,target=-1;
        try{Calendar day=Calendar.getInstance();for(int d=0;d<2;d++){long[] ts=timesCached(c,day);boolean friday=day.get(Calendar.DAY_OF_WEEK)==Calendar.FRIDAY;
            for(int i=0;i<6;i++){if(i==1)continue;if(ts[i]>now&&(target<0||ts[i]<target))target=ts[i];if(!(friday&&i==2)){long iq=ts[i]+PrayerPhase.iqamaMinutes(i)*60000L;if(iq>now&&(target<0||iq<target))target=iq;}}day.add(Calendar.DATE,1);}
        }catch(Exception e){android.util.Log.w("WathakkerLock","Boundary scheduling fallback",e);}
        // Two independent alarms, re-evaluated at every schedule() call:
        // - pulse: non-wakeup minute alarm. Minute digits are only visible while a host is showing
        //   them, so while the device sleeps this defers and fires at the next wake.
        // - boundary: wakeup alarm at the next prayer/iqama instant, armed HERE rather than promoted
        //   by a later pulse delivery - a deferred non-wakeup pulse can never be the mechanism that
        //   was supposed to keep the rollover on time. Same PI identity each call, so no accumulation.
        // If target computation failed the old boundary PI stays pending; it fires one harmless
        // extra update and the next successful schedule() replaces it.
        // Regression diagnostics (within the staleness-fix scope): record which alarm branch actually
        // armed - exact vs the silent inexact SecurityException fallback - plus the exact-alarm access
        // state, so a stale device can be told apart from logcat: canExact=false or *Exact=false means
        // the OS denied exact alarms; otherwise deferral happened at delivery time (sleep/batching).
        boolean canExact=android.os.Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms();
        boolean pulseExact=true,boundExact=true;
        try{a.setExactAndAllowWhileIdle(AlarmManager.RTC,minute,pulse(c));}catch(SecurityException e){pulseExact=false;a.setAndAllowWhileIdle(AlarmManager.RTC,minute,pulse(c));}
        if(target>0){try{a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target,boundaryPi(c));}catch(SecurityException e){boundExact=false;a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target,boundaryPi(c));}}
        android.util.Log.i("WathakkerLock","arm next="+minute+" target="+target+" canExact="+canExact+" pulseExact="+pulseExact+" boundExact="+boundExact);
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id,getClass());schedule(c);}
    @Override public void onEnabled(Context c){schedule(c);}
    @Override public void onDisabled(Context c){schedule(c);}
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids)c.getSharedPreferences("lock_tasbih",0).edit().remove("count_"+id).apply();schedule(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){update(c,m,id,getClass());}
    @Override public void onReceive(Context c,Intent in){
        String a=in.getAction();
        if(action(c,"TAP").equals(a)){
            int id=in.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
            AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
            if(info!=null&&info.provider.equals(new ComponentName(c,Tasbih.class))){SharedPreferences p=c.getSharedPreferences("lock_tasbih",0);String key="count_"+id;p.edit().putInt(key,p.getInt(key,0)+1).apply();update(c,AppWidgetManager.getInstance(c),id,Tasbih.class);}return;
        }
        if(action(c,"TICK").equals(a)||action(c,"BOUNDARY").equals(a)){
            android.util.Log.i("WathakkerLock",(action(c,"BOUNDARY").equals(a)?"boundary fire":"tick fire")+" t="+System.currentTimeMillis());
            boolean full=action(c,"BOUNDARY").equals(a)||phaseChanged(c);updateAllTick(c,full);HomeWidgets.updateAll(c,!full);try{com.example.widget.WidgetUpdater.INSTANCE.updateFace(c);}catch(Throwable t){android.util.Log.w("WathakkerLock","face widget update failed",t);}schedule(c);return;
        }
        if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_TIME_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a)||Intent.ACTION_DATE_CHANGED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)){
            updateAll(c);HomeWidgets.updateAll(c);try{com.example.widget.WidgetUpdater.INSTANCE.updateAll(c);}catch(Throwable t){android.util.Log.w("WathakkerLock","home widget updater failed",t);}schedule(c);return;
        }
        super.onReceive(c,in);
    }
    public static void updateAll(Context c){com.example.utils.Digits.refresh(c);watchLanguage(c);AppWidgetManager m=AppWidgetManager.getInstance(c);for(Class<?> t:TYPES)for(int id:m.getAppWidgetIds(new ComponentName(c,t)))update(c,m,id,t);}
    /** Minute-pulse update: countdown types every tick, static types only when the phase tracker flags a boundary. */
    public static void updateAllTick(Context c,boolean full){com.example.utils.Digits.refresh(c);watchLanguage(c);AppWidgetManager m=AppWidgetManager.getInstance(c);for(Class<?> t:TYPES){boolean pulseOnly=(t==Day.class||t==Tasbih.class||t==Hijri.class||t==Gregorian.class||t==Prayers.class);if(pulseOnly&&!full)continue;for(int id:m.getAppWidgetIds(new ComponentName(c,t)))update(c,m,id,t);}}
    private static long lastDayKey=-1;private static int lastPhase=Integer.MIN_VALUE;private static boolean lastIqama=false;
    /** True when the day, next-prayer index or iqama state changed since the previous tick (or on first tick after process start). */
    private static synchronized boolean phaseChanged(Context c){
        try{Calendar today=Calendar.getInstance();long[] ts=timesCached(c,today);long now=System.currentTimeMillis();
            Calendar yesterday=(Calendar)today.clone();yesterday.add(Calendar.DAY_OF_MONTH,-1);
            boolean iqama=iqamaWindow(ts,timesCached(c,yesterday),now)!=null;
            int next=-1;for(int i=0;i<6;i++)if(i!=1&&ts[i]>now){next=i;break;}if(next<0)next=6;
            long dayKey=today.get(Calendar.YEAR)*1000L+today.get(Calendar.DAY_OF_YEAR);
            boolean changed=(lastPhase==Integer.MIN_VALUE)||(dayKey!=lastDayKey)||(next!=lastPhase)||(iqama!=lastIqama);
            lastDayKey=dayKey;lastPhase=next;lastIqama=iqama;return changed;
        }catch(Exception e){android.util.Log.w("WathakkerLock","phase tracking failed - full update",e);return true;}
    }
    private static long tcDay=-1;private static double tcLat=Double.NaN,tcLng=Double.NaN;private static int tcMethod=-1;private static long[] tcVal=null;
    /** Per-day cache of the reflection-based prayer-time calculation, keyed on date + location + method. */
    static synchronized long[] timesCached(Context c,Calendar day)throws Exception{
        Class<?> p=Class.forName("com.example.fajr.data.FajrPreferences");Object prefs=p.getConstructor(Context.class).newInstance(c);
        double lat=(Double)p.getMethod("getLatitude").invoke(prefs),lng=(Double)p.getMethod("getLongitude").invoke(prefs);
        int method=(Integer)p.getMethod("getCalculationMethodId").invoke(prefs);
        long key=day.get(Calendar.YEAR)*1000L+day.get(Calendar.DAY_OF_YEAR);
        if(tcVal!=null&&key==tcDay&&lat==tcLat&&lng==tcLng&&method==tcMethod)return tcVal.clone();
        long[] r=times(c,day);tcDay=key;tcLat=lat;tcLng=lng;tcMethod=method;tcVal=r;return r.clone();
    }
    private static void update(Context c,AppWidgetManager m,int id,Class<?> type){com.example.utils.Digits.refresh(c);
        RemoteViews v=new RemoteViews(c.getPackageName(),res(c,"layout","lock_widget_image"));
        Bundle opts=m.getAppWidgetOptions(id);int min=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,123),max=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,min);boolean tiny=min>0&&max<=80&&(type==Hijri.class||type==Gregorian.class||type==Next.class);HomeWidgets.ovBegin();HomeWidgets.langForNative.set(lang(c));Bitmap bmp=renderSize(c,type,id,tiny);
        v.setImageViewBitmap(res(c,"id","lock_widget_art"),bmp);HomeWidgets.ovApply(c,v,bmp.getWidth(),bmp.getHeight());
        v.setInt(res(c,"id","lock_widget_art"),"setLayoutDirection",rtl(c)?android.view.View.LAYOUT_DIRECTION_RTL:android.view.View.LAYOUT_DIRECTION_LTR);
        v.setOnClickPendingIntent(res(c,"id","lock_widget_art"),null);
        m.updateAppWidget(id,v);
    }
    /** Call r7's own calculator/preferences so all widgets agree with its prayer screen. */
    private static long[] times(Context c,Calendar day)throws Exception{
        Class<?> p=Class.forName("com.example.fajr.data.FajrPreferences");Object prefs=p.getConstructor(Context.class).newInstance(c);
        double lat=(Double)p.getMethod("getLatitude").invoke(prefs),lng=(Double)p.getMethod("getLongitude").invoke(prefs);
        int method=(Integer)p.getMethod("getCalculationMethodId").invoke(prefs);
        Class<?> meth=Class.forName("com.example.fajr.calculation.CalculationMethod");Object companion;try{companion=meth.getField("Companion").get(null);}catch(NoSuchFieldException nf){companion=meth.getField("INSTANCE").get(null);}Object choice=companion.getClass().getMethod("fromId",int.class).invoke(companion,method);
        Class<?> calc=Class.forName("com.example.fajr.calculation.PrayerAdjustments");Object result=calc.getMethod("calculateAll",android.content.Context.class,Calendar.class,double.class,double.class,meth).invoke(calc.getField("INSTANCE").get(null),c,day,lat,lng,choice);
        String[] getters={"getFajr","getSunrise","getDhuhr","getAsr","getMaghrib","getIsha"};long[] out=new long[6];for(int i=0;i<6;i++)out[i]=(Long)result.getClass().getMethod(getters[i]).invoke(result);return out;
    }
    private static String time(long value,Locale l){return HomeWidgets.tm(value,l.getLanguage());}
    private static String number(long n,Locale l){NumberFormat f=NumberFormat.getIntegerInstance(l);f.setGroupingUsed(false);return f.format(n);}
    private static Typeface font(Context c,String file){return Typeface.createFromAsset(c.getAssets(),"lockfonts/"+file+".ttf");}
    private static boolean night(Context c){return (c.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
    private static int ink(Context c){return night(c)?Color.WHITE:0xFF1C1C1E;}
    private static int inkSoft(Context c){return night(c)?0xE6FFFFFF:0xE61C1C1E;}
    private static int track(Context c){return night(c)?0x40FFFFFF:0x40000000;}
    private static TextPaint paint(Typeface font,float size,float d,int color){TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(font);p.setTextSize(size*d);p.setColor(color);int shadow=(Color.red(color)+Color.green(color)+Color.blue(color)>384)?0x66000000:0x66FFFFFF;p.setShadowLayer(3*d,0,d,shadow);return p;}
    private static void lines(Canvas canvas,int w,int h,float d,String[] text,float[] sizes,Typeface[] fonts,int INK,int SOFT){
        StaticLayout[] ls=new StaticLayout[text.length];float total=0;
        String[] shown=new String[text.length];for(int i=0;i<text.length;i++)shown[i]=com.example.utils.Digits.forWidget(text[i]);
        for(int i=0;i<text.length;i++){TextPaint lp=paint(fonts[i],sizes[i],d,i==0?INK:SOFT);float measured=lp.measureText(shown[i]);if(measured>w-4*d)lp.setTextSize(lp.getTextSize()*(w-4*d)/measured);ls[i]=StaticLayout.Builder.obtain(shown[i],0,shown[i].length(),lp,w).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();total+=ls[i].getHeight();}
        total+=(text.length-1)*2*d;float y=Math.max(0,(h-total)/2);
        for(int li=0;li<ls.length;li++){StaticLayout l=ls[li];if(!HomeWidgets.capture(text[li],w/2f,y+l.getHeight()/2f,w-4*d,sizes[li]*d,li==0?INK:SOFT)){canvas.save();canvas.translate(0,y);l.draw(canvas);canvas.restore();}y+=l.getHeight()+2*d;}
    }
    public static Bitmap render(Context c,Class<?> type,int id){return renderSize(c,type,id,false);}
    private static void centered(Canvas cv,String text,float cx,float cy,TextPaint p,float max){if(HomeWidgets.capture(text,cx,cy,max,p.getTextSize(),p.getColor()))return;text=com.example.utils.Digits.forWidget(text);float width=p.measureText(text);if(width>max)p.setTextSize(p.getTextSize()*max/width);StaticLayout line=StaticLayout.Builder.obtain(text,0,text.length(),p,(int)max).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build();cv.save();cv.translate(cx-max/2,cy-line.getHeight()/2f);line.draw(cv);cv.restore();}
    public static String errText(String code){if(code.startsWith("ar"))return "تعذر حساب الصلاة";if(code.startsWith("fr"))return "Heure de prière indisponible";if(code.startsWith("de"))return "Gebetszeit nicht verfügbar";if(code.startsWith("es"))return "Hora de oración no disponible";if(code.startsWith("it"))return "Ora della preghiera non disponibile";if(code.startsWith("tr"))return "Namaz vakti hesaplanamadı";if(code.startsWith("ur"))return "نماز کا وقت دستیاب نہیں";if(code.startsWith("id"))return "Waktu salat tidak tersedia";if(code.startsWith("ru"))return "Время молитвы недоступно";if(code.startsWith("pt"))return "Hora de oração indisponível";return "Prayer time unavailable";}
    public static long[] iqamaWindow(long[] today,long[] yesterday,long now){return PrayerPhase.window(today,yesterday,now,Calendar.getInstance().get(Calendar.DAY_OF_WEEK)==Calendar.FRIDAY);}
    public static Bitmap renderSize(Context c,Class<?> type,int id,boolean tiny){
        float d=c.getResources().getDisplayMetrics().density;int wd=tiny?54:123,hd=54;
        Bitmap bmp=Bitmap.createBitmap((int)(wd*d),(int)(hd*d),Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(bmp);int w=bmp.getWidth(),h=bmp.getHeight();Locale l=locale(c);int INK=ink(c),SOFT=inkSoft(c),TRACK=track(c);boolean ar=arabic(c);boolean rtl=rtl(c);String code=lang(c);Typeface sysM=Typeface.create("sans-serif-medium",Typeface.NORMAL),sysB=Typeface.create("sans-serif",Typeface.BOLD);com.example.ui.i18n.L10n.INSTANCE.init(c);
        Typeface amiri=font(c,"amiri_bold"),medium=Typeface.createFromAsset(c.getAssets(),"lockpersonal/expoarabic_medium.ttf"),bold=Typeface.createFromAsset(c.getAssets(),"lockpersonal/expoarabic_semibold.ttf");float pad=(tiny?6:12)*d;
        if(type==Day.class){if(ar){try{Bitmap art=BitmapFactory.decodeStream(c.getAssets().open("lockpersonal/day_"+Calendar.getInstance().get(Calendar.DAY_OF_WEEK)+".png"));float ratio=Math.min((w-2*pad)/art.getWidth(),34*d/art.getHeight());float aw=art.getWidth()*ratio,ah=art.getHeight()*ratio;Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);ink.setColorFilter(new PorterDuffColorFilter(INK,PorterDuff.Mode.SRC_IN));cv.drawBitmap(art,null,new RectF((w-aw)/2,(h-ah)/2,(w+aw)/2,(h+ah)/2),ink);}catch(Exception ex){throw new RuntimeException(ex);}}else{String name=new SimpleDateFormat("EEEE",l).format(new Date());centered(cv,name,w/2f,h/2f,paint(rtl?amiri:sysB,29,d,INK),w-2*pad);}}
        else if(type==Gregorian.class){Date now=new Date();String date=new SimpleDateFormat(tiny?"M/yyyy":"MMMM",l).format(now);String day=new SimpleDateFormat("d",l).format(now);if(tiny){lines(cv,w,h,d,new String[]{day,date},new float[]{18,9},new Typeface[]{rtl?bold:sysB,rtl?medium:sysM},INK,SOFT);}else{lines(cv,w,h,d,new String[]{day+" "+date,new SimpleDateFormat("yyyy",l).format(now)},new float[]{18,16},new Typeface[]{rtl?bold:sysB,rtl?bold:sysB},INK,SOFT);}}
        else if(type==Hijri.class){IslamicCalendar cal=new IslamicCalendar();cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);cal.setTimeInMillis(System.currentTimeMillis());int hoff=c.getSharedPreferences("fajr_prefs",0).getInt("hijri_offset",0);if(hoff!=0)cal.add(Calendar.DATE,hoff);String month=com.example.prayer.HijriDate.INSTANCE.monthName(cal.get(Calendar.MONTH)+1,code),day=number(cal.get(Calendar.DAY_OF_MONTH),l),year=number(cal.get(Calendar.YEAR),l);if(tiny&&ar){centered(cv,day,w/2f,16*d,paint(bold,18,d,INK),w-2*pad);try{Bitmap art=BitmapFactory.decodeStream(c.getAssets().open("lockpersonal/hijri_"+(cal.get(Calendar.MONTH)+1)+".png"));float ratio=Math.min((w-2*pad)/art.getWidth(),18*d/art.getHeight());float aw=art.getWidth()*ratio,ah=art.getHeight()*ratio;Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);ink.setColorFilter(new PorterDuffColorFilter(INK,PorterDuff.Mode.SRC_IN));cv.drawBitmap(art,null,new RectF((w-aw)/2,34*d-ah/2,(w+aw)/2,34*d+ah/2),ink);}catch(Exception ex){throw new RuntimeException(ex);}}else{lines(cv,w,h,d,tiny?new String[]{day,month}:new String[]{day+" "+month,year},tiny?new float[]{18,10}:new float[]{17,16},tiny?new Typeface[]{rtl?bold:sysB,rtl?amiri:sysB}:new Typeface[]{rtl?bold:sysB,rtl?bold:sysB},INK,SOFT);}}
        else if(type==Tasbih.class){int n=c.getSharedPreferences("lock_tasbih",0).getInt("count_"+id,0);String[] dhikr=rtl?new String[]{"سُبْحَانَ اللَّه","الْحَمْدُ لِلَّه","اللَّهُ أَكْبَر"}:new String[]{"SubhanAllah","Alhamdulillah","Allahu Akbar"};lines(cv,w,h,d,new String[]{dhikr[(n/33)%3],number(n%33,l)+" / "+number(33,l)},new float[]{17,13},new Typeface[]{rtl?amiri:sysB,rtl?medium:sysM},INK,SOFT);}
        else {
            try {Calendar today=Calendar.getInstance();long[] ts=timesCached(c,today);long now=System.currentTimeMillis();int next=-1;for(int i=0;i<6;i++)if(i!=1&&ts[i]>now){next=i;break;}long at,prev;
                if(next<0){Calendar tomorrow=(Calendar)today.clone();tomorrow.add(Calendar.DAY_OF_MONTH,1);at=timesCached(c,tomorrow)[0];prev=ts[5];next=0;}else{at=ts[next];if(next>0)prev=ts[next==2?0:next-1];else{Calendar yesterday=(Calendar)today.clone();yesterday.add(Calendar.DAY_OF_MONTH,-1);prev=timesCached(c,yesterday)[5];}}
                com.example.ui.i18n.L10n lx=com.example.ui.i18n.L10n.INSTANCE;String[] names={lx.t(com.example.ui.i18n.S.PrayerFajr),lx.t(com.example.ui.i18n.S.PrayerShuruq),lx.t(com.example.ui.i18n.S.PrayerDhuhr),lx.t(com.example.ui.i18n.S.PrayerAsr),lx.t(com.example.ui.i18n.S.PrayerMaghrib),lx.t(com.example.ui.i18n.S.PrayerIsha)};if(today.get(Calendar.DAY_OF_WEEK)==Calendar.FRIDAY)names[2]=lx.t(com.example.ui.i18n.S.PrayerJumuah);
                boolean iqama=false;if(type==Bar.class||type==Next.class||type==Countdown.class||type==CountdownClean.class){Calendar yesterday=(Calendar)today.clone();yesterday.add(Calendar.DAY_OF_MONTH,-1);long[] window=iqamaWindow(ts,timesCached(c,yesterday),now);if(window!=null){iqama=true;next=(int)window[0];prev=window[1];at=window[2];}}HomeWidgets.ovArm(iqama,at,now);
                String target=iqama?(code.startsWith("ar")?"إقامة الصلاة":PrayerPhase.iqamaFor(code,names[next])):names[next];
                if(type==Prayers.class){int[] indexes={0,2,3,4,5};float cell=(w-2*pad)/5;for(int j=0;j<5;j++){int i=indexes[j];float x=rtl?w-pad-cell*(j+.5f):pad+cell*(j+.5f);centered(cv,names[i],x,20*d,paint(medium,7,d,INK),cell-1*d);centered(cv,time(ts[i],l),x,34*d,paint(bold,8,d,INK),cell-1*d);if(i==next){Paint marker=new Paint(Paint.ANTI_ALIAS_FLAG);marker.setColor(INK);cv.drawRoundRect(new RectF(x-cell*.35f,41*d,x+cell*.35f,42.5f*d),d,d,marker);}}}
                else if(type==Countdown.class||type==CountdownClean.class){
                    centered(cv,target,rtl?w-pad-25*d:pad+25*d,12*d,paint(rtl?bold:sysB,13,d,INK),48*d);
                    centered(cv,time(at,l),rtl?pad+26*d:w-pad-26*d,12*d,paint(rtl?medium:sysM,12,d,INK),54*d);
                    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);float top=25*d,width=w-2*pad,bh=3*d;p.setColor(TRACK);cv.drawRoundRect(new RectF(pad,top,pad+width,top+bh),bh/2,bh/2,p);float fraction=Math.max(0,Math.min(1,(now-prev)/(float)Math.max(1,at-prev))),fw=Math.max(bh,width*fraction);p.setColor(INK);cv.drawRoundRect(new RectF(pad,top,pad+fw,top+bh),bh/2,bh/2,p);
                    HomeWidgets.ovSemi.set(Boolean.TRUE);long mins=Math.max(0,(at-now)/60000);String remain=HomeWidgets.ovToken.get()!=null?HomeWidgets.ovToken.get():(iqama?HomeWidgets.mmss(at-now):String.format(Locale.ENGLISH,"%02d:%02d",mins/60,mins%60));centered(cv,(type==Countdown.class?HomeWidgets.remPrefix(code,": "):"")+"⁦"+remain+"⁩",w/2f,41*d,paint(rtl?bold:sysB,12,d,INK),w-2*pad);
                }
                else if(type==Bar.class){centered(cv,target,w/2f,13*d,paint(rtl?bold:sysB,13,d,INK),w-2*pad);centered(cv,time(at,l),w/2f,42*d,paint(rtl?medium:sysM,12,d,INK),w-2*pad);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);float top=26*d,width=w-2*pad,bh=4*d;p.setColor(TRACK);cv.drawRoundRect(new RectF(pad,top,pad+width,top+bh),bh/2,bh/2,p);float fraction=Math.max(0,Math.min(1,(now-prev)/(float)Math.max(1,at-prev))),fw=Math.max(bh,width*fraction);p.setColor(INK);cv.drawRoundRect(new RectF(pad,top,pad+fw,top+bh),bh/2,bh/2,p);}
                else {long minutes=Math.max(0,(at-now)/60000);String countdown=iqama?HomeWidgets.mmss(at-now):HomeWidgets.durText(minutes,code);String remaining=iqama?HomeWidgets.remPrefix(code," ")+HomeWidgets.mmss(at-now):HomeWidgets.remaining(minutes,code);lines(cv,w,h,d,tiny?new String[]{target,time(at,l),countdown}:new String[]{target+" "+time(at,l),remaining},tiny?new float[]{12,11,9}:new float[]{13,9},tiny?new Typeface[]{rtl?bold:sysB,rtl?medium:sysM,rtl?bold:sysB}:new Typeface[]{rtl?bold:sysB,rtl?bold:sysB},INK,SOFT);}
            }catch(Exception ex){android.util.Log.e("WathakkerLock","Prayer bridge failed",ex);lines(cv,w,h,d,new String[]{errText(code)},new float[]{11},new Typeface[]{rtl?medium:sysM},INK,SOFT);}
        }
        return bmp;
    }
}
