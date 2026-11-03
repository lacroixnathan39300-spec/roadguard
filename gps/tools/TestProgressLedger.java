package fr.roadguard.app;
import org.json.*;
import java.nio.file.*;

public class TestProgressLedger {
  static void reject(JSONObject json)throws Exception{try{new ProgressLedger(json);throw new AssertionError("Invalid ledger accepted");}catch(java.io.IOException expected){}}
  public static void main(String[] args)throws Exception{
    String text=new String(Files.readAllBytes(Paths.get(args[0])),java.nio.charset.StandardCharsets.UTF_8);
    ProgressLedger ledger=new ProgressLedger(new JSONObject(text));
    if(ledger.modules.size()!=7||ledger.validation>ledger.development)throw new AssertionError("Invalid totals");
    if(!ProgressLedger.percent(1d/3).equals("0,3333 %"))throw new AssertionError("Four decimal places");
    JSONObject bad=new JSONObject(text);bad.getJSONArray("modules").getJSONObject(0).put("weight",21);reject(bad);
    bad=new JSONObject(text);JSONObject task=bad.getJSONArray("modules").getJSONObject(0).getJSONArray("tasks").getJSONObject(0);task.put("coded",false);task.getJSONArray("checks").getJSONObject(0).put("passed",true).put("evidence","proof");reject(bad);
    bad=new JSONObject(text);task=bad.getJSONArray("modules").getJSONObject(0).getJSONArray("tasks").getJSONObject(0);task.getJSONArray("checks").getJSONObject(0).put("passed",true).put("evidence","");reject(bad);
    bad=new JSONObject(text);task=bad.getJSONArray("modules").getJSONObject(0).getJSONArray("tasks").getJSONObject(0);task.getJSONArray("checks").getJSONObject(0).put("weight",99);reject(bad);
    System.out.println("PASS weighted ledger: seven modules, 100 points, four decimals, invalid weights and unproved validation rejected");
  }
}
