package fr.roadguard.app;
/** Shared phone/car confirmation, using distinct monotonic fixes and a retry cooldown. */
final class CarReroutePolicy {
  static final double ENTER_METERS=200,RETURN_METERS=120;
  static final long CONFIRM_MS=8000,MAX_GAP_MS=15000,COOLDOWN_MS=90000;
  long lastFix=-1,offSince=-1,lastRequest=-1;int count=0;
  void resetDetection(){count=0;offSince=-1;}
  void reset(){resetDetection();lastFix=-1;lastRequest=-1;}
  boolean due(long fixAt,long now,double lateral,double accuracy,double speed,boolean enabled,boolean pending){
    if(fixAt<=lastFix)return false;
    if(fixAt<=0||now<fixAt||now-fixAt>MAX_GAP_MS){resetDetection();return false;}
    if(lastFix>=0&&fixAt-lastFix>MAX_GAP_MS)resetDetection();lastFix=fixAt;
    if(!enabled||pending||!Double.isFinite(lateral)||lateral<0||!Double.isFinite(accuracy)||accuracy<=0||accuracy>35||!Double.isFinite(speed)||speed<1.5){resetDetection();return false;}
    if(lateral<RETURN_METERS){resetDetection();return false;}
    if(lateral<ENTER_METERS)return false;
    if(offSince<0)offSince=fixAt;count++;
    if(count<3||fixAt-offSince<CONFIRM_MS||lastRequest>=0&&(now<lastRequest||now-lastRequest<COOLDOWN_MS))return false;
    lastRequest=now;resetDetection();return true;
  }
}
