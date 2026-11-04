package fr.roadguard.app;
import androidx.car.app.navigation.model.Maneuver;
final class CarManeuvers {
  static Maneuver from(String instruction){
    String glyph=NavigationProgress.glyph(instruction);int type;
    if("↰".equals(glyph))type=Maneuver.TYPE_TURN_NORMAL_LEFT;
    else if("↱".equals(glyph))type=Maneuver.TYPE_TURN_NORMAL_RIGHT;
    else if("↶".equals(glyph))type=Maneuver.TYPE_U_TURN_LEFT;
    else if("⟳".equals(glyph)){
      java.util.regex.Matcher exit=java.util.regex.Pattern.compile("sortie\\s+(\\d+)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(instruction);
      if(exit.find()){int number=Integer.parseInt(exit.group(1));if(number>=1&&number<=20)return new Maneuver.Builder(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW).setRoundaboutExitNumber(number).build();}
      type=Maneuver.TYPE_UNKNOWN;
    }
    else if("⚑".equals(glyph))type=Maneuver.TYPE_DESTINATION;
    else type=Maneuver.TYPE_STRAIGHT;
    return new Maneuver.Builder(type).build();
  }
  private CarManeuvers(){}
}
