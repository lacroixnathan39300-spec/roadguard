import fr.roadguard.app.NearbyOfflinePlanner;
import java.util.*;
public class PlannerTest {
 public static void main(String[] args){
  List<NearbyOfflinePlanner.Cell> a=NearbyOfflinePlanner.plan(46.7,5.9,50,5);
  if(a.size()<100||a.size()>600)throw new AssertionError("count "+a.size());
  for(int i=1;i<a.size();i++)if(a.get(i).distanceKm<a.get(i-1).distanceKm)throw new AssertionError("order");
  if(a.get(0).distanceKm>5)throw new AssertionError("near");
  if(NearbyOfflinePlanner.distanceKm(46.7,5.9,46.7,5.9)!=0)throw new AssertionError("distance");
  System.out.println("OK sectors="+a.size()+" first="+a.get(0).distanceKm);
 }
}
