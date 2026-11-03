package fr.roadguard.app;
public class TestMotion {
  static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
  static boolean fix(GpsMotion m,long t,boolean gnss,double speed,double bearing){return m.accept(t,gnss,6,speed,1,bearing,4);}
  public static void main(String[] args){
    GpsMotion m=new GpsMotion();check(fix(m,1000,true,25,359),"GNSS accepté");
    check(Math.abs(m.speed(1000)-90)<.001,"90 km/h");check(Math.abs(m.speedMetersPerSecond(1000)-25)<.000001,"tracking/rerouting uses metres per second");check(!fix(m,2000,false,Double.NaN,Double.NaN),"réseau intercalé ignoré");check(m.speed(2000)==90,"pas de faux zéro réseau");
    check(fix(m,2500,true,Double.NaN,Double.NaN),"fix sans vitesse");check(m.speed(2500)==90,"vitesse manquante conservée brièvement");check(m.course()==359,"cap conservé, pas nord forcé");
    check(fix(m,3000,true,26,1),"nouveau GNSS");check(m.course()==1,"cap de la voiture actualisé");check(!fix(m,2900,true,0,180),"ordre monotone");
    check(fix(m,4000,true,0,180),"arrêt réellement mesuré");check(m.speed(4000)>90,"zéro isolé non affiché");check(fix(m,5000,true,0,180),"arrêt confirmé");check(m.speed(5000)==0,"arrêt confirmé affiché");check(m.course()==1,"pas de cap aléatoire à l’arrêt");
    check(Double.isNaN(m.speed(11001)),"signal périmé inconnu");check(!m.courseReliable(19001),"cap périmé identifié");
    check(GpsMotion.displaySpeed(0,8)==0,"correction ne crée pas de mouvement");check(GpsMotion.displaySpeed(80,5)==85,"correction +5");check(GpsMotion.displaySpeed(5,-15)==0,"correction bornée à zéro");check(Double.isNaN(GpsMotion.displaySpeed(Double.NaN,5)),"inconnu reste inconnu");
    check(m.accept(5500,true,9,30,8,90,4),"position sans vitesse fiable acceptée");check(m.speed(5500)==0,"vitesse imprécise non utilisée");
    m.resetCourse();check(Double.isNaN(m.course()),"réinitialisation cap");check(fix(m,6000,true,15,74),"réacquisition");check(m.course()==74,"cap recalé en roulant");
    check("Champagnole".equals(NavigationIntentInput.query("google.navigation:q=Champagnole")),"navigation vocale");check(NavigationIntentInput.coordinates(NavigationIntentInput.query("geo:0,0?q=46.736%2C5.891%28Maison%29"))[0]==46.736,"geo avec libellé");check(NavigationIntentInput.query("https://example.org?q=xx")==null,"schéma refusé");check(NavigationIntentInput.query("geo:0,0?q=%zz")==null,"encodage invalide refusé");check(NavigationIntentInput.coordinates("NaN,5")==null,"NaN refusé");
    check(Math.abs(CarProjection.deltaX(CarProjection.x(-179.99,1024),CarProjection.x(179.99,1024),1024))<1,"antiméridien");check(Double.isFinite(CarProjection.y(90,1024)),"pôles bornés");
    m.invalidateMotion();check(Double.isNaN(m.speedMetersPerSecond(6100)),"unknown motion remains unknown in m/s");check(Double.isNaN(m.speed(6100)),"GPS disabled clears stale speed");check(!m.courseReliable(6100),"GPS disabled clears stale course");check(!fix(m,5900,true,22,180),"invalidating motion retains out-of-order guard");check(fix(m,7000,true,22,180),"fresh GPS resumes after interruption");
    System.out.println("PASS motion: GNSS/network, missing speed/course, true stop, stale/quality guards, compensation, heading reset, navigation intents and projection");
  }
}
