package fr.roadguard.app;
import android.widget.*;
/** Native access to the existing local/import/country engines; no embedded database is replaced. */
final class RadarToolsUi {
 static boolean available(MainActivity a){return true;}
 static void show(MainActivity a){
  if(!a.accountConnected()){a.selected="Compte";a.legacySection=false;a.render();return;}
  ScrollView scroll=new ScrollView(a);LinearLayout box=a.column();box.setPadding(a.pxDp(16),a.pxDp(12),a.pxDp(16),a.pxDp(28));box.setBackgroundColor(0xff090f19);scroll.addView(box);a.frame.addView(scroll,new android.widget.FrameLayout.LayoutParams(-1,-1));
  TextView heading=a.label("AJOUTER DES RADARS",22);heading.setTypeface(null,android.graphics.Typeface.BOLD);a.add(box,heading);
  a.add(box,a.label("Les sept bases d’origine restent conservées. Les points personnels et les imports sont stockés séparément sur cet appareil.",13));
  action(a,box,"＋","Enregistrer un radar ici","Position GPS actuelle · fixe, feu rouge ou contrôle mobile temporaire",()->{a.refreshLastPosition();a.addCustomRadar();});
  action(a,box,"⇧","Importer un fichier CSV","Latitude ; longitude ; type · jusqu’à 1 Mo / 20 000 lignes",()->new android.app.AlertDialog.Builder(a).setTitle("Importer des radars personnels").setMessage("Format : latitude;longitude;type, avec un point décimal. Exemple : 46.75000;6.12000;Radar fixe\n\nCe fichier remplace uniquement l’import CSV personnel précédent. Les points ajoutés manuellement, les pays téléchargés et les sept bases embarquées sont conservés.").setNegativeButton("Annuler",null).setPositiveButton("Choisir un fichier",(d,w)->{if(available(a))a.openCsv();}).show());
  action(a,box,"⇧","Importer un ZIP Lufop · Suisse","Archive CSV/ASC suisse · moteur d’import existant",()->a.chooseLufopSwissCsvZip());
  action(a,box,"⇩","Télécharger des radars par pays","Choisir un pays et sa source",()->a.openSimpleRadarCountryPicker());
  action(a,box,"◎","Mes radars enregistrés","Consulter ou supprimer uniquement mes points personnels",()->a.manageCustomRadars());
  action(a,box,"⚠","Bases et réglages radar","Consulter les bases chargées, les sources et les filtres",()->{a.selected="Alertes";a.legacySection=true;a.legacySectionScreen=a.selected;a.render();});
  a.add(box,a.button("Retour à la carte",()->{a.selected="Carte";a.legacySection=false;a.render();}));
 }
 private static void action(MainActivity a,LinearLayout box,String icon,String title,String detail,Runnable run){a.premiumAction(box,icon,title,detail,()->{if(available(a))run.run();});}
 private RadarToolsUi(){}
}
