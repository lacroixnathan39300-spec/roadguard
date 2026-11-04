package fr.roadguard.app.link;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Tactile du mode miroir : rejoue sur le téléphone les appuis et glissés faits sur l'autoradio.
 * Service d'accessibilité activé volontairement par l'utilisateur ; il ne lit aucun contenu de l'écran.
 */
public final class MirrorTouchService extends AccessibilityService {
    public static volatile MirrorTouchService instance;
    private final List<float[]> points = new ArrayList<>();
    private long downAt;

    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
    @Override public void onDestroy() { instance = null; super.onDestroy(); }

    /** Coordonnées 0–1 relatives à l'image envoyée ; action « down », « move » ou « up ». */
    public synchronized void touch(String action, float x, float y, long timeMs) {
        DisplayMetrics m = new DisplayMetrics();
        ((DisplayManager) getSystemService(DISPLAY_SERVICE)).getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(m);
        float px = Math.max(0, Math.min(1, x)) * (m.widthPixels - 1);
        float py = Math.max(0, Math.min(1, y)) * (m.heightPixels - 1);
        switch (action) {
            case "down": points.clear(); points.add(new float[]{px, py}); downAt = timeMs; break;
            case "move": if (!points.isEmpty() && points.size() < 60) points.add(new float[]{px, py}); break;
            case "up":
                if (points.isEmpty()) return;
                points.add(new float[]{px, py});
                dispatch(Math.max(1, timeMs - downAt));
                points.clear();
                break;
            default: break;
        }
    }

    private void dispatch(long durationMs) {
        float[] first = points.get(0), last = points.get(points.size() - 1);
        float dx = last[0] - first[0], dy = last[1] - first[1];
        Path path = new Path();
        path.moveTo(first[0], first[1]);
        boolean tap = dx * dx + dy * dy < 30 * 30;
        if (!tap) for (int i = 1; i < points.size(); i++) path.lineTo(points.get(i)[0], points.get(i)[1]);
        // Appui court = tap ; appui tenu = appui long ; sinon glissé à la même vitesse.
        long duration = tap ? (durationMs >= 500 ? Math.min(durationMs, 1500) : 60) : Math.max(100, Math.min(durationMs, 3000));
        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, duration)).build();
        dispatchGesture(g, null, null);
    }
}
