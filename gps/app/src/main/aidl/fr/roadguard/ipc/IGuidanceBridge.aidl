package fr.roadguard.ipc;
import fr.roadguard.ipc.IGuidanceListener;
import fr.roadguard.ipc.INavigationListener;
/** v1: authenticated local GPS-to-Companion bridge. */
interface IGuidanceBridge {
 void subscribe(IGuidanceListener listener);
 void unsubscribe(IGuidanceListener listener);
 boolean requestStop(String routeId);
 int navigationVersion();
 void navigation(String request, INavigationListener listener);
 void cancelNavigation(String scope);
 String mapInfo();
 byte[] mapTile(String catalog, String kind, int z, int x, int y);
}
