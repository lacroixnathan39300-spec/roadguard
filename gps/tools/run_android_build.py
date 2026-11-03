#!/usr/bin/env python3
"""Run the authorized host build with the current process's network settings.
Never copy ephemeral proxy credentials into the delivered project."""
from pathlib import Path
import argparse,os,re,subprocess,sys,urllib.parse,shutil
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--gradle',required=True);p.add_argument('--sdk',required=True);p.add_argument('--cache',required=True);p.add_argument('--variant',choices=['phone','auto','tests'],required=True);p.add_argument('--test',action='append',default=[])
a=p.parse_args();version=re.search(r"versionName\s+'([^']+)'",(ROOT/'app/build.gradle').read_text()).group(1)
cache=Path(a.cache).resolve();cache.mkdir(parents=True,exist_ok=True)
lines=['org.gradle.daemon=false','org.gradle.workers.max=2','systemProp.org.gradle.internal.http.connectionTimeout=20000','systemProp.org.gradle.internal.http.socketTimeout=30000']
for scheme in ['http','https']:
 proxy=os.environ.get(scheme.upper()+'_PROXY') or os.environ.get(scheme+'_proxy')
 if not proxy:continue
 parts=urllib.parse.urlsplit(proxy)
 if parts.hostname:
  lines += ['systemProp.'+scheme+'.proxyHost='+parts.hostname,'systemProp.'+scheme+'.proxyPort='+str(parts.port or 80)]
  if parts.username:lines += ['systemProp.'+scheme+'.proxyUser='+urllib.parse.unquote(parts.username)]
  if parts.password:lines += ['systemProp.'+scheme+'.proxyPassword='+urllib.parse.unquote(parts.password)]
properties=cache/'gradle.properties';properties.write_text('\n'.join(lines)+'\n');properties.chmod(0o600)
(ROOT/'local.properties').write_text('sdk.dir='+str(Path(a.sdk).resolve())+'\n')
env=os.environ.copy();env['GRADLE_USER_HOME']=str(cache);env['ANDROID_SDK_ROOT']=str(Path(a.sdk).resolve())
task=':app:testDebugUnitTest' if a.variant=='tests' else ':app:assembleDebug'
# This legacy property changes source sets inside the same debug variant. Clean
# before switching to avoid stale generated R classes in D8's intermediate cache.
generated=ROOT/'app/build'
if generated.is_symlink():raise RuntimeError('Refusing to clean a linked build directory')
if generated.exists():shutil.rmtree(generated)
args=[a.gradle,':app:clean',task,'-ProadguardAndroidAuto='+('true' if a.variant=='auto' else 'false'),'--no-daemon','--console=plain','--max-workers=2']
if a.variant=='tests':
 # Test workers need the same current network settings as Gradle. Keep them
 # outside the project; never persist a proxy address or credential in source.
 init=cache/'roadguard-test-network.gradle'
 init.write_text('''gradle.projectsEvaluated {
  allprojects {
    tasks.withType(org.gradle.api.tasks.testing.Test).configureEach {
      ['http','https'].each { scheme ->
        ['proxyHost','proxyPort','proxyUser','proxyPassword'].each { option ->
          def name = scheme + '.' + option
          def value = System.getProperty(name)
          if (value) systemProperty name, value
        }
      }
    }
  }
}
''')
 args += ['--init-script',str(init)]
for test in a.test:args += ['--tests',test]
log=ROOT/'verification'/('gradle-'+a.variant+'-'+version+'.log')
with log.open('w') as out:
 process=subprocess.Popen(args,cwd=ROOT,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
 for line in process.stdout:out.write(line);out.flush();print(line,end='',flush=True)
 code=process.wait();out.write('\nGRADLE_EXIT='+str(code)+'\n');print('GRADLE_EXIT='+str(code),flush=True)
sys.exit(code)
