"""Paired phone menu behavior audit. Never sends alerts or changes external accounts."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[];states={}
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=60);return (r.stdout+r.stderr).decode(errors='replace')
def tree():
 x=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((s.split('=',1)[1] for s in x.splitlines() if s.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def check(n,ok,detail=''):checks.append(dict(name=n,passed=bool(ok),detail=detail));print(n,bool(ok),detail,flush=True)
def tap(label):
 root=ET.fromstring(tree());parents={c:p for p in root.iter() for c in p}
 for n in root.iter('node'):
  if label in [n.get('text'),n.get('content-desc')]:
   target=n
   while target.get('clickable')!='true' and target in parents:target=parents[target]
   if target.get('clickable')!='true':continue
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
   if len(b)==4 and b[2]>b[0] and b[3]>b[1] and 90<b[1]<2220:
    adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(1.5);return True
 return False
def foreground(pkg):return any('mCurrentFocus=' in x and pkg in x for x in adb('shell','dumpsys','window').splitlines())
def shot(n):
 x=tree();(out/'checks.json').write_text(json.dumps(checks,indent=2,ensure_ascii=False));(out/(n+'.xml')).write_text(x);(out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
 tokens=[(z.get('text',''),z.get('content-desc',''),z.get('checked','')) for z in ET.fromstring(x).iter('node') if z.get('text') or z.get('content-desc')];states[n]=tokens
 check(n+' foreground',foreground(pkg));return tokens
def record(n):return subprocess.Popen(['adb','shell','screenrecord','--time-limit','45',f'/sdcard/{n}.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
def stoprecord(proc,n):
 adb('shell','pkill','-INT','screenrecord')
 try:proc.wait(timeout=8)
 except:proc.kill()
 adb('pull',f'/sdcard/{n}.mp4',str(out/(n+'.mp4')))
def swipe(up=True):adb('shell','input','swipe','540','1800' if up else '650','540','650' if up else '1800','400');time.sleep(.8)
def launch():adb('shell','am','force-stop',pkg);adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(4)
def openroute(label):
 launch()
 if not tap('Menu' if lang=='en' else 'القائمة'):return False
 return tap(label)
adb('wait-for-device');adb('root');time.sleep(4);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','am','force-stop','com.google.android.apps.nexuslauncher');adb('shell','input','keyevent','KEYCODE_BACK')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  slug=name+'-'+lang;adb('shell','am','force-stop',pkg);adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');adb('shell','date','103010002026.00')
  folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  xml=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{xml}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  routes=['Favorites','Reminders','Statistics','Tags','Date converter','Qibla','Tasbeeh','Fajr alarm','Settings','About'] if lang=='en' else ['المفضلة','التنبيهات','الإحصائيات','التصنيفات','محول التاريخ','القبلة','السبحة','منبه صلاة الفجر','الإعدادات','حول التطبيق']
  for idx,label in enumerate(routes):
   if idx not in [1,2,8,9]:continue
   tag=slug+'-route'+str(idx);proc=record(tag+'-motion');ok=openroute(label);check(tag+' opened '+label,ok);shot(tag)
   if not ok:stoprecord(proc,tag+'-motion');continue
   if idx==3:
    check(tag+' add dialog',tap('Add tag' if lang=='en' else 'إضافة تصنيف'));shot(tag+'-add');adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1);shot(tag+'-cancel')
   elif idx==6:
    # Click the displayed count itself, which is inside the recovered tappable ring.
    before=tree();nums=[z.get('text') for z in ET.fromstring(before).iter('node') if re.fullmatch(r'\d+',z.get('text','')) and z.get('text') not in ['33','99','100']]
    value=nums[0] if nums else '0';hit=tap(value);shot(tag+'-increment');after=tree();check(tag+' increment',hit and str(int(value)+1) in [z.get('text') for z in ET.fromstring(after).iter('node')]);check(tag+' target99',tap('99'));shot(tag+'-target');launch();openroute(label);shot(tag+'-reopen')
   elif idx==8:
    stoprecord(proc,tag+'-motion');proc=None
    nested=['App font','Appearance','Language','Lock screen widgets','Smartwatch settings'] if lang=='en' else ['خط التطبيق','المظهر','اللغة','ويدجت شاشة القفل','إعدادات الساعة الذكية']
    for j,entry in enumerate(nested):
     openroute(label);found=False
     for k in range(4):
      if tap(entry):found=True;break
      swipe()
     check(tag+' nested '+entry,found);shot(tag+'-nested'+str(j))
     if j in [0,1,2]:
      choices=([('App font light','خط التطبيق الخفيف'),('Dark','داكن'),('English','English')][j]);choice=choices[0 if lang=='en' else 1]
      proc2=record(tag+'-choice'+str(j));hit=tap(choice);check(tag+' staged choice '+str(j),hit);shot(tag+'-pending'+str(j))
      check(tag+' cancel '+str(j),tap('Cancel' if lang=='en' else 'إلغاء'));shot(tag+'-cancel'+str(j));stoprecord(proc2,tag+'-choice'+str(j))
     else:adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(.7)
   elif idx==9:
    swipe();shot(tag+'-scroll')
   adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(.8);check(tag+' back stays',foreground(pkg))
   if proc:stoprecord(proc,tag+'-motion')
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))
# Semantic comparison is evidence, not a blanket pass: timing, counts and source/APK conflicts need inspection.
comparisons=[]
for n,a in list(states.items()):
 if n.startswith('original-'):
  other='comparison-'+n[len('original-'):]
  if other in states:comparisons.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'checks.json').write_text(json.dumps(checks,indent=2,ensure_ascii=False));(out/'semantic-comparison.json').write_text(json.dumps(comparisons,indent=2,ensure_ascii=False));print('failed',sum(not x['passed'] for x in checks),'of',len(checks))
