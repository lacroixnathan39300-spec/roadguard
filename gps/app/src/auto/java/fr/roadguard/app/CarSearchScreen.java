package fr.roadguard.app;
import android.os.Handler;
import android.os.Looper;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.*;
import androidx.lifecycle.*;
import org.json.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

/** Host-managed destination input; bounded results and no account or location upload. */
final class CarSearchScreen extends Screen {
  interface Places { JSONArray find(String text)throws Exception; }
  static final class Place {
    final double lat,lon;final String name;
    Place(double a,double b,String n){lat=a;lon=b;name=n;}
  }
  final Handler main=new Handler(Looper.getMainLooper());
  final ExecutorService io=Executors.newSingleThreadExecutor();
  final CarFavoritesScreen.Choose choose;
  Places places=text->new JSONObject(CarRouteData.get(CommunitySync.ORIGIN+"/api/v1/places?q="+URLEncoder.encode(text,"UTF-8"),200000)).getJSONArray("places");
  List<Place> results=Collections.emptyList();String query="",message="Saisis une ville, une adresse ou des coordonnées";
  boolean loading=false,closed=false;int generation=0;Runnable debounce;Future<?> lastRequest;
  CarSearchScreen(CarContext context,CarFavoritesScreen.Choose choose){super(context);this.choose=choose;
    getLifecycle().addObserver((LifecycleEventObserver)(owner,event)->{if(event==Lifecycle.Event.ON_DESTROY)close();});
  }
  static List<Place> parse(JSONArray rows,String fallback){
    List<Place> out=new ArrayList<>();Set<String> seen=new HashSet<>();
    for(int i=0;i<Math.min(50,rows.length())&&out.size()<6;i++){
      JSONObject row=rows.optJSONObject(i);if(row==null)continue;
      double lat=row.optDouble("lat",Double.NaN),lon=row.optDouble("lng",Double.NaN);
      if(!RouteRequestPolicy.coordinate(lat,lon))continue;
      String key=String.format(Locale.US,"%.6f,%.6f",lat,lon);if(!seen.add(key))continue;
      String name=row.optString("name",fallback).trim();if(name.isEmpty())name=fallback;if(name.length()>100)name=name.substring(0,100);
      out.add(new Place(lat,lon,name));
    }return out;
  }
  void search(String value,boolean immediate){
    if(closed)return;if(debounce!=null)main.removeCallbacks(debounce);int token=++generation;
    query=value==null?"":value.trim();if(query.length()>160)query=query.substring(0,160);final String requested=query;
    results=Collections.emptyList();loading=false;
    if(query.length()<2){message="Saisis au moins deux caractères";invalidate();return;}
    loading=true;invalidate();debounce=()->{
      if(closed||token!=generation)return;
      lastRequest=io.submit(()->{try{
        double[] coordinate=NavigationIntentInput.coordinates(requested);
        List<Place> found=coordinate==null?parse(places.find(requested),requested):Collections.singletonList(new Place(coordinate[0],coordinate[1],requested));
        main.post(()->publish(token,found,null));
      }catch(Exception failed){main.post(()->publish(token,Collections.emptyList(),"Recherche indisponible · réessaie avec le réseau"));}});
    };
    if(immediate)debounce.run();else main.postDelayed(debounce,450);
  }
  void publish(int token,List<Place> found,String error){if(closed||token!=generation)return;results=found;loading=false;message=error!=null?error:"Aucun résultat · précise la recherche";invalidate();}
  @Override public Template onGetTemplate(){
    SearchTemplate.Builder view=new SearchTemplate.Builder(new SearchTemplate.SearchCallback(){
      @Override public void onSearchTextChanged(String text){search(text,false);}
      @Override public void onSearchSubmitted(String text){search(text,true);}
    }).setHeaderAction(Action.BACK).setSearchHint("Ville ou adresse").setInitialSearchText(query).setShowKeyboardByDefault(true).setLoading(loading);
    if(!loading){ItemList.Builder list=new ItemList.Builder().setNoItemsMessage(message);
      for(Place p:results)list.addItem(new Row.Builder().setTitle(p.name).addText(String.format(Locale.FRANCE,"%.5f, %.5f",p.lat,p.lon)).setOnClickListener(()->{if(!closed){choose.route(p.lat,p.lon,p.name);getScreenManager().pop();}}).build());
      view.setItemList(list.build());
    }return view.build();
  }
  void close(){if(closed)return;closed=true;generation++;main.removeCallbacksAndMessages(null);io.shutdownNow();}
}
