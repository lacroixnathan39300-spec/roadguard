#!/usr/bin/env python3
"""Compile the production extractor without any vendor JAR, as CodeAssist does."""
from pathlib import Path
import shutil, subprocess, tempfile

root=Path(__file__).resolve().parents[1]
source=root/'app/src/main/java/fr/roadguard/app/RadarSevenZipExtractor.java'
with tempfile.TemporaryDirectory(prefix='roadguard-no-jar-') as temporary:
    path=Path(temporary)
    stub=path/'Stubs.java'
    stub.write_text('''package fr.roadguard.app;
import java.io.*;
class CanonicalRadarStore {
  static final long MAX_BYTES=33554432;
  interface Cancellation { void check() throws IOException; }
  static final Cancellation NONE=()->{};
}
class RadarArchivePolicy { static void verify(File file)throws IOException {} }
class SpeedAlertSqliteImporter { static boolean sqliteHeader(InputStream input){return true;} }
''')
    harness=path/'MissingRuntime.java'
    harness.write_text('''package fr.roadguard.app;
import java.io.*;
class MissingRuntime {
  public static void main(String[] args)throws Exception {
    try { RadarSevenZipExtractor.extract(new File(args[0]),"FR",new File(args[1]),CanonicalRadarStore.NONE); }
    catch(IOException e) {
      if(e.getMessage().contains("Bibliothèque radar 7z absente"))return;
      throw e;
    }
    throw new AssertionError("Missing library must fail visibly");
  }
}
''')
    artifact=path/'fr-camera.sqlite.7z';artifact.write_bytes(b'7z')
    compiler=['javac'] if shutil.which('javac') else ['java','-m','jdk.compiler/com.sun.tools.javac.Main']
    subprocess.run(compiler+['--release','8','-sourcepath',str(path),'-d',str(path),str(stub),str(source),str(harness)],check=True)
    subprocess.run(['java','-cp',str(path),'fr.roadguard.app.MissingRuntime',str(artifact),str(path)],check=True)
print('PASS extractor compiles with no vendor JAR and fails visibly when 7z runtime is absent')
