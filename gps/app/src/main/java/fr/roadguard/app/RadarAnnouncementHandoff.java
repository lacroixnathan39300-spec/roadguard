package fr.roadguard.app;

/** Keep the first radar announcement audible before proximity tones take over. */
final class RadarAnnouncementHandoff {
  static boolean allowBeeps(boolean voiceEnabled,boolean announced,boolean speechPending){
    if(!voiceEnabled)return true;
    if(speechPending)return false;
    return announced;
  }
  private RadarAnnouncementHandoff(){}
}
