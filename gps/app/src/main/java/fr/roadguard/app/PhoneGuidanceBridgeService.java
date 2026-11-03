package fr.roadguard.app;

import android.app.Service;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import fr.roadguard.ipc.*;
import org.json.JSONObject;
import java.security.MessageDigest;

/** Bound service only: does not start GPS, acquire location or open an activity. */
public class PhoneGuidanceBridgeService extends Service {
  static final String COMPANION = "fr.roadguard.drive.companion";
  static final String COMPANION_CERT = "3f294ccdd3e2e5c0b7b5791807aa8dfaf50f9e0809c1ca964474e5a9c34d35cc";
  static final int MAX_SNAPSHOT = 170000;
  private static volatile PhoneGuidanceBridgeService running;
  private static volatile String latest;
  private static long sequence;
  private static long publishedAt = -60000;
  private static String previous;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final RemoteCallbackList<IGuidanceListener> callbacks = new RemoteCallbackList<>();
  private SharedPreferences prefs;
  private PhoneNavigationCommands navigation;
  private PhoneMapTiles mapTiles;
  private final SharedPreferences.OnSharedPreferenceChangeListener preferences = (p, key) -> {
    if("phone_guidance_share".equals(key)&&!p.getBoolean(key,false)&&navigation!=null)navigation.revoke();
    if ("phone_guidance_share".equals(key) || "cached_navigation".equals(key) ||
        "destination".equals(key) || "destination_label".equals(key)) publish(this, "", null);
  };

  /** Actual package UID plus pinned signing certificate, checked on every inbound call. */
  boolean trustedUid(int uid) {
    try {
      PackageManager pm = getPackageManager();
      if (pm.getApplicationInfo(COMPANION, 0).uid != uid) return false;
      byte[] pin = new byte[32];
      for (int i = 0; i < pin.length; i++) pin[i] = (byte) Integer.parseInt(COMPANION_CERT.substring(i*2,i*2+2),16);
      if (Build.VERSION.SDK_INT >= 28) return pm.hasSigningCertificate(COMPANION, pin, PackageManager.CERT_INPUT_SHA256);
      @SuppressWarnings("deprecation") PackageInfo info = pm.getPackageInfo(COMPANION, PackageManager.GET_SIGNATURES);
      if (info.signatures == null || info.signatures.length != 1) return false;
      return MessageDigest.isEqual(pin, MessageDigest.getInstance("SHA-256").digest(info.signatures[0].toByteArray()));
    } catch (Exception unavailable) { return false; }
  }
  private int authenticate() {
    int uid = Binder.getCallingUid();
    if (!trustedUid(uid)) throw new SecurityException("Unapproved ROADGUARD companion");
    return uid;
  }
  final IGuidanceBridge.Stub bridge = new IGuidanceBridge.Stub() {
    @Override public String mapInfo(){authenticate();return mapTiles.info();}
    @Override public byte[] mapTile(String catalog,String kind,int z,int x,int y){final int uid=authenticate();byte[] bytes=mapTiles.tile(catalog,kind,z,x,y);return trustedUid(uid)&&mapTiles.allowed()?bytes:null;}
    @Override public int navigationVersion() { authenticate(); return 1; }
    @Override public void navigation(String request, INavigationListener listener) {
      final int uid=authenticate();
      if(request==null||request.length()>2048||listener==null)throw new IllegalArgumentException("navigation");
      main.post(()->{if(running==PhoneGuidanceBridgeService.this&&trustedUid(uid))navigation.submit(request,listener,()->trustedUid(uid));});
    }
    @Override public void cancelNavigation(String scope) {
      authenticate(); if(scope!=null&&scope.length()<=64)main.post(()->navigation.cancel(scope));
    }
    @Override public void subscribe(IGuidanceListener listener) {
      final int uid = authenticate();
      if (listener == null) throw new IllegalArgumentException("listener");
      main.post(() -> {
        if (running != PhoneGuidanceBridgeService.this || !trustedUid(uid)) return;
        // One approved Companion process; cap abandoned subscriptions even before binder death.
        if (callbacks.getRegisteredCallbackCount() >= 2) return;
        callbacks.register(listener, uid);
        dispatch();
      });
    }
    @Override public void unsubscribe(IGuidanceListener listener) {
      authenticate(); if (listener != null) main.post(() -> callbacks.unregister(listener));
    }
    @Override public boolean requestStop(String routeId) {
      final int uid = authenticate();
      if (!PhoneNavigationStopReceiver.matches(PhoneGuidanceBridgeService.this, routeId)) return false;
      // Accepted for execution. An inactive snapshot, not this return value, confirms completion.
      return main.post(() -> {
        if (trustedUid(uid)) PhoneNavigationStopReceiver.stop(PhoneGuidanceBridgeService.this, routeId);
      });
    }
  };
  @Override public void onCreate() {
    super.onCreate(); running = this; navigation=new PhoneNavigationCommands(this);mapTiles=new PhoneMapTiles(this);
    prefs = getSharedPreferences("MainActivity",0);
    prefs.registerOnSharedPreferenceChangeListener(preferences);
    if (!prefs.getBoolean("phone_guidance_share",false) || SystemClock.elapsedRealtime()-publishedAt > 30000)
      publish(this,"",null);
  }
  @Override public IBinder onBind(Intent intent) { return bridge; }
  @Override public void onDestroy() {
    if (running == this) running = null; navigation.close();
    prefs.unregisterOnSharedPreferenceChangeListener(preferences);
    main.removeCallbacksAndMessages(null); callbacks.kill(); super.onDestroy();
  }
  /** Called before the legacy notification throttle, on foreground and background GPS fixes. */
  static synchronized void publish(Context context, String guidance, JSONObject route) {
    try {
      boolean active = route != null && context.getSharedPreferences("MainActivity",0).getBoolean("phone_guidance_share",false);
      JSONObject state = new JSONObject().put("protocol",1).put("active",active)
        .put("guidance",active ? guidance.substring(0,Math.min(1000,guidance.length())) : "")
        .put("route",active ? route : JSONObject.NULL);
      String content = state.toString(); long now = SystemClock.elapsedRealtime();
      if (content.equals(previous) && now-publishedAt < 10000) return;
      state.put("sequence",++sequence).put("at",System.currentTimeMillis());
      String raw=state.toString();
      if (raw.length()>MAX_SNAPSHOT) return;
      previous=content; publishedAt=now; latest=raw;
      PhoneGuidanceBridgeService service=running;
      if (service!=null) {service.main.removeCallbacks(service.deliver); service.main.postDelayed(service.deliver,100);}
    } catch (Exception ignored) { /* Leave the previous validated snapshot; never emit partial JSON. */ }
  }
  private final Runnable deliver = this::dispatch;
  private void dispatch() {
    String snapshot=latest;
    if (snapshot==null) return;
    int count=callbacks.beginBroadcast();
    try {for(int i=0;i<count;i++) {
      IGuidanceListener listener=callbacks.getBroadcastItem(i);
      if (!trustedUid((Integer)callbacks.getBroadcastCookie(i))) {callbacks.unregister(listener);continue;}
      try {listener.onSnapshot(snapshot);} catch(RemoteException dead) {callbacks.unregister(listener);}
    }} finally {callbacks.finishBroadcast();}
  }
}
