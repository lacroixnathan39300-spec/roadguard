package fr.roadguard.app;
import java.util.*;import org.junit.*;import static org.junit.Assert.*;
public class PhoneBackgroundGuidanceTest {
 @Test public void savedRouteSuppliesNextManeuverAndRemainingDistance(){
  NavigationProgress model=new NavigationProgress(Arrays.asList(new double[]{46,6},new double[]{46.001,6},new double[]{46.002,6}));
  MainActivity.NavStep step=new MainActivity.NavStep(46.001,6,"Tournez à droite");step.routeIndex=1;
  NavigationProgress.Position position=model.measure(46,6,0);
  String text=BackgroundAlertService.phoneGuidanceText(model,position,Collections.singletonList(step));
  assertTrue(text.contains("Reste estimé"));assertTrue(text.contains("Tournez à droite"));
 }
 @Test public void offRouteAndArrivalNeverInventAnUpcomingTurn(){
  NavigationProgress model=new NavigationProgress(Arrays.asList(new double[]{46,6},new double[]{46.001,6}));
  assertTrue(BackgroundAlertService.phoneGuidanceText(model,new NavigationProgress.Position(0,0,0,100,200),Collections.emptyList()).startsWith("Hors trajet"));
  assertEquals("Vous êtes à destination",BackgroundAlertService.phoneGuidanceText(model,new NavigationProgress.Position(0,1,100,0,0),Collections.emptyList()));
 }
}
