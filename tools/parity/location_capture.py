"""Same saved location/date/settings in original and comparison installs."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):return subprocess.run(['adb',*a],capture_output=True,text=True,timeout=60).stdout
def tree():
 x=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((s.split('=',1)[1] for s in x.splitlines() if s.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def shot(n):
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']));(out/(n+'.xml')).write_text(tree())
adb('wait-for-device');adb('root');time.sleep(5);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:assert 'Success' in adb('install','-r','-g',a)
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','date','103016222026.00')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for place,lat,lon,city,expected in [('borg',30.8619,29.5845,'برج العرب الجديدة',['04:46','06:14','11:45','02:52','05:16','06:35']),('cairo',30.0444,31.2357,'القاهرة',['04:39','06:07','11:39','02:47','05:10','06:29'])]:
  adb('shell','am','force-stop',pkg);folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  app='<map><string name="language">ar</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  fajr=f'<map><float name="latitude" value="{lat}"/><float name="longitude" value="{lon}"/><string name="city_name">{city}</string><string name="city_name_ar">{city}</string><string name="country_name">مصر</string><boolean name="use_current_location" value="false"/><int name="calculation_method" value="1"/><int name="asr_juristic" value="0"/><int name="high_lat_rule" value="3"/></map>'
  for f,x in [('app_prefs',app),('fajr_prefs',fajr)]:adb('shell',f"echo '{x}' > {folder}/{f}.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/{f}.xml; chmod 660 {folder}/{f}.xml; restorecon -R {folder}")
  (out/(name+'-'+place+'-saved-prefs.xml')).write_text(adb('shell','cat',folder+'/fajr_prefs.xml'))
  adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(8);shot(name+'-'+place+'-top');alltext=tree()
  for i in range(2):adb('shell','input','swipe','540','1800','540','1000','500');time.sleep(1);shot(name+'-'+place+'-scroll'+str(i));alltext+=tree()
  for t in expected:checks.append({'app':name,'place':place,'time':t,'passed':t in alltext})
(out/'checks.json').write_text(json.dumps(checks,indent=2));(out/'logcat.txt').write_text(adb('logcat','-d'));(out/'date.txt').write_text(adb('shell','date'))
assert all(x['passed'] for x in checks),checks
