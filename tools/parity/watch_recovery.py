"""Paired original Oct26 watch UI and state evidence. No publication or external messages."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[];states={}
def adb(*args):
 r=subprocess.run(['adb',*args],capture_output=True,timeout=45);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,ok,detail=''):
 checks.append(dict(name=n,passed=bool(ok),detail=detail));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(n,ok,detail,flush=True)
def tree():
 text=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((x.split('=',1)[1] for x in text.splitlines() if x.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def shot(n):
 x=tree();(out/(n+'.xml')).write_text(x);(out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=20));states[n]=[(z.get('text',''),z.get('content-desc','')) for z in ET.fromstring(x).iter('node') if z.get('text') or z.get('content-desc')];(out/'states.json').write_text(json.dumps(states,ensure_ascii=False,indent=2))
 check(n+' foreground',any(pkg in x and 'mCurrentFocus=' in x for x in adb('shell','dumpsys','window').splitlines()))
def tap(text):
 root=ET.fromstring(tree());parents={c:p for p in root.iter() for c in p}
 for n in root.iter('node'):
  if text not in [n.get('text'),n.get('content-desc')]:continue
  target=n
  while target.get('clickable')!='true' and target in parents:target=parents[target]
  b=list(map(int,re.findall(r'\d+',target.get('bounds',''))))
  if target.get('clickable')=='true' and len(b)==4 and b[2]>b[0] and b[3]>b[1] and 15<b[1]<420:
   adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(.7);return True
 return False
def scroll(up=True):adb('shell','input','swipe','227','340' if up else '120','227','120' if up else '340','300');time.sleep(.5)
def choose(text):
 for k in range(5):
  if tap(text):return True
  scroll()
 return False
def launch(page=3,sub=0):
 adb('shell','am','force-stop',pkg);zone();adb('shell','am','start','-W','-n',pkg+'/com.wathakker.wear.MainActivity','--ei','page',str(page),'--ei','sub',str(sub));time.sleep(4)
 for _ in range(12):
  x=tree()
  if 'Starting' not in x and 'permissioncontroller' not in x:break
  adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
def zone():
 # Wear's TimeZoneMediator can overwrite a raw setprop even with auto timezone off.
 # UTC is deliberate here to isolate app arithmetic from different Cairo TZDB versions.
 adb('shell','cmd','alarm','set-timezone','UTC');adb('shell','setprop','persist.sys.timezone','UTC')
 adb('shell','am','broadcast','-a','android.intent.action.TIMEZONE_CHANGED','--es','time-zone','UTC')
 (out/(slug+'-environment.txt')).write_text(adb('shell','date')+adb('shell','getprop','persist.sys.timezone')+adb('shell','settings','get','global','auto_time_zone'))
def reset():
 adb('shell','am','force-stop',pkg);folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
 xml=f'<map><int name="lang" value="{2 if lang=="en" else 1}"/><int name="pat" value="1"/><int name="theme" value="0"/><int name="font_choice" value="1"/><float name="font_scale" value="1.0"/><boolean name="perm_asked" value="true"/></map>'
 adb('shell',f"echo '{xml}' > {folder}/wear_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/wear_prefs.xml; chmod 660 {folder}/wear_prefs.xml; restorecon -R {folder}")
def prefs(n):
 text=adb('shell','cat',f'/data/data/{pkg}/shared_prefs/wear_prefs.xml');(out/(n+'.xml')).write_text(text);return {x.get('name'):x.get('value',x.text) for x in ET.fromstring(text).iter() if x.get('name')}
def record(n):return subprocess.Popen(['adb','shell','screenrecord','--time-limit','25','/sdcard/'+n+'.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
def stop(p,n):
 adb('shell','pkill','-INT','screenrecord')
 try:p.wait(timeout=5)
 except:p.kill()
 adb('pull','/sdcard/'+n+'.mp4',str(out/(n+'.mp4')))
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
for f in ['watch-original.apk','watch-rebuilt.apk','widgethost.apk']:check('install '+f,'Success' in adb('install','-r','-g',f))
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','cmd','connectivity','airplane-mode','enable');time.sleep(2);adb('shell','setprop','persist.sys.timezone','UTC')
adb('shell','settings','put','global','window_animation_scale','1');adb('shell','settings','put','global','transition_animation_scale','1');adb('shell','settings','put','global','animator_duration_scale','1')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['ar','en']:
  slug=name+'-'+lang;reset();zone();adb('shell','date','103010002026.00');adb('logcat','-c')
  for page in range(4):
   launch(page);shot(slug+'-page'+str(page))
   if page in [1,3]:scroll();shot(slug+'-page'+str(page)+'-scroll')
  # Real pager transitions through all four pages, then nested theme enter/edge-back.
  launch(0);p=record(slug+'-navigation')
  for i in range(3):adb('shell','input','swipe','360','227','90','227','420');time.sleep(.6)
  shot(slug+'-pager-settings');found=choose('لون السمة' if lang=='ar' else 'Theme color');check(slug+' theme entered via row',found);shot(slug+'-theme-entered');adb('shell','input','swipe','5','227','420','227','500');time.sleep(.6);shot(slug+'-edge-back');stop(p,slug+'-navigation')
  for sub in range(1,9):
   launch(3,sub);shot(slug+'-sub'+str(sub));scroll();shot(slug+'-sub'+str(sub)+'-scroll');adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(.5);shot(slug+'-sub'+str(sub)+'-back')
  # Every pattern, theme and font choice: press, inspect persisted state and reopen.
  groups=[(1,'pat',[(1,'ربع الحزب','Rub el Hizb'),(8,'نجوم ومعينات','Stars and diamonds'),(9,'شبكة سداسية','Hex grid'),(10,'نجمة ثمانية متقاطعة','Crossed octagram'),(11,'أرابيسك دائري','Round arabesque')]),(2,'theme',[(0,'ذهبي','Gold'),(1,'فيروزي','Turquoise'),(2,'أزرق','Blue'),(3,'أخضر','Green')]),(7,'font_choice',[(0,'خط التطبيق','App font'),(1,'خط التطبيق الخفيف','App font light'),(2,'خط التطبيق العريض','App font bold'),(3,'خط النظام','System font'),(4,'خط القاهرة','Cairo'),(5,'خط النسخ','Naskh'),(6,'خط أميري','Amiri'),(7,'خط الرقعة (للمناسبات)','Ruqaa (occasions)')])]
  for sub,key,opts in groups:
   for value,ar,en in opts:
    launch(3,sub);ok=choose(en if lang=='en' else ar);n=slug+'-'+key+str(value);check(n+' click',ok);values=prefs(n+'-prefs');check(n+' persisted',values.get(key)==str(value),values.get(key,''));shot(n);launch(0);shot(n+'-reopen')
  reset();launch(3,3);check(slug+' sync mode press',choose('Sync with phone' if lang=='en' else 'مزامنة مع الهاتف'));check(slug+' sync saved',prefs(slug+'-sync-prefs').get('locmode')=='0');shot(slug+'-sync')
  reset()
  for mode,kind in [('ranged',0),('short',0),('short',1),('short',2),('long',3),('short',4),('short',8),('tile',0)]:
   adb('shell','am','force-stop',pkg);zone();adb('shell','am','start','-W','-n',pkg+'/com.wathakker.wear.PreviewActivity','--es','mode',mode,'--ei','kind',str(kind),'--ei','countdown_sec','180','--es','cd_style','stopwatch');time.sleep(2);shot(slug+'-provider-'+mode+str(kind))
  (out/(slug+'-logcat.txt')).write_text(adb('logcat','-d'));(out/(slug+'-package.txt')).write_text(adb('shell','dumpsys','package',pkg))
comparisons=[]
for n,a in states.items():
 if n.startswith('original-'):
  other='comparison-'+n[len('original-'):]
  if other in states:comparisons.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(comparisons,ensure_ascii=False,indent=2));print('checks',len(checks),'failures',sum(not x['passed'] for x in checks),flush=True)
