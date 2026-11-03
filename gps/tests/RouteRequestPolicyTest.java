import fr.roadguard.app.RouteRequestPolicy;
public class RouteRequestPolicyTest {
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){
  String url=RouteRequestPolicy.osrmUrl(46.7,5.9,46.8,6.0);
  check(url.equals("https://router.project-osrm.org/route/v1/driving/5.900000,46.700000;6.000000,46.800000?overview=full&geometries=geojson&steps=true"));
  check(RouteRequestPolicy.isSafeEndpoint("https://router.project-osrm.org/route/v1/driving"));
  check(!RouteRequestPolicy.isSafeEndpoint("http://router.project-osrm.org/route/v1/driving"));
  check(!RouteRequestPolicy.isSafeEndpoint("https://evil.example/route"));
  check(!RouteRequestPolicy.isSafeResponse(503,"application/json",20));
  check(!RouteRequestPolicy.isSafeResponse(200,"text/html",20));
  check(!RouteRequestPolicy.isSafeResponse(200,"application/json",2_000_000));
  check(RouteRequestPolicy.isSafeResponse(200,"application/json; charset=utf-8",100));
  check(RouteRequestPolicy.responseWithinLimit(-1,1_500_000));
  check(!RouteRequestPolicy.responseWithinLimit(-1,1_500_001));
  check(!RouteRequestPolicy.responseWithinLimit(1_500_001,1));
  check(RouteRequestPolicy.coordinate(90,180));check(!RouteRequestPolicy.coordinate(Double.NaN,0));check(!RouteRequestPolicy.coordinate(0,181));
  check(RouteRequestPolicy.isValidRouteSummary(1,1,2));
  check(RouteRequestPolicy.isValidRouteSummary(RouteRequestPolicy.MAX_ROUTE_METERS,RouteRequestPolicy.MAX_ROUTE_SECONDS,RouteRequestPolicy.MAX_ROUTE_POINTS));
  check(!RouteRequestPolicy.isValidRouteSummary(0,1,2));
  check(!RouteRequestPolicy.isValidRouteSummary(1,0,2));
  check(!RouteRequestPolicy.isValidRouteSummary(Double.NaN,1,2));
  check(!RouteRequestPolicy.isValidRouteSummary(RouteRequestPolicy.MAX_ROUTE_METERS+1,1,2));
  check(!RouteRequestPolicy.isValidRouteSummary(1,RouteRequestPolicy.MAX_ROUTE_SECONDS+1,2));
  check(!RouteRequestPolicy.isValidRouteSummary(1,1,1));
  check(!RouteRequestPolicy.isValidRouteSummary(1,1,RouteRequestPolicy.MAX_ROUTE_POINTS+1));
  boolean failed=false;try{RouteRequestPolicy.osrmUrl(91,0,0,0);}catch(IllegalArgumentException e){failed=true;}check(failed);
  System.out.println("RouteRequestPolicyTest OK");
 }
}
