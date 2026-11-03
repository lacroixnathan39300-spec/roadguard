package fr.roadguard.app;
import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Native layout/state integration. Requires Android/Robolectric; not run by the JVM script. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class MapOverlayRuntimeTest {
 public static class LayoutActivity extends MainActivity {
  @Override void showStartupScreen(){if(community!=null){community.destroy();community=null;}}
 }
 org.robolectric.android.controller.ActivityController<LayoutActivity> controller;LayoutActivity a;
 @Before public void create(){controller=Robolectric.buildActivity(LayoutActivity.class).create();a=controller.get();}
 @After public void close(){controller.destroy();}
 void fixture(int width,int height){
  a.frame=new FrameLayout(a);a.setContentView(a.frame);a.map=new WebView(a);a.frame.addView(a.map,new FrameLayout.LayoutParams(-1,-1));
  a.mapTopOverlay=new LinearLayout(a);a.mapTopOverlay.setOrientation(LinearLayout.VERTICAL);
  TextView guidance=new TextView(a);guidance.setMinHeight(a.pxDp(120));guidance.setText("Tourner à droite");a.mapTopOverlay.addView(guidance);
  a.inlineMapSpeed=new TextView(a);a.inlineMapSpeed.setVisibility(View.GONE);a.mapTopOverlay.addView(a.inlineMapSpeed);
  a.mapTopScroll=new ScrollView(a);a.mapTopScroll.addView(a.mapTopOverlay);FrameLayout.LayoutParams top=new FrameLayout.LayoutParams(a.pxDp(width-20),-2,Gravity.TOP|Gravity.START);top.setMargins(a.pxDp(10),a.pxDp(10),a.pxDp(10),0);a.frame.addView(a.mapTopScroll,top);
  a.driveSheet=new LinearLayout(a);a.frame.addView(a.driveSheet,new FrameLayout.LayoutParams(-1,a.pxDp(80),Gravity.BOTTOM));
  a.mapActionRail=new LinearLayout(a);a.mapActionRail.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<3;i++)a.mapActionRail.addView(new TextView(a),new LinearLayout.LayoutParams(a.pxDp(44),a.pxDp(44)));a.frame.addView(a.mapActionRail,new FrameLayout.LayoutParams(-2,-2,Gravity.END));
  a.speedHudFrame=new FrameLayout(a);a.frame.addView(a.speedHudFrame,new FrameLayout.LayoutParams(a.pxDp(66),a.pxDp(66)));
  for(int pass=0;pass<3;pass++){a.frame.measure(View.MeasureSpec.makeMeasureSpec(a.pxDp(width),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(a.pxDp(height),View.MeasureSpec.EXACTLY));a.frame.layout(0,0,a.pxDp(width),a.pxDp(height));a.layoutMapOverlays();}
  a.frame.measure(View.MeasureSpec.makeMeasureSpec(a.pxDp(width),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(a.pxDp(height),View.MeasureSpec.EXACTLY));a.frame.layout(0,0,a.pxDp(width),a.pxDp(height));
 }
 boolean overlaps(View x,View y){return x.getLeft()<y.getRight()&&x.getRight()>y.getLeft()&&x.getTop()<y.getBottom()&&x.getBottom()>y.getTop();}
 @Test public void portraitControlsAvoidPanels(){fixture(393,760);assertEquals(View.VISIBLE,a.speedHudFrame.getVisibility());assertFalse(overlaps(a.speedHudFrame,a.mapTopScroll));assertFalse(overlaps(a.speedHudFrame,a.driveSheet));if(a.mapActionRail.getVisibility()==View.VISIBLE)assertFalse(overlaps(a.speedHudFrame,a.mapActionRail));}
 @Test public void landscapePanelsUseSeparateColumns(){fixture(800,320);assertTrue(a.mapTopScroll.getWidth()<=a.pxDp(360));assertTrue(a.driveSheet.getWidth()<=a.pxDp(380));assertTrue(a.driveSheet.getLeft()>a.mapTopScroll.getRight());assertFalse(overlaps(a.speedHudFrame,a.driveSheet));}
 @Test public void largeTextUsesReadableInlineSpeed(){android.content.res.Configuration config=new android.content.res.Configuration(a.getResources().getConfiguration());config.fontScale=1.8f;a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());fixture(393,760);assertEquals(View.GONE,a.speedHudFrame.getVisibility());assertEquals(View.VISIBLE,a.inlineMapSpeed.getVisibility());}
 @Test public void rotationStateRetainsExplorationAndSearch(){a.lastManualMapView="{\"paused\":true,\"lat\":46.75,\"lon\":5.9,\"zoom\":14}";a.destinationSearchInput=new EditText(a);a.destinationSearchInput.setText("Champagnole gare");a.driveSheetLevel=2;Bundle state=new Bundle();a.onSaveInstanceState(state);assertEquals(a.lastManualMapView,state.getString("manual_map_view"));assertEquals("Champagnole gare",state.getString("drive_search_query"));assertEquals(2,state.getInt("drive_sheet_level"));}
}
