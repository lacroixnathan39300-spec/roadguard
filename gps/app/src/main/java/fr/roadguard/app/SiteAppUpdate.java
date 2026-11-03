package fr.roadguard.app;
import android.app.*;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import android.widget.*;import java.io.*;import java.security.*;import org.json.*;
/** Authenticated, bounded download; digest, identity and signing key checked before opening Android installer. */
final class SiteAppUpdate {
 private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();private static volatile boolean downloading;
 static boolean newer(String available,String installed){String[] a=available.split("[^0-9]+"),b=installed.split("[^0-9]+");for(int i=0;i<Math.max(a.length,b.length);i++){int x=i<a.length&&!a[i].isEmpty()?Integer.parseInt(a[i]):0,y=i<b.length&&!b[i].isEmpty()?Integer.parseInt(b[i]):0;if(x!=y)return x>y;}return false;}
 static void check(MainActivity a,boolean manual){
  if(a.community==null||!a.accountConnected()){if(manual)Toast.makeText(a,"Connecte ton compte ROADGUARD pour les mises à jour",Toast.LENGTH_LONG).show();return;}
  android.content.SharedPreferences p=a.getPreferences(0);long now=System.currentTimeMillis();if(!manual&&now-p.getLong("update_checked_at",0)<21600000)return;p.edit().putLong("update_checked_at",now).apply();if(manual)Toast.makeText(a,"Recherche de mise à jour…",Toast.LENGTH_SHORT).show();
  worker.execute(()->{try{String app=a.getPackageName().contains("dashwheel")?"drive":"gps";JSONObject response=new JSONObject(new String(a.community.requestBytes("/api/updates/latest?application="+app,"GET",null,null,System.currentTimeMillis()+12000,32000),"UTF-8"));JSONObject release=response.optJSONObject("release");boolean available=release!=null&&newer(release.getString("version"),a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName);p.edit().putString("update_available_version",available?release.getString("version"):"").apply();
   a.runOnUiThread(()->{if(a.isDestroyed()||a.isFinishing())return;ReferenceUi.refreshNow(a);if(!available){if(manual)Toast.makeText(a,"Application à jour",Toast.LENGTH_LONG).show();return;}
    if(!manual){Toast.makeText(a,"Mise à jour ROADGUARD disponible · Réglages > Mises à jour",Toast.LENGTH_LONG).show();return;}
    new AlertDialog.Builder(a).setTitle("Mise à jour disponible · "+release.optString("version")).setMessage("Télécharger "+Math.round(release.optLong("size")/1000000d)+" Mo depuis le site ROADGUARD ? Android confirmera l’installation. Tes données restent conservées.").setNegativeButton("Plus tard",null).setPositiveButton("Télécharger",(d,w)->download(a,release)).show();
   });
  }catch(Exception e){if(manual)a.runOnUiThread(()->{if(!a.isDestroyed())Toast.makeText(a,"Vérification indisponible · réessaie plus tard. "+e.getMessage(),Toast.LENGTH_LONG).show();});}});
 }
 private static synchronized void download(MainActivity a,JSONObject release){if(downloading){Toast.makeText(a,"Téléchargement déjà en cours",Toast.LENGTH_SHORT).show();return;}downloading=true;
  java.util.concurrent.atomic.AtomicBoolean cancel=new java.util.concurrent.atomic.AtomicBoolean();ProgressDialog progress=downloadProgress(a,cancel);
  worker.execute(()->{File target=new File(a.getCacheDir(),"roadguard-update.apk"),partial=new File(a.getCacheDir(),"roadguard-update.part.apk");try{
   String path=release.getString("path"),sha=release.getString("sha256");long expected=release.getLong("size");if(!path.matches("/api/updates/download/(drive|gps)-apk-[a-f0-9]{64}")||!sha.matches("[a-f0-9]{64}")||expected<=0||expected>512L*1024*1024)throw new IOException("Version invalide");if(a.getCacheDir().getUsableSpace()<expected+16000000)throw new IOException("Espace disponible insuffisant");
   a.community.downloadRelease(path,partial,expected,cancel,n->a.runOnUiThread(()->{if(!a.isDestroyed())progress.setProgress((int)(n*100/expected));}));
   MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(partial)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));if(!sha.equals(hash.toString()))throw new IOException("Contrôle du fichier échoué");
   PackageManager pm=a.getPackageManager();PackageInfo archive=pm.getPackageArchiveInfo(partial.getPath(),PackageManager.GET_SIGNATURES),current=pm.getPackageInfo(a.getPackageName(),PackageManager.GET_SIGNATURES);if(archive==null||!a.getPackageName().equals(archive.packageName)||archive.versionCode<=current.versionCode||archive.signatures==null||current.signatures==null||!java.util.Arrays.equals(archive.signatures,current.signatures)||archive.applicationInfo==null||archive.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)throw new IOException("APK incompatible ou signature différente");if(!partial.renameTo(target))throw new IOException("Impossible de finaliser le fichier");
   a.runOnUiThread(()->{if(a.isDestroyed()||a.isFinishing())return;progress.dismiss();new AlertDialog.Builder(a).setTitle("Mise à jour prête").setMessage("Fichier vérifié. Si nécessaire, autorise l’installation depuis ROADGUARD dans Android puis reviens ici.").setNeutralButton("Autoriser",(d,w)->openInstallPermission(a)).setNegativeButton("Plus tard",null).setPositiveButton("Installer",(d,w)->install(a)).show();});
  }catch(Exception e){partial.delete();a.runOnUiThread(()->{if(!a.isDestroyed()){progress.dismiss();Toast.makeText(a,cancel.get()?"Téléchargement annulé":"Mise à jour non installée : "+e.getMessage(),Toast.LENGTH_LONG).show();}});}finally{downloading=false;}});
 }
 static ProgressDialog downloadProgress(MainActivity a,java.util.concurrent.atomic.AtomicBoolean cancel){
  ProgressDialog progress=new ProgressDialog(a);progress.setTitle("Mise à jour ROADGUARD");progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);progress.setMax(100);progress.setCancelable(false);
  progress.setButton(DialogInterface.BUTTON_NEGATIVE,"Annuler",(d,w)->cancel.set(true));progress.show();return progress;
 }
 static boolean openInstallPermission(MainActivity a){
  if(a.isFinishing()||a.isDestroyed())return false;
  try{a.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));return true;}
  catch(ActivityNotFoundException|SecurityException absent){
   try{a.startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));return true;}
   catch(ActivityNotFoundException|SecurityException unavailable){Toast.makeText(a,"Réglages d’installation indisponibles. Ouvre les réglages de sécurité Android.",Toast.LENGTH_LONG).show();return false;}
  }
 }
 static void install(MainActivity a){try{a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+a.getPackageName()+".roadguard.updates/apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){Toast.makeText(a,"Installation indisponible : "+e.getMessage(),Toast.LENGTH_LONG).show();}}
 private SiteAppUpdate(){}
}
