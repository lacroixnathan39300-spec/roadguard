package fr.roadguard.app;

import android.app.Application;
import android.os.*;
import android.widget.*;
import java.lang.reflect.*;
import java.text.Collator;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class CountryManagerRuntimeTest {
  static Object get(Object instance,String name)throws Exception{Field field=RadarDownloadActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(instance);}
  static void put(Object instance,String name,Object value)throws Exception{Field field=RadarDownloadActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(instance,value);}
  @Test public void countryScreenAppliesInsetsWithoutCallingNewApisOnAndroidEight()throws Exception{
    org.robolectric.android.controller.ActivityController<RadarDownloadActivity> controller=Robolectric.buildActivity(RadarDownloadActivity.class).setup();
    try{RadarDownloadActivity activity=controller.get();android.view.ViewGroup content=activity.findViewById(android.R.id.content);android.view.View root=content.getChildAt(0);
      android.view.WindowInsets insets;
      if(Build.VERSION.SDK_INT>=30)insets=new android.view.WindowInsets.Builder().setInsets(android.view.WindowInsets.Type.systemBars(),android.graphics.Insets.of(1,24,2,40)).setInsets(android.view.WindowInsets.Type.ime(),android.graphics.Insets.of(0,0,0,160)).setVisible(android.view.WindowInsets.Type.ime(),true).build();
      else {
        // The old constructor exists on API 26 but is no longer in SDK 36 stubs.
        Constructor<android.view.WindowInsets> legacy=android.view.WindowInsets.class.getDeclaredConstructor(android.graphics.Rect.class);
        legacy.setAccessible(true);insets=legacy.newInstance(new android.graphics.Rect(1,24,2,40));
      }
      root.dispatchApplyWindowInsets(insets);int base=Math.round(12*activity.getResources().getDisplayMetrics().density);
      assertEquals(base+24,root.getPaddingTop());assertEquals(base+1,root.getPaddingLeft());assertEquals(base+2,root.getPaddingRight());assertEquals(base+(Build.VERSION.SDK_INT>=30?160:40),root.getPaddingBottom());
    }finally{controller.pause().stop().destroy();}
  }
  @Test public void countrySearchIgnoresAccentsAndListUsesFrenchOrder()throws Exception{
    org.robolectric.android.controller.ActivityController<RadarDownloadActivity> controller=Robolectric.buildActivity(RadarDownloadActivity.class).setup();
    try{RadarDownloadActivity activity=controller.get();String[][] countries=(String[][])get(activity,"countries");Collator collator=Collator.getInstance(Locale.FRANCE);
      for(int i=1;i<countries.length;i++)assertTrue(collator.compare(countries[i-1][1],countries[i][1])<=0);
      EditText search=(EditText)get(activity,"search");search.setText("republique");
      LinearLayout list=(LinearLayout)get(activity,"countryList");assertEquals(1,list.getChildCount());
      search.setText("sUeDe");assertEquals(1,list.getChildCount());
      search.setText("zzzz");assertEquals(0,list.getChildCount());
    }finally{controller.pause().stop().destroy();}
  }
  @Test public void localCatalogIsSeparateFromDownloadAndUnsupportedUpdates()throws Exception{
    org.robolectric.android.controller.ActivityController<RadarDownloadActivity> controller=Robolectric.buildActivity(RadarDownloadActivity.class).setup();
    try{RadarDownloadActivity activity=controller.get();put(activity,"inventoryLoading",false);
      Map<String,Integer> fixture=new HashMap<>();fixture.put("FR",7);fixture.put("AT",9);put(activity,"activeCounts",fixture);
      EditText search=(EditText)get(activity,"search");search.setText("france");
      LinearLayout row=(LinearLayout)((LinearLayout)get(activity,"countryList")).getChildAt(0);
      Button button=(Button)row.getChildAt(2);assertTrue(button.isEnabled());assertTrue(button.getText().toString().contains("Installer"));
      LinearLayout details=(LinearLayout)row.getChildAt(1);assertTrue(((TextView)details.getChildAt(1)).getText().toString().contains("7 points"));
      assertFalse(button.getText().toString().contains("Actif"));
      search.setText("autriche");row=(LinearLayout)((LinearLayout)get(activity,"countryList")).getChildAt(0);button=(Button)row.getChildAt(2);
      assertFalse(button.isEnabled());details=(LinearLayout)row.getChildAt(1);assertTrue(((TextView)details.getChildAt(1)).getText().toString().contains("9 points"));
      assertTrue(((TextView)details.getChildAt(1)).getText().toString().contains("mise à jour indisponible"));
    }finally{controller.pause().stop().destroy();}
  }
}
