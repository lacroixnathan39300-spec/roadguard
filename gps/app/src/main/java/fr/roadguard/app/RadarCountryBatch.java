package fr.roadguard.app;

import java.util.*;

/** UI-independent sequential queue, with a retained outcome for every offered country. */
final class RadarCountryBatch {
  enum Outcome { WAITING, RUNNING, RECEIVED, IMPORTED, FAILED, CANCELLED }
  private final LinkedHashMap<String,Outcome> states=new LinkedHashMap<>();
  private String current;
  private boolean cancelled;
  RadarCountryBatch(String[] countries){
    for(String code:countries){
      if(code==null||!code.matches("[A-Z]{2}")||states.containsKey(code))throw new IllegalArgumentException("Pays de file invalide");
      states.put(code,Outcome.WAITING);
    }
  }
  synchronized String next(){
    if(cancelled||current!=null)return null;
    for(Map.Entry<String,Outcome> entry:states.entrySet())if(entry.getValue()==Outcome.WAITING){
      current=entry.getKey();entry.setValue(Outcome.RUNNING);return current;
    }
    return null;
  }
  synchronized void finish(String code,Outcome outcome){
    if(current==null||!current.equals(code)||outcome==null||outcome==Outcome.WAITING||outcome==Outcome.RUNNING)
      throw new IllegalStateException("Résultat sans transfert en cours");
    states.put(code,outcome);current=null;
  }
  synchronized void cancel(){
    cancelled=true;
    for(Map.Entry<String,Outcome> entry:states.entrySet())if(entry.getValue()==Outcome.WAITING)entry.setValue(Outcome.CANCELLED);
  }
  synchronized boolean cancelled(){return cancelled;}
  synchronized int count(Outcome outcome){int result=0;for(Outcome state:states.values())if(state==outcome)result++;return result;}
  synchronized int total(){return states.size();}
  synchronized Map<String,Outcome> snapshot(){return Collections.unmodifiableMap(new LinkedHashMap<>(states));}
}
