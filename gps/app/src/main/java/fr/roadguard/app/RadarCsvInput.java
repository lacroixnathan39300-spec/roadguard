package fr.roadguard.app;
/** Validate personal CSV without rebuilding the visible catalogue or loading embedded assets. */
final class RadarCsvInput {
 static int count(String csv){if(csv==null||csv.length()>1000000)return 0;int count=0;for(String line:csv.split("\\n")){String[] c=line.trim().split("[,;]",3);if(c.length<2)continue;try{double lat=Double.parseDouble(c[0].trim()),lon=Double.parseDouble(c[1].trim());String label=c.length>2?c[2].trim():"Zone de danger";if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180&&!label.isEmpty()&&label.length()<=80)count++;}catch(NumberFormatException ignored){}}return count;}
 private RadarCsvInput(){}
}
