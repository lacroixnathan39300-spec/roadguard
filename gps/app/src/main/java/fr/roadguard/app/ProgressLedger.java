package fr.roadguard.app;

import org.json.*;
import java.io.IOException;
import java.util.*;

/** Computes progress from explicit weighted tasks and executed checks, never an estimate. */
final class ProgressLedger {
  static final class Module {
    final String name;final double weight,development,validation,previousDevelopment,previousValidation;
    Module(String name,double weight,double dev,double checked,double previousDev,double previousChecked){
      this.name=name;this.weight=weight;development=100*dev/weight;validation=100*checked/weight;
      previousDevelopment=100*previousDev/weight;previousValidation=100*previousChecked/weight;
    }
  }
  final List<Module> modules=new ArrayList<>();
  double development,validation,previousDevelopment,previousValidation;
  final String version,active,baseline;
  ProgressLedger(JSONObject ledger)throws IOException{
    try{
      version=ledger.getString("version");baseline=ledger.optString("baseline_version","référence historique");active=ledger.getString("active_work");
      JSONArray groups=ledger.getJSONArray("modules");double total=0;
      if(groups.length()!=7)throw new IOException("Le suivi doit contenir sept modules");
      for(int i=0;i<groups.length();i++){
        JSONObject group=groups.getJSONObject(i);double weight=group.getDouble("weight"),tasksWeight=0,dev=0,checked=0,previousDev=0,previousChecked=0;
        JSONArray tasks=group.getJSONArray("tasks");
        for(int j=0;j<tasks.length();j++){
          JSONObject task=tasks.getJSONObject(j);double points=task.getDouble("weight");
          if(!Double.isFinite(points)||points<=0)throw new IOException("Poids de tâche invalide");
          tasksWeight+=points;boolean coded=task.getBoolean("coded"),wasCoded=task.getBoolean("baseline_coded");
          if(coded)dev+=points;if(wasCoded)previousDev+=points;
          double planned=0,passed=0,previousPassed=0;JSONArray checks=task.getJSONArray("checks");
          for(int k=0;k<checks.length();k++){
            JSONObject check=checks.getJSONObject(k);double checkWeight=check.getDouble("weight");
            if(!Double.isFinite(checkWeight)||checkWeight<=0)throw new IOException("Poids de contrôle invalide");
            planned+=checkWeight;
            if(check.getBoolean("passed")){if(!coded||check.optString("evidence").isEmpty())throw new IOException("Validation sans code ni preuve");passed+=checkWeight;}
            if(check.optBoolean("baseline_passed",false)){if(!wasCoded)throw new IOException("Référence incohérente");previousPassed+=checkWeight;}
          }
          if(Math.abs(planned-points)>.000001)throw new IOException("Contrôles non pondérés à 100 %");
          checked+=passed;previousChecked+=previousPassed;
        }
        if(!Double.isFinite(weight)||weight<=0||Math.abs(tasksWeight-weight)>.000001)throw new IOException("Somme des tâches incorrecte");
        modules.add(new Module(group.getString("name"),weight,dev,checked,previousDev,previousChecked));
        total+=weight;development+=dev;validation+=checked;previousDevelopment+=previousDev;previousValidation+=previousChecked;
      }
      if(Math.abs(total-100)>.000001)throw new IOException("Le total doit être de 100 points");
    }catch(JSONException malformed){throw new IOException("Suivi pondéré invalide",malformed);}
  }
  static String percent(double value){return String.format(Locale.FRANCE,"%.4f %%",value);}
  static String delta(double value){return String.format(Locale.FRANCE,"%+.4f pts",value);}
}
