"""Finite selected regression for proposed dependency alignment, not all-app proof."""
import subprocess,pathlib,shutil,json
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);results=[]
for scenario in ['phone_settings_controls','phone_location_pickers','phone_bell_cancel']:
 work=pathlib.Path('regression-'+scenario);work.mkdir(exist_ok=True)
 for apk in ['original.apk','comparison.apk','widgethost.apk']:
  target=work/apk
  if not target.exists():target.symlink_to(pathlib.Path(apk).resolve())
 res=subprocess.run(['python3','-u',str(pathlib.Path('tools/parity',scenario+'.py').resolve())],cwd=work)
 data=work/'evidence'
 if data.exists():shutil.copytree(data,out/scenario,dirs_exist_ok=True)
 checks=json.loads((data/'checks.json').read_text()) if (data/'checks.json').exists() else []
 results.append({'scenario':scenario,'exit_code':res.returncode,'checks':len(checks),'failed':[x for x in checks if not x.get('passed')]});(out/'regression-summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2));print(results[-1],flush=True)
assert all(x['exit_code']==0 and x['checks'] and not x['failed'] for x in results),'See scoped regression evidence'
