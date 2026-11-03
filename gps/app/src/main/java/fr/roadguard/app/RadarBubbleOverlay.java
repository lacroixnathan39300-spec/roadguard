package fr.roadguard.app;

import android.content.*;
import android.app.AlertDialog;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Opt-in movable overlay. No inspection or control of the application underneath. */
final class RadarBubbleOverlay {
  private static final int COMPACT_DP=76;
  interface QuickReport {void submit(String kind);}
  static final class AccessibleBubble extends FrameLayout {AccessibleBubble(Context c){super(c);}@Override public boolean performClick(){return super.performClick();}}
  private final Context context;private final QuickReport quickReport;private final WindowManager windows;
  private final Handler main=new Handler(Looper.getMainLooper());
  private AccessibleBubble bubble;private TextView text,plus;
  private WindowManager.LayoutParams params,plusParams;
  private AlertDialog reportDialog;
  private int left,top,right,bottom,compactX,compactY;
  private int originX,originY,originCompactX,originCompactY;
  private float downX,downY;
  private boolean moved,longClicked,expanded,attached;
  private String radarName="";private int radarMeters;
  private final Runnable longPress=()->{if(bubble!=null&&!moved)bubble.performLongClick();};
  RadarBubbleOverlay(Context context,QuickReport report){this.context=context;quickReport=report;windows=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);}
  private int dp(int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
  private void bounds(){
    left=dp(4);top=dp(28);right=context.getResources().getDisplayMetrics().widthPixels-dp(4);bottom=context.getResources().getDisplayMetrics().heightPixels-dp(28);
    if(Build.VERSION.SDK_INT>=30&&windows!=null)try{WindowMetrics metrics=windows.getCurrentWindowMetrics();Rect area=metrics.getBounds();Insets insets=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=insets.left+dp(4);top=insets.top+dp(4);right=area.width()-insets.right-dp(4);bottom=area.height()-insets.bottom-dp(4);}catch(RuntimeException unavailable){}
    right=Math.max(left+1,right);bottom=Math.max(top+1,bottom);
  }
  private GradientDrawable background(){
    GradientDrawable result=new GradientDrawable(GradientDrawable.Orientation.TL_BR,expanded?new int[]{0xff64172c,0xff27121c}:new int[]{0xff252c36,0xff111720});
    result.setCornerRadius(dp(expanded?22:COMPACT_DP/2));result.setStroke(dp(2),0xfff02d54);return result;
  }
  private WindowManager.LayoutParams window(int width,int height){WindowManager.LayoutParams lp=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.START;return lp;}
  private void loadAnchor(){
    android.content.SharedPreferences saved=context.getSharedPreferences("MainActivity",0);int size=dp(COMPACT_DP);
    int rx=saved.getInt("radar_bubble_x_ratio",-1),ry=saved.getInt("radar_bubble_y_ratio",-1);
    int x=rx>=0?BubblePlacement.coordinate(rx,left,right,size):saved.getInt("radar_bubble_x",right-size-dp(4));
    int y=ry>=0?BubblePlacement.coordinate(ry,top,bottom,size):saved.getInt("radar_bubble_y",top+Math.round((bottom-top)*.54f));
    BubblePlacement.Box box=BubblePlacement.fit(left,top,right,bottom,size,size,x,y);compactX=box.x;compactY=box.y;
  }
  private BubblePlacement.Box body(){
    int compact=dp(COMPACT_DP),width=expanded?dp(248):compact;
    int height=expanded?Math.max(dp(86),Math.round(dp(18)*context.getResources().getConfiguration().fontScale*3.4f+dp(14))):compact;
    return BubblePlacement.fit(left,top,right,bottom,width,height,compactX+(compact-width)/2,compactY+(compact-height)/2);
  }
  private void saveAnchor(){
    int size=dp(COMPACT_DP);BubblePlacement.Box anchor=BubblePlacement.fit(left,top,right,bottom,size,size,compactX,compactY);compactX=anchor.x;compactY=anchor.y;
    int rx=BubblePlacement.ratio(compactX,left,right,size),ry=BubblePlacement.ratio(compactY,top,bottom,size);
    context.getSharedPreferences("MainActivity",0).edit().putInt("radar_bubble_x",compactX).putInt("radar_bubble_y",compactY).putInt("radar_bubble_x_ratio",rx).putInt("radar_bubble_y_ratio",ry).apply();
  }
  void show(){
    if(bubble!=null||windows==null||!Settings.canDrawOverlays(context))return;
    bounds();loadAnchor();bubble=new AccessibleBubble(context);bubble.setElevation(dp(18));bubble.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);bubble.setFocusable(true);
    bubble.setOnClickListener(v->{try{context.startActivity(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(RuntimeException unavailable){Toast.makeText(context,"Ouverture de ROADGUARD indisponible",Toast.LENGTH_SHORT).show();}});
    bubble.setOnLongClickListener(v->{longClicked=true;chooseReport();return true;});
    text=new TextView(context);text.setGravity(Gravity.CENTER);text.setTextColor(Color.WHITE);text.setTypeface(null,android.graphics.Typeface.BOLD);text.setMaxLines(3);text.setEllipsize(android.text.TextUtils.TruncateAt.END);text.setPadding(dp(8),dp(6),dp(8),dp(6));text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);bubble.addView(text,new FrameLayout.LayoutParams(-1,-1));
    params=window(dp(COMPACT_DP),dp(COMPACT_DP));renderText();placeBody();
    bubble.setOnTouchListener((view,event)->{
      if(params==null)return true;
      switch(event.getActionMasked()){
        case MotionEvent.ACTION_DOWN:downX=event.getRawX();downY=event.getRawY();originX=params.x;originY=params.y;originCompactX=compactX;originCompactY=compactY;moved=false;longClicked=false;main.postDelayed(longPress,ViewConfiguration.getLongPressTimeout());return true;
        case MotionEvent.ACTION_MOVE:
          int dx=Math.round(event.getRawX()-downX),dy=Math.round(event.getRawY()-downY);moved|=Math.abs(dx)+Math.abs(dy)>ViewConfiguration.get(context).getScaledTouchSlop();
          if(moved){main.removeCallbacks(longPress);BubblePlacement.Box box=BubblePlacement.fit(left,top,right,bottom,params.width,params.height,originX+dx,originY+dy);params.x=box.x;params.y=box.y;compactX=params.x+(params.width-dp(COMPACT_DP))/2;compactY=params.y+(params.height-dp(COMPACT_DP))/2;if(updateBody())syncPlus();}return true;
        case MotionEvent.ACTION_UP:main.removeCallbacks(longPress);if(moved)saveAnchor();else if(!longClicked)view.performClick();return true;
        case MotionEvent.ACTION_CANCEL:main.removeCallbacks(longPress);if(moved){compactX=originCompactX;compactY=originCompactY;placeBody();if(updateBody())syncPlus();}moved=false;return true;
        default:return true;
      }
    });
    try{windows.addView(bubble,params);attached=true;syncPlus();}catch(RuntimeException denied){android.util.Log.w("ROADGUARD","Bulle refusée par Android",denied);close();}
  }
  private void renderText(){
    if(bubble==null)return;bubble.setBackground(background());
    if(expanded){text.setText("⚠ RADAR · "+AlertPresentation.text(radarMeters)+"\n"+radarName);text.setTextSize(18);bubble.setContentDescription(radarName+" dans "+AlertPresentation.voice(radarMeters)+". Toucher pour ouvrir la carte, appui long pour signaler.");}
    else{text.setText(context.getResources().getConfiguration().fontScale>1.3f?"◎":"◎\nRADAR");text.setTextSize(20);bubble.setContentDescription("ROADGUARD. Toucher pour ouvrir la carte, appui long pour signaler.");}
  }
  private void placeBody(){if(params==null)return;BubblePlacement.Box box=body();params.x=box.x;params.y=box.y;params.width=box.width;params.height=box.height;}
  private boolean updateBody(){if(!attached||bubble==null)return false;try{windows.updateViewLayout(bubble,params);return true;}catch(RuntimeException denied){close();return false;}}
  boolean isShowing(){return attached&&bubble!=null;}
  private void removePlus(){if(plus!=null)try{windows.removeView(plus);}catch(RuntimeException ignored){}plus=null;plusParams=null;}
  private void syncPlus(){
    if(!attached||params==null)return;BubblePlacement.Box box=BubblePlacement.action(left,top,right,bottom,new BubblePlacement.Box(params.x,params.y,params.width,params.height),dp(44),dp(4));
    if(box==null){removePlus();return;}
    if(plus==null){plus=new TextView(context);plus.setText("+");plus.setTextSize(29);plus.setTextColor(Color.WHITE);plus.setGravity(Gravity.CENTER);plus.setContentDescription("Signaler à ma position sans ouvrir ROADGUARD");plus.setOnClickListener(v->chooseReport());GradientDrawable shape=new GradientDrawable();shape.setColor(0xffe71e48);shape.setCornerRadius(dp(23));plus.setBackground(shape);plus.setElevation(dp(18));plusParams=window(box.width,box.height);plusParams.x=box.x;plusParams.y=box.y;try{windows.addView(plus,plusParams);}catch(RuntimeException denied){removePlus();}}
    else{plusParams.x=box.x;plusParams.y=box.y;try{windows.updateViewLayout(plus,plusParams);}catch(RuntimeException denied){removePlus();}}
  }
  private void chooseReport(){
    if(!attached||reportDialog!=null&&reportDialog.isShowing())return;
    String[] labels={"Radar fixe","Radar mobile","Contrôle routier (police / gendarmerie)"},kinds={"fixed","mobile","control"};
    try{reportDialog=new AlertDialog.Builder(context).setTitle("Signaler à ma position").setItems(labels,(dialog,index)->quickReport.submit(kinds[index])).setNegativeButton("Annuler",null).create();
      reportDialog.setOnDismissListener(dialog->reportDialog=null);reportDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);reportDialog.show();
    }catch(RuntimeException denied){reportDialog=null;Toast.makeText(context,"Signalement indisponible dans la bulle",Toast.LENGTH_SHORT).show();}
  }
  void radar(String name,int meters){if(bubble==null)return;radarName=name==null?"Radar":name;radarMeters=meters;expanded=true;renderText();placeBody();if(updateBody())syncPlus();}
  void clearRadar(){if(bubble==null||!expanded)return;expanded=false;renderText();placeBody();if(updateBody())syncPlus();}
  void reflow(){if(bubble==null)return;bounds();loadAnchor();renderText();placeBody();if(updateBody())syncPlus();}
  void close(){
    main.removeCallbacksAndMessages(null);if(reportDialog!=null){AlertDialog dialog=reportDialog;reportDialog=null;try{dialog.dismiss();}catch(RuntimeException ignored){}}
    removePlus();if(bubble!=null)try{windows.removeView(bubble);}catch(RuntimeException ignored){}attached=false;bubble=null;text=null;params=null;expanded=false;
  }
}
