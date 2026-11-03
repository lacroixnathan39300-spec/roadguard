package fr.roadguard.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.*;
import java.util.zip.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.util.Locale;

/** Standalone, explicitly launched screen. No source impersonation or embedded credentials. */
public final class RadarDownloadActivity extends Activity {
  private final Handler main = new Handler(Looper.getMainLooper());
  private ProgressBar bar;
  private TextView status, inventory;
  private Button start,batchButton;
  private java.util.Map<String,Integer> activeCounts=java.util.Collections.emptyMap();
  private boolean inventoryLoading=true;
  private CheckBox enabled;
  private EditText url, hash;
  private Spinner country;
  private volatile boolean running;
  private volatile boolean radarImportCancel;
  private RadarCountryBatch countryBatch;
  private final java.util.Map<String,String> batchDetails=new java.util.LinkedHashMap<>();
  private boolean batchMode;
  private volatile RadarArchiveDownloader archiveTransfer;
  private java.util.Map<String,Boolean> verifiedArchives=java.util.Collections.emptyMap();
  private int inventoryGeneration;
  private java.util.Map<String,Integer> importedCounts=java.util.Collections.emptyMap(),loadedImportedCounts=java.util.Collections.emptyMap();
  private java.util.Map<String,String> importWarnings=java.util.Collections.emptyMap();
  private boolean importsConfirmed;
  private final java.util.ArrayList<Button> countryButtons = new java.util.ArrayList<>();
  private static final int PICK_ARCHIVE=8216;

  private LinearLayout countryList;
  private EditText search;
  private final java.util.Map<String,Button> downloadButtons=new java.util.LinkedHashMap<>();
  private final String[][] countries={
    {"AL","Albanie","🇦🇱"},
    {"DE","Allemagne","🇩🇪"},
    {"AD","Andorre","🇦🇩"},
    {"AT","Autriche","🇦🇹"},
    {"BE","Belgique","🇧🇪"},
    {"BY","Biélorussie","🇧🇾"},
    {"BA","Bosnie-Herzégovine","🇧🇦"},
    {"BG","Bulgarie","🇧🇬"},
    {"CY","Chypre","🇨🇾"},
    {"HR","Croatie","🇭🇷"},
    {"DK","Danemark","🇩🇰"},
    {"ES","Espagne","🇪🇸"},
    {"EE","Estonie","🇪🇪"},
    {"FI","Finlande","🇫🇮"},
    {"FR","France","🇫🇷"},
    {"GR","Grèce","🇬🇷"},
    {"GE","Géorgie","🇬🇪"},
    {"HU","Hongrie","🇭🇺"},
    {"IE","Irlande","🇮🇪"},
    {"IS","Islande","🇮🇸"},
    {"IT","Italie","🇮🇹"},
    {"LV","Lettonie","🇱🇻"},
    {"LI","Liechtenstein","🇱🇮"},
    {"LT","Lituanie","🇱🇹"},
    {"LU","Luxembourg","🇱🇺"},
    {"MK","Macédoine du Nord","🇲🇰"},
    {"MT","Malte","🇲🇹"},
    {"MD","Moldavie","🇲🇩"},
    {"MC","Monaco","🇲🇨"},
    {"ME","Monténégro","🇲🇪"},
    {"NO","Norvège","🇳🇴"},
    {"NL","Pays-Bas","🇳🇱"},
    {"PL","Pologne","🇵🇱"},
    {"PT","Portugal","🇵🇹"},
    {"RO","Roumanie","🇷🇴"},
    {"GB","Royaume-Uni","🇬🇧"},
    {"RU","Russie","🇷🇺"},
    {"CZ","République tchèque","🇨🇿"},
    {"RS","Serbie","🇷🇸"},
    {"SK","Slovaquie","🇸🇰"},
    {"SI","Slovénie","🇸🇮"},
    {"CH","Suisse","🇨🇭"},
    {"SE","Suède","🇸🇪"},
    {"TR","Turquie","🇹🇷"},
    {"UA","Ukraine","🇺🇦"}};
  private void onUi(Runnable action){main.post(()->{if(!isDestroyed())action.run();});}
  private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
  private TextView text(String value,int size,int color){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;
  }
  private android.graphics.drawable.GradientDrawable bg(int fill,int stroke,int radius){
    android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
    d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;
  }
  private boolean downloadable(String code){
    return RadarDownloadSource.find(code)!=null;
  }
  private boolean publishedCountry(String code){
    for(String[] c:countries)if(c[0].equals(code))return true;
    return false;
  }
  private int embeddedCount(String code){return activeCounts.containsKey(code)?activeCounts.get(code):0;}
  private static String searchKey(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.FRANCE).trim();}
  private void loadActiveCounts(){
    inventoryLoading=true;final int generation=++inventoryGeneration;
    new Thread(()->{
      java.util.Map<String,Integer> counts=new java.util.HashMap<>();
      java.util.Map<String,Boolean> archives=new java.util.HashMap<>();
      java.util.Map<String,Integer> imported=new java.util.HashMap<>(),loaded=new java.util.HashMap<>();
      java.util.Map<String,String> warnings=new java.util.HashMap<>();String error="";
      for(String code:CanonicalRadarCatalog.countries(UpdatedRadarCatalog.directory(getApplicationContext()))){
        try{CanonicalRadarStore.Snapshot snapshot=CanonicalRadarStore.load(UpdatedRadarCatalog.directory(getApplicationContext()),code);imported.put(code,snapshot.records.size());loaded.put(code,RadarActivationState.loadedCount(getApplicationContext(),code));
          if(!snapshot.warning.isEmpty()||snapshot.backup)warnings.put(code,snapshot.backup?"Sauvegarde utilisée":snapshot.warning);
        }catch(IOException invalid){warnings.put(code,"Surcouche locale indisponible");}
      }
      for(String[] country:countries){RadarDownloadSource source=RadarDownloadSource.find(country[0]);
        File archive=downloadedArchive(country[0]);
        if(source!=null&&archive.isFile())try{RadarArchivePolicy.verify(archive);archives.put(country[0],source.verify(archive));}
        catch(IOException invalid){archives.put(country[0],false);}
      }
      try{android.content.SharedPreferences prefs=getSharedPreferences("MainActivity",MODE_PRIVATE);
        for(BundledRadarCatalog.Point point:UpdatedRadarCatalog.active(getApplicationContext(),prefs.getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false),prefs.getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true)))counts.put(point.country,counts.getOrDefault(point.country,0)+1);
      }catch(Exception invalid){error=String.valueOf(invalid.getMessage());}
      final boolean confirmed=RadarActivationState.confirmed(getApplicationContext());
      final String problem=error;
      onUi(()->{if(isDestroyed()||generation!=inventoryGeneration)return;activeCounts=java.util.Collections.unmodifiableMap(counts);verifiedArchives=java.util.Collections.unmodifiableMap(archives);importedCounts=java.util.Collections.unmodifiableMap(imported);loadedImportedCounts=java.util.Collections.unmodifiableMap(loaded);importWarnings=java.util.Collections.unmodifiableMap(warnings);importsConfirmed=confirmed;inventoryLoading=false;renderCountries(search.getText().toString());if(!problem.isEmpty())status.setText("Catalogue local indisponible : "+problem);});
    },"radar-active-inventory").start();
  }
  private File downloadedArchive(String code){
    return new File(new File(getFilesDir(),"radar_downloads"),code.toLowerCase(Locale.ROOT)+"-camera.sqlite.7z");
  }
  private void renderCountries(String filter){
    if(countryList==null)return;
    countryList.removeAllViews();countryButtons.clear();downloadButtons.clear();
    String q=searchKey(filter==null?"":filter);
    for(String[] c:countries){
      final String code=c[0],label=c[1];
      if(!searchKey(label).contains(q)&&!searchKey(code).contains(q))continue;
      File localArchive=downloadedArchive(code);
      boolean saved=Boolean.TRUE.equals(verifiedArchives.get(code));
      boolean damaged=Boolean.FALSE.equals(verifiedArchives.get(code));
      int included=embeddedCount(code);
      int imported=importedCounts.getOrDefault(code,0),loaded=loadedImportedCounts.getOrDefault(code,0);
      boolean supported=downloadable(code);
      LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
      line.setGravity(android.view.Gravity.CENTER_VERTICAL);
      line.setPadding(dp(10),dp(9),dp(8),dp(9));
      line.setBackground(bg(0xff101a24,0xff273544,12));
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(6);
      countryList.addView(line,lp);
      TextView flag=text(c[2],27,Color.WHITE);line.addView(flag,new LinearLayout.LayoutParams(dp(46),-2));
      LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
      line.addView(details,new LinearLayout.LayoutParams(0,-2,1));
      details.addView(text(label,16,Color.WHITE));
      String description=imported>0?(loaded>0?"Actif dans le moteur • "+loaded+" points importés":importsConfirmed?"Importé • "+imported+" points • aucun point ajouté au moteur":"Importé • "+imported+" points • chargement moteur à confirmer"):
        damaged?"Archive locale invalide • base intégrée conservée":
        saved?(included>0?"Base locale : "+included+" points • ":"")+"Archive vérifiée • import disponible":
        inventoryLoading?"Vérification du catalogue local…":localArchive.isFile()?"Archive ancienne • vérification requise":included>0?"Catalogue local vérifié • "+included+" points":"Aucun radar local pour ce pays";
      if(importWarnings.containsKey(code))description+=" • "+importWarnings.get(code);
      if(!supported&&!inventoryLoading&&included>0)description+=" • mise à jour indisponible";
      TextView state=text(description,11,loaded>0&&!importWarnings.containsKey(code)?0xff48df81:imported>0||localArchive.isFile()?0xffffb347:included>0?0xff48df81:0xffa8b5c5);
      details.addView(state);
      Button b=new Button(this);
      b.setText(supported?(imported>0?"Base importée":saved?"Importer":("FR".equals(code)||"CH".equals(code))?"Installer":"↓ Installer"):inventoryLoading?"Vérification…":included>0?"Base incluse":"Indisponible");
      b.setAllCaps(false);b.setTextSize(12);b.setTextColor(Color.WHITE);
      b.setBackground(bg(supported?0xffad0b29:0xff36414b,supported?0xfffb365a:0xff56616c,10));
      LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(124),dp(47));
      line.addView(b,bp);b.setEnabled(supported&&!running&&imported==0);
      if(supported)b.setOnClickListener(v->downloadCountryArchive(code));
      countryButtons.add(b);downloadButtons.put(code,b);
    }
  }
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    if(!getSharedPreferences("roadguard_community",MODE_PRIVATE).getBoolean("approved",false)){finish();return;}
    final java.text.Collator collator=java.text.Collator.getInstance(Locale.FRANCE);java.util.Arrays.sort(countries,(a,b)->collator.compare(a[1],b[1]));
    getWindow().setStatusBarColor(0xff060b11);getWindow().setNavigationBarColor(0xff060b11);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(12),dp(12),dp(12),dp(12));root.setBackgroundColor(0xff070d14);
    LinearLayout head=new LinearLayout(this);head.setGravity(android.view.Gravity.CENTER_VERTICAL);
    root.addView(head);
    TextView title=text("◉  Installer les radars",23,Color.WHITE);
    head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    Button close=new Button(this);close.setText("✕");close.setTextColor(Color.WHITE);
    close.setBackground(bg(0xff161e27,0xfff72b51,12));head.addView(close,new LinearLayout.LayoutParams(dp(55),dp(48)));
    close.setOnClickListener(v->finish());
    root.addView(text("Radars d’origine inclus. Installation vérifiée des mises à jour ; archives France/Suisse intégrées.",13,0xffacb9c8));
    search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);
    search.setHintTextColor(0xff91a0b1);search.setHint("⌕  Rechercher un pays…");
    search.setBackground(bg(0xff101a24,0xff526779,12));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.topMargin=dp(12);sp.bottomMargin=dp(10);
    root.addView(search,sp);
    countryList=new LinearLayout(this);countryList.setOrientation(LinearLayout.VERTICAL);
    ScrollView scroller=new ScrollView(this);scroller.setFillViewport(false);scroller.addView(countryList);
    root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
    bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);
    root.addView(bar,new LinearLayout.LayoutParams(-1,dp(9)));
    status=text("Les archives téléchargées sont conservées séparément des bases intégrées.",12,0xffa8b5c5);
    root.addView(status);
    LinearLayout actions=new LinearLayout(this);actions.setGravity(android.view.Gravity.CENTER);
    root.addView(actions);
    Button all=new Button(this);batchButton=all;all.setText("Installer les mises à jour");all.setAllCaps(false);
    all.setTextColor(Color.WHITE);all.setBackground(bg(0xff18232e,0xfff72b51,12));
    actions.addView(all,new LinearLayout.LayoutParams(0,dp(53),1));
    all.setOnClickListener(v->{
      if(running){
        RadarArchiveDownloader transfer=archiveTransfer;
        if(transfer!=null){radarImportCancel=true;if(countryBatch!=null)countryBatch.cancel();transfer.cancel();status.setText("Arrêt demandé ; archives précédentes conservées.");}
        else status.setText("Un import local est en cours. Attendez sa fin.");
      }else if(!batchMode)startCountryBatch();
    });
    Button sources=new Button(this);sources.setText("Sources");sources.setAllCaps(false);
    sources.setTextColor(Color.WHITE);sources.setBackground(bg(0xff18232e,0xfff72b51,12));
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(53),1);ap.leftMargin=dp(6);actions.addView(sources,ap);
    sources.setOnClickListener(v->showAdvanced());
    Button inventoryButton=new Button(this);inventoryButton.setText("État des bases");inventoryButton.setAllCaps(false);
    inventoryButton.setTextColor(Color.WHITE);inventoryButton.setBackground(bg(0xff18232e,0xfff72b51,12));
    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,dp(53),1);ip.leftMargin=dp(6);actions.addView(inventoryButton,ip);
    inventoryButton.setOnClickListener(v->showAdvanced());
    // Existing importer and source management remain accessible from the advanced dialog.
    country=new Spinner(this);country.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
      new String[]{"FR","CH","DE","BE","ES","IT","LU","AT","NL","PT","GB","IE","PL","CZ","SK","HU","SI","HR","GR","BG","RO","MD","LT","LV","EE","SE","FI","NO","CY","AD","DK"}));
    inventory=text("",13,Color.WHITE);
    enabled=new CheckBox(this);enabled.setText("Inclure les surcouches Lufop et locales de ce pays");
    url=new EditText(this);url.setSingleLine(true);url.setHint("URL HTTPS du CSV");
    hash=new EditText(this);hash.setSingleLine(true);hash.setHint("SHA-256");
    start=new Button(this);start.setText("Télécharger le CSV vérifié");start.setOnClickListener(v->download());
    country.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){showInventory();}
      @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
    });
    search.addTextChangedListener(new android.text.TextWatcher(){
      public void beforeTextChanged(CharSequence s,int st,int count,int after){}
      public void onTextChanged(CharSequence s,int st,int before,int count){renderCountries(s.toString());}
      public void afterTextChanged(android.text.Editable e){}
    });
    // targetSdk 36: apply actual system bar insets, including gesture/navigation areas.
    // Insets are added to the existing 12dp content padding, not substituted for it.
    if(android.os.Build.VERSION.SDK_INT>=30){
      getWindow().setDecorFitsSystemWindows(false);
      android.view.WindowInsetsController controller=getWindow().getDecorView().getWindowInsetsController();
      if(controller!=null)controller.setSystemBarsAppearance(0,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }else{
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    root.setOnApplyWindowInsetsListener((view,insets)->{
      if(android.os.Build.VERSION.SDK_INT>=30){
        android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout()|android.view.WindowInsets.Type.ime());
        view.setPadding(dp(12)+bars.left,dp(12)+bars.top,dp(12)+bars.right,dp(12)+bars.bottom);
      }else{
        view.setPadding(dp(12)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),dp(12)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());
      }
      return insets;
    });
    setContentView(root);root.requestApplyInsets();renderCountries("");showInventory();loadActiveCounts();
  }
  private void notifyRadarCatalogChanged(){
    RadarCatalogRevision.changed(getApplicationContext());
    BackgroundAlertService.requestRadarReload();
  }
  private void showAdvanced(){
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(14),dp(8),dp(14),dp(8));
    panel.addView(text("Inventaire et importation avancée",18,0xff15202a));
    for(View view:new View[]{country,inventory,enabled,url,hash,start}){
      if(view.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)view.getParent()).removeView(view);
    }
    panel.addView(country);panel.addView(inventory);panel.addView(enabled);
    panel.addView(url);panel.addView(hash);panel.addView(start);
    Button importZip=new Button(this);importZip.setText("Importer une base locale (ZIP, CSV, SQLite)");
    panel.addView(importZip);
    importZip.setOnClickListener(v->{if(running)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_ARCHIVE);});
    new android.app.AlertDialog.Builder(this).setTitle("Sources et état des bases")
      .setMessage("SpeedAlert_Databases : hvdwolf et contributeurs, CC BY 4.0. https://github.com/hvdwolf/SpeedAlert_Databases\nArchives originales conservées ; conversion ROADGUARD en surcouche CSV.")
      .setView(panel).setPositiveButton("Fermer",(d,w)->{}).show();
  }
  private void showInventory(){
    if(inventory==null||country==null||country.getSelectedItem()==null)return;
    final String code=country.getSelectedItem().toString();
    enabled.setOnCheckedChangeListener(null);
    enabled.setChecked(getSharedPreferences("radar_country_visibility",MODE_PRIVATE).getBoolean(code,true));
    enabled.setOnCheckedChangeListener((button,checked)->{
      getSharedPreferences("radar_country_visibility",MODE_PRIVATE).edit().putBoolean(code,checked).apply();
      notifyRadarCatalogChanged();
      status.setText("Réglage enregistré pour "+code+". Rechargement des alertes demandé.");loadActiveCounts();
    });
    inventory.setText("Recherche des radars intégrés pour "+code+"…");
    new Thread(()->{
      try{
        int count=LufopEuropeCatalog.count(getApplicationContext(),code);
        CanonicalRadarStore.Snapshot local=CanonicalRadarStore.load(UpdatedRadarCatalog.directory(getApplicationContext()),code);
        int loaded=RadarActivationState.loadedCount(getApplicationContext(),code);
        String details="Lufop intégré ("+code+") : "+count+" points";
        if(!local.records.isEmpty())details+="\nImport SQLite local : "+local.records.size()+" points • "+loaded+" chargés par le moteur\nProvenance de l'import local non vérifiée";
        if(local.backup)details+="\nSauvegarde de la surcouche utilisée";
        final String description=details;
        onUi(()->{if(country.getSelectedItem()!=null&&code.equals(country.getSelectedItem().toString()))inventory.setText(description);});
      }catch(Exception e){onUi(()->inventory.setText("Inventaire indisponible : "+e.getMessage()));}
    },"radar-inventory-worker").start();
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request!=PICK_ARCHIVE||result!=RESULT_OK||data==null||data.getData()==null||running)return;
    final Uri uri=data.getData();final String selectedCountry=country.getSelectedItem().toString();running=true;start.setEnabled(false);bar.setIndeterminate(true);
    status.setText("Analyse de l'archive locale…");
    new Thread(()->{
      File dir=UpdatedRadarCatalog.directory(getApplicationContext());
      File tmp=new File(dir,"lufop_europe_asc_overlay.pending");
      File dest=new File(dir,"lufop_europe_asc_overlay.csv");
      File backup=new File(dir,"lufop_europe_asc_overlay.backup");
      boolean backedUp=false;
      File olderBackup=new File(dir,"lufop_europe_asc_overlay.backup.previous");
      try{
        if(!dir.exists() && !dir.mkdirs())throw new IOException("Dossier de mises à jour inaccessible");
        // Accept original Lufop ASC ZIPs as well as ROADGUARD overlay CSV/ZIP.
        try(InputStream probe=getContentResolver().openInputStream(uri)){
          if(probe==null)throw new IOException("Fichier inaccessible");
          boolean sqlite=SpeedAlertSqliteImporter.sqliteHeader(probe);
          if(sqlite){importLocalSqlite(uri,selectedCountry);return;}
        }
        try(InputStream probe=getContentResolver().openInputStream(uri)){
          if(probe==null)throw new IOException("Fichier inaccessible");
          if(LufopAscZipImporter.isAscArchive(probe)){
            try(InputStream archive=getContentResolver().openInputStream(uri)){
              if(archive==null)throw new IOException("Archive ASC inaccessible");
              int points=LufopAscZipImporter.convert(archive,tmp,count->onUi(()->status.setText("Conversion ASC : "+count+" points")));
              onUi(()->status.setText("Archive Lufop convertie : "+points+" points. Installation…"));
            }
          } else {
          try(InputStream input=getContentResolver().openInputStream(uri)){
          if(input==null)throw new IOException("Fichier inaccessible");
          java.io.BufferedInputStream buffered=new java.io.BufferedInputStream(input);buffered.mark(4);
          int a=buffered.read(),b=buffered.read();buffered.reset();
          InputStream csv=buffered;
          if(a=='P'&&b=='K'){
            ZipInputStream zip=new ZipInputStream(buffered);ZipEntry entry;boolean found=false;int entries=0;
            while((entry=zip.getNextEntry())!=null){
              if(++entries>2000)throw new IOException("Archive trop complexe");
              String name=entry.getName();
              if(!LufopAscZipImporter.safeEntryName(name))throw new IOException("Archive ZIP : chemin non autorisé");
              if(!entry.isDirectory()&&(name.equals("lufop_europe_asc_overlay.csv")||name.endsWith("/lufop_europe_asc_overlay.csv"))){csv=new BoundedInputStream(zip,35000000L);found=true;break;}
            }
            if(!found)throw new IOException("ZIP non reconnu : attendu fichiers .asc Lufop ou lufop_europe_asc_overlay.csv");
          }
          try(BufferedReader reader=new BufferedReader(new InputStreamReader(csv,"UTF-8"));BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp),"UTF-8"))){
            String header=reader.readLine();if(!"country;type;lat;lon;speed_kmh;id;label".equals(header))throw new IOException("Format CSV non compatible");
            writer.write(header);writer.newLine();String line;int rows=0;long bytes=header.length();
            while((line=reader.readLine())!=null){bytes+=line.length();if(bytes>30000000L)throw new IOException("Fichier trop volumineux");
              String[] c=line.split(";",-1);if(c.length!=7||!c[0].matches("[A-Z]{2}"))throw new IOException("Ligne invalide : "+(rows+2));
              double lat,lon;int speed;
              try{lat=Double.parseDouble(c[2]);lon=Double.parseDouble(c[3]);speed=Integer.parseInt(c[4]);}
              catch(NumberFormatException invalid){throw new IOException("Valeur numérique invalide à la ligne "+(rows+2),invalid);}
              if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||speed<0||speed>300)throw new IOException("Coordonnées ou vitesse invalides");
              writer.write(line);writer.newLine();if(++rows>200000)throw new IOException("Trop de points");
              if(rows%10000==0){final int count=rows;onUi(()->status.setText("Vérification : "+count+" points"));}
            }
            if(rows==0)throw new IOException("Base vide");
          }
        }
          } // end legacy CSV/ZIP branch
        } // end archive type selection
        // Validate the completed temporary file before replacing the active overlay.
        int validated=validateOverlay(tmp);
        // Two-stage backup rotation: preserve the previous known-good backup until
        // the new catalog is safely installed. Never alter APK-bundled databases.
        if(olderBackup.exists() && !olderBackup.delete())throw new IOException("Ancienne sauvegarde secondaire inaccessible");
        boolean rotated=false;
        if(backup.exists()){
          if(!backup.renameTo(olderBackup))throw new IOException("Rotation de sauvegarde impossible");
          rotated=true;
        }
        if(dest.exists()){
          if(!dest.renameTo(backup)){
            if(rotated)olderBackup.renameTo(backup);
            throw new IOException("Sauvegarde de la base actuelle impossible");
          }
          backedUp=true;
        }
        if(!tmp.renameTo(dest)){
          boolean restored=!backedUp || backup.renameTo(dest);
          if(rotated && !backup.exists())olderBackup.renameTo(backup);
          if(!restored)throw new IOException("Installation échouée ; restauration automatique impossible. Sauvegarde conservée");
          throw new IOException("Installation impossible, ancienne base restaurée");
        }
        // Keep both generations of backup for recovery if the installed file later corrupts.
        final int installed=validated;
        LufopEuropeCatalog.invalidate();
        notifyRadarCatalogChanged();
        onUi(()->{bar.setIndeterminate(false);bar.setProgress(100);status.setTextColor(Color.GREEN);status.setText("Base locale installée : "+installed+" points. Le service radar actif reçoit une demande de rechargement.");showInventory();running=false;start.setEnabled(true);loadActiveCounts();});
      }catch(Exception e){new File(UpdatedRadarCatalog.directory(getApplicationContext()),"lufop_europe_asc_overlay.pending").delete();onUi(()->{bar.setIndeterminate(false);bar.setProgress(0);status.setTextColor(Color.RED);status.setText("INTERVENTION OBLIGATOIRE : "+e.getMessage()+". Bases originales intactes.");running=false;start.setEnabled(true);loadActiveCounts();});}
    },"radar-zip-import").start();
  }
  private void importLocalSqlite(Uri uri,String code)throws IOException{
    File temporary=File.createTempFile("sqlite-import-",".sqlite",getCacheDir());
    try{
      try(InputStream input=getContentResolver().openInputStream(uri);FileOutputStream output=new FileOutputStream(temporary)){
        if(input==null)throw new IOException("Base SQLite inaccessible");byte[] buffer=new byte[8192];int n;long total=0;
        while((n=input.read(buffer))!=-1){if(isDestroyed()||Thread.currentThread().isInterrupted())throw new IOException("Import SQLite annulé");if((total+=n)>CanonicalRadarStore.MAX_BYTES)throw new IOException("Base SQLite trop volumineuse");output.write(buffer,0,n);}
        output.getFD().sync();
      }
      CanonicalRadarStore.Cancellation cancellation=()->{if(isDestroyed()||Thread.currentThread().isInterrupted())throw new IOException("Import SQLite annulé");};
      java.util.List<CanonicalRadar> records=SpeedAlertSqliteImporter.readLocal(temporary,code,cancellation);
      CanonicalRadarStore.install(UpdatedRadarCatalog.directory(getApplicationContext()),code,records,cancellation);
      onUi(()->{notifyRadarCatalogChanged();running=false;start.setEnabled(true);bar.setIndeterminate(false);bar.setProgress(100);status.setTextColor(0xffffb347);status.setText(code+" : "+records.size()+" points SQLite importés. Revenez à la carte pour confirmer leur chargement par le moteur.");loadActiveCounts();});
    }finally{temporary.delete();}
  }
  /** Restrict decompression of a selected ZIP entry before parsing it. */
  private static final class BoundedInputStream extends FilterInputStream {
    private long remaining;
    BoundedInputStream(InputStream in,long maxBytes){super(in);remaining=maxBytes;}
    @Override public int read() throws IOException {
      int b=super.read();if(b>=0 && --remaining<0)throw new IOException("Archive ZIP décompressée trop volumineuse");return b;
    }
    @Override public int read(byte[] b,int off,int len) throws IOException {
      if(remaining<=0){int check=super.read();if(check<0)return -1;throw new IOException("Archive ZIP décompressée trop volumineuse");}
      int n=super.read(b,off,(int)Math.min((long)len,remaining));if(n>0)remaining-=n;return n;
    }
  }
  /** Strictly validate the staged CSV; never touch the bundled APK databases. */
  private static int validateOverlay(File staged) throws IOException {
    int rows=0;long bytes=0;
    try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(staged),"UTF-8"))){
      if(!"country;type;lat;lon;speed_kmh;id;label".equals(r.readLine()))throw new IOException("En-tête invalide");
      String line;
      while((line=r.readLine())!=null){
        bytes+=line.length();
        if(bytes>35000000L||++rows>200000)throw new IOException("Base trop volumineuse");
        String[] c=line.split(";",-1);
        if(c.length!=7||!c[0].matches("[A-Z]{2}"))throw new IOException("Ligne invalide : "+(rows+1));
        try{
          double lat=Double.parseDouble(c[2]),lon=Double.parseDouble(c[3]);
          int speed=Integer.parseInt(c[4]);
          if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||speed<0||speed>300)
            throw new IOException("Coordonnées ou vitesse invalides à la ligne "+(rows+1));
        }catch(NumberFormatException ex){throw new IOException("Nombre invalide à la ligne "+(rows+1),ex);}
      }
    }
    if(rows==0)throw new IOException("Base radar vide");
    return rows;
  }
  /** Sequential updates: one active transfer; every country retains its own outcome. */
  private void startCountryBatch(){
    if(running||batchMode)return;
    countryBatch=new RadarCountryBatch(new String[]{"FR","CH","DE","BE","ES","IT","LU"});
    batchDetails.clear();
    batchMode=true;batchButton.setText("Annuler");status.setTextColor(Color.WHITE);nextCountryBatch();
  }
  private void nextCountryBatch(){
    if(!batchMode||running)return;
    String code=countryBatch.next();
    if(code==null){
      batchMode=false;batchButton.setText("Installer les mises à jour");
      int received=countryBatch.count(RadarCountryBatch.Outcome.IMPORTED);
      int failed=countryBatch.count(RadarCountryBatch.Outcome.FAILED);
      int cancelled=countryBatch.count(RadarCountryBatch.Outcome.CANCELLED);
      status.setTextColor(0xffffb347);
      StringBuilder summary=new StringBuilder(countryBatch.cancelled()?"Mise à jour interrompue : ":"Imports terminés : ");
      summary.append(received).append('/').append(countryBatch.total()).append(" bases importées, ").append(failed).append(" échec(s), ").append(cancelled).append(" annulé(s).");
      for(java.util.Map.Entry<String,RadarCountryBatch.Outcome> entry:countryBatch.snapshot().entrySet()){
        summary.append("\n").append(entry.getKey()).append(" : ");
        switch(entry.getValue()){
          case IMPORTED:summary.append("base importée");break;
          case RECEIVED:summary.append("archive reçue");break;
          case FAILED:summary.append(batchDetails.containsKey(entry.getKey())?batchDetails.get(entry.getKey()):"échec");break;
          case CANCELLED:summary.append("annulé");break;
          default:summary.append("en attente");
        }
      }
      summary.append("\nRevenez à la carte pour confirmer le chargement. Bases d’origine conservées.");
      status.setText(summary.toString());loadActiveCounts();return;
    }
    downloadCountryArchive(code);
  }
  private void downloadCountryArchive(String code){
    if(running)return;
    final RadarDownloadSource source=RadarDownloadSource.find(code);
    if(source==null){status.setText("Mise à jour indisponible pour "+code+". Catalogue local conservé.");return;}
    final RadarArchiveDownloader transfer=new RadarArchiveDownloader();archiveTransfer=transfer;radarImportCancel=false;
    final RadarCountryBatch batch=batchMode?countryBatch:null;
    running=true;start.setEnabled(false);for(Button b:countryButtons)b.setEnabled(false);
    batchButton.setText("Annuler");status.setTextColor(Color.WHITE);bar.setIndeterminate(true);status.setText("Connexion : "+code+"…");
    new Thread(()->{
      RadarCountryBatch.Outcome result;String message;
      try{
        CanonicalRadarStore.Cancellation cancellation=()->{if(radarImportCancel||Thread.currentThread().isInterrupted()||(batch!=null&&batch.cancelled()))throw new RadarArchiveDownloader.Cancelled();};
        File archive=downloadedArchive(code);
        if(!archive.isFile()||!source.verify(archive)){
          archive=RadarArchiveInstaller.bundled(source,getAssets()::open,new File(getFilesDir(),"radar_downloads"),cancellation);
          if(archive==null)archive=transfer.download(source,new File(getFilesDir(),"radar_downloads"),(done,total)->onUi(()->{
            if(archiveTransfer!=transfer)return;bar.setIndeterminate(false);bar.setProgress((int)Math.min(80,done*80/total));status.setText(code+" : "+done+" / "+total+" octets reçus");
          }));
        }
        onUi(()->{if(archiveTransfer==transfer){bar.setIndeterminate(true);status.setText(code+" : extraction et import en arrière-plan…");}});
        int count=RadarArchiveInstaller.install(archive,source,UpdatedRadarCatalog.directory(getApplicationContext()),getCacheDir(),cancellation,getAssets()::open);
        // An import commits independently of the Activity callback. It never writes an active receipt.
        RadarCatalogRevision.changed(getApplicationContext());BackgroundAlertService.requestRadarReload();
        result=RadarCountryBatch.Outcome.IMPORTED;
        message=code+" : "+count+" points importés. Revenez à la carte pour confirmer leur chargement. Les doublons peuvent réduire le nombre d’ajouts.";

      }catch(RadarArchiveDownloader.Cancelled cancelled){
        result=RadarCountryBatch.Outcome.CANCELLED;message=code+" : opération annulée. Bases radar précédentes conservées.";
      }catch(Exception error){
        result=RadarCountryBatch.Outcome.FAILED;message=code+" : "+error.getMessage()+". Archive précédente et bases intégrées conservées.";
      }
      final RadarCountryBatch.Outcome outcome=result;final String detail=message;
      onUi(()->{
        if(batch!=null){batch.finish(code,outcome);batchDetails.put(code,detail);}
        running=false;archiveTransfer=null;start.setEnabled(true);bar.setIndeterminate(false);
        bar.setProgress(outcome==RadarCountryBatch.Outcome.IMPORTED?100:0);
        status.setTextColor(outcome==RadarCountryBatch.Outcome.FAILED?Color.RED:0xffffb347);status.setText(detail);
        if(outcome==RadarCountryBatch.Outcome.IMPORTED){
          java.util.Map<String,Boolean> next=new java.util.HashMap<>(verifiedArchives);next.put(code,true);verifiedArchives=java.util.Collections.unmodifiableMap(next);
        }
        renderCountries(search.getText().toString());
        if(batchMode)nextCountryBatch();else{batchButton.setText("Installer les mises à jour");loadActiveCounts();}
      });
    },"radar-country-"+code.toLowerCase(Locale.ROOT)).start();
  }
  @Override protected void onDestroy(){
    radarImportCancel=true;if(countryBatch!=null)countryBatch.cancel();
    RadarArchiveDownloader transfer=archiveTransfer;if(transfer!=null)transfer.cancel();
    inventoryGeneration++;main.removeCallbacksAndMessages(null);super.onDestroy();
  }
  private void download() {
    if(running)return;
    final String address=url.getText().toString().trim();
    final String checksum=hash.getText().toString().trim();
    final String cc=country.getSelectedItem().toString();
    if(!("FR".equals(cc)||"CH".equals(cc))) {
      status.setText("Les bases Europe sont déjà embarquées. Les mises à jour CSV vérifiées sont actuellement limitées à FR et CH.");status.setTextColor(Color.RED);return;
    }
    if(!address.startsWith("https://")||!checksum.matches("[a-fA-F0-9]{64}")) {
      status.setText("INTERVENTION OBLIGATOIRE : renseigner une URL HTTPS directe et le SHA-256.");status.setTextColor(Color.RED);return;
    }
    running=true;start.setEnabled(false);status.setTextColor(Color.WHITE);bar.setIndeterminate(true);
    new Thread(()->{
      try {
        File dest=UpdatedRadarCatalog.directory(getApplicationContext());
        AuthorizedRadarImporter.Source source=new AuthorizedRadarImporter.Source(cc,address,checksum,100_000_000L);
        File saved=AuthorizedRadarImporter.downloadAndVerify(source,dest,(done,total)->onUi(()->{
          if(total>0){bar.setIndeterminate(false);bar.setProgress((int)Math.min(100,done*100/total));status.setText(String.format(Locale.ROOT,"Téléchargement : %d / %d octets",done,total));}
          else{bar.setIndeterminate(true);status.setText("Téléchargement : "+done+" octets");}
        }));
        onUi(()->{bar.setIndeterminate(false);bar.setProgress(100);notifyRadarCatalogChanged();status.setText("Base vérifiée et installée : "+saved.getName()+". Rechargement des alertes demandé.");running=false;start.setEnabled(true);loadActiveCounts();});
      } catch(Exception error) {
        onUi(()->{bar.setIndeterminate(false);bar.setProgress(0);status.setTextColor(Color.RED);status.setText("INTERVENTION OBLIGATOIRE : "+error.getMessage()+". Les bases intégrées restent disponibles.");running=false;start.setEnabled(true);loadActiveCounts();});
      }
    },"radar-download-worker").start();
  }
}
