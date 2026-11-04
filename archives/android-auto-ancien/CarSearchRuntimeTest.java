package fr.roadguard.app;
import android.app.Application;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.model.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,34,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class CarSearchRuntimeTest {
  CarSearchScreen screen;
  @Before public void setup(){screen=new CarSearchScreen(TestCarContext.createCarContext(RuntimeEnvironment.getApplication()),(lat,lon,name)->{});}
  @After public void close(){screen.close();}
  void finishRequest()throws Exception{screen.lastRequest.get(10,TimeUnit.SECONDS);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
  @Test public void invalidAndDuplicatePlacesAreExcludedAndListIsBounded()throws Exception{
    JSONArray rows=new JSONArray().put(new JSONObject().put("lat",999).put("lng",5)).put(new JSONObject().put("lat",46).put("lng",5).put("name","Champagnole")).put(new JSONObject().put("lat",46).put("lng",5).put("name","Doublon"));
    for(int i=0;i<20;i++)rows.put(new JSONObject().put("lat",47+i*.01).put("lng",5).put("name","Lieu "+i));
    List<CarSearchScreen.Place> all=CarSearchScreen.parse(rows,"Recherche");assertEquals(6,all.size());assertEquals("Champagnole",all.get(0).name);
  }
  @Test public void submittedQueryBuildsHostSearchResults()throws Exception{
    screen.places=q->new JSONArray().put(new JSONObject().put("name","Pont-du-Navoy").put("lat",46.75).put("lng",5.8));
    screen.search("Pont du Navoy",true);assertTrue(((SearchTemplate)screen.onGetTemplate()).isLoading());finishRequest();
    SearchTemplate result=(SearchTemplate)screen.onGetTemplate();assertFalse(result.isLoading());assertEquals(1,result.getItemList().getItems().size());assertEquals("Pont-du-Navoy",((Row)result.getItemList().getItems().get(0)).getTitle().toString());
  }
  @Test public void coordinatesWorkWithoutSearchServer()throws Exception{
    screen.places=q->{throw new AssertionError("Le serveur ne doit pas être appelé pour des coordonnées");};screen.search("46.75,5.9",true);finishRequest();assertEquals(1,screen.results.size());assertEquals(46.75,screen.results.get(0).lat,.000001);
  }
  @Test public void networkErrorEndsLoadingAndAllowsRetry()throws Exception{
    screen.places=q->{throw new java.io.IOException("offline");};screen.search("Champagnole",true);finishRequest();assertFalse(screen.loading);assertTrue(screen.message.contains("indisponible"));assertNotNull(screen.onGetTemplate());
    screen.places=q->new JSONArray().put(new JSONObject().put("name","Champagnole").put("lat",46.75).put("lng",5.9));screen.search("Champagnole",true);finishRequest();assertEquals(1,screen.results.size());
  }
  @Test public void obsoleteResponseCannotReplaceNewerQuery()throws Exception{
    screen.search("Ancienne",false);int old=screen.generation;screen.search("Nouvelle",false);
    screen.publish(old,Collections.singletonList(new CarSearchScreen.Place(46,5,"Ancienne")),null);assertTrue(screen.results.isEmpty());assertEquals("Nouvelle",screen.query);
  }
  @Test public void closedScreenRejectsLateResponseAndStopsExecutor(){
    int token=screen.generation;screen.close();screen.publish(token,Collections.singletonList(new CarSearchScreen.Place(46,5,"Tardif")),null);assertTrue(screen.results.isEmpty());assertTrue(screen.io.isShutdown());
  }
}
