package fr.roadguard.app;
import android.content.Intent;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.Screen;
import androidx.car.app.validation.HostValidator;
public final class RoadguardCarAppService extends CarAppService {
  @Override public HostValidator createHostValidator(){return new HostValidator.Builder(this).addAllowedHosts(R.array.roadguard_allowed_hosts).build();}
  @Override public Session onCreateSession(){return new Session(){
    RoadguardCarScreen screen;
    @Override public Screen onCreateScreen(Intent intent){screen=new RoadguardCarScreen(getCarContext());screen.handleIntent(intent);return screen;}
    @Override public void onNewIntent(Intent intent){if(screen!=null)screen.handleIntent(intent);}
  };}
}
