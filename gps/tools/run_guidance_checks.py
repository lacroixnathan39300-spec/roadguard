#!/usr/bin/env python3
"""Focused lot 286 checks: real policies/tracker/gate, explicit Android prefs/TTS adapters only."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
names='GuidanceAudioPolicy SpeechAudioGate AlertSpeechTracker AlertMilestones RadarIdentity BundledRadarCatalog RadarAlertPolicy DangerTypePolicy CarReroutePolicy GpsMotion AppVisibilityPolicy BubblePlacement NavigationProgress RouteRequestPolicy RadarApproachFilter NavigationIntentInput DestinationCoordinates CarProjection'.split()
tests=['audio-tests/TestGuidanceAudio.java','audio-tests/TestSpeechPriority.java','speech-tests/TestSpeechMilestones.java','TestRerouteConfirmation.java','TestBubbleTransitions.java','motion-tests/TestMotion.java','TestRouteContinuity.java']
adapters=['android/content/Context.java','android/content/SharedPreferences.java','android/content/res/AssetManager.java']
logs=[]
def run(label,args):
 p=subprocess.run(list(map(str,args)),cwd=root,capture_output=True,text=True,timeout=90);out=p.stdout+p.stderr
 logs.append(label+'\n'+out+'\nexit='+str(p.returncode));print(label+'\n'+out,flush=True)
 if p.returncode:raise SystemExit(p.returncode)
try:
 with tempfile.TemporaryDirectory(prefix='roadguard-guidance-') as build:
  run('compile-java8',['java','com.sun.tools.javac.Main','--release','8','-d',build]+[root/'app/src/main/java/fr/roadguard/app'/(n+'.java') for n in names]+[root/'tools'/t for t in tests]+[root/'tools/jvm-support'/a for a in adapters]+list((root/'tools/speech-tests/android').rglob('*.java')))
  for test in ['TestGuidanceAudio','TestSpeechPriority','TestSpeechMilestones','TestRerouteConfirmation','TestBubbleTransitions','TestMotion','TestRouteContinuity']:run(test,['java','-cp',build,'fr.roadguard.app.'+test])
finally:
 (root/'verification/guidance-11.14.286.log').write_text('\n\n'.join(logs)+'\n')
