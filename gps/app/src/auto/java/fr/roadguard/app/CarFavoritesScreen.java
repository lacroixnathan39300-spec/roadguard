package fr.roadguard.app;
import android.content.SharedPreferences;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.*;
import androidx.core.graphics.drawable.IconCompat;
import java.util.*;
import org.json.*;

/** One screen, bounded pages: every saved favorite stays reachable without deep back stacks. */
final class CarFavoritesScreen extends Screen {
  interface Choose { void route(double lat,double lon,String label); }
  final SharedPreferences prefs;final Choose choose;int page=0;
  CarFavoritesScreen(CarContext context,Choose choose){super(context);prefs=context.getSharedPreferences("MainActivity",0);this.choose=choose;}
  Action pageAction(int icon,int delta){return new Action.Builder().setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),icon)).build()).setOnClickListener(()->{page+=delta;invalidate();}).build();}
  @Override public Template onGetTemplate(){
    List<JSONObject> valid=new ArrayList<>();try{JSONArray all=new JSONArray(prefs.getString("destination_favorites","[]"));for(int i=0;i<all.length()&&i<128;i++){JSONObject f=all.optJSONObject(i);if(f!=null&&RouteRequestPolicy.coordinate(f.optDouble("lat",Double.NaN),f.optDouble("lon",Double.NaN)))valid.add(f);}}catch(Exception ignored){}
    if(valid.isEmpty())return new MessageTemplate.Builder("Enregistre une destination dans les favoris du téléphone").setTitle("Mes lieux").setHeaderAction(Action.BACK).build();
    int pages=(valid.size()+5)/6;page=Math.max(0,Math.min(page,pages-1));ItemList.Builder items=new ItemList.Builder();
    for(int i=page*6;i<Math.min(valid.size(),(page+1)*6);i++){JSONObject f=valid.get(i);double lat=f.optDouble("lat"),lon=f.optDouble("lon");String label=f.optString("name","Favori");if(label.length()>80)label=label.substring(0,80);final String name=label;
      items.addItem(new Row.Builder().setTitle(name.isEmpty()?"Favori":name).setOnClickListener(()->{choose.route(lat,lon,name);getScreenManager().pop();}).build());
    }
    ListTemplate.Builder view=new ListTemplate.Builder().setTitle("Mes lieux · "+(page+1)+"/"+pages).setHeaderAction(Action.BACK).setSingleList(items.build());
    if(pages>1){ActionStrip.Builder actions=new ActionStrip.Builder();if(page>0)actions.addAction(pageAction(android.R.drawable.ic_media_previous,-1));if(page+1<pages)actions.addAction(pageAction(android.R.drawable.ic_media_next,1));view.setActionStrip(actions.build());}
    return view.build();
  }
}
