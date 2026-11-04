package fr.roadguard.app.link;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;

/**
 * Écran « ROADGUARD Link » du téléphone : association avec l'autoradio (code affiché par DRIVE),
 * démarrage/arrêt de la liaison, autoradios associés.
 */
public final class LinkActivity extends Activity {
    private static final int BG = 0xFF05070A, PANEL = 0xFF121820, ACCENT = 0xFF00B8FF, MUTED = 0xFF9AA7B4;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button toggle;
    private LinearLayout peers;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(20));
        root.setBackgroundColor(BG);

        root.addView(text("ROADGUARD Link", 28, Color.WHITE, true));
        root.addView(text("Ton téléphone envoie sa position GPS et sa musique à l'autoradio (DRIVE). "
            + "Active le partage de connexion du téléphone et connecte l'autoradio à ce Wi-Fi.", 15, MUTED, false));

        status = text("", 18, ACCENT, true);
        status.setPadding(0, dp(18), 0, dp(8));
        root.addView(status);

        toggle = button("Démarrer la liaison", v -> {
            if (LinkService.running) startService(new Intent(this, LinkService.class).setAction(LinkService.ACTION_STOP));
            else startForegroundService(new Intent(this, LinkService.class));
            handler.postDelayed(this::refresh, 400);
        });
        root.addView(toggle);

        root.addView(section("Associer un autoradio"));
        root.addView(text("Sur l'autoradio : ROADGUARD DRIVE → bouton du téléphone → « Associer ». Tape ici le code à 6 chiffres affiché.", 15, MUTED, false));
        EditText code = new EditText(this);
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        code.setHint("Code à 6 chiffres");
        code.setTextColor(Color.WHITE);
        code.setHintTextColor(MUTED);
        code.setTextSize(24);
        code.setGravity(Gravity.CENTER);
        root.addView(code);
        root.addView(button("Associer", v -> {
            String c = code.getText().toString().trim();
            if (!c.matches("\\d{6}")) { toast("Le code fait 6 chiffres."); return; }
            // Le code reste valable 3 minutes, le temps que l'autoradio se connecte.
            LinkService.prefs(this).edit().putString("pending_code", c)
                .putLong("pending_until", System.currentTimeMillis() + 180_000).apply();
            if (!LinkService.running) startForegroundService(new Intent(this, LinkService.class));
            toast("Code enregistré : appuie sur « Connecter » sur l'autoradio.");
            code.setText("");
        }));

        root.addView(section("Musique"));
        root.addView(text("Pour afficher et piloter la musique du téléphone sur l'autoradio, autorise ROADGUARD dans « Accès aux notifications ».", 15, MUTED, false));
        root.addView(button("Ouvrir l'accès aux notifications",
            v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));

        root.addView(section("Notifications sur l'autoradio"));
        root.addView(text("Messages (SMS, WhatsApp…) et appels s'affichent sur le poste, avec leurs boutons (répondre, refuser…). "
            + "Nécessite le même accès aux notifications que la musique.", 15, MUTED, false));
        android.widget.Switch notif = new android.widget.Switch(this);
        notif.setText("Transmettre messages et appels");
        notif.setTextColor(Color.WHITE);
        notif.setTextSize(17);
        notif.setChecked(LinkService.notificationsEnabled(this));
        notif.setOnCheckedChangeListener((b, on) -> LinkService.prefs(this).edit().putBoolean("forward_notifications", on).apply());
        notif.setPadding(0, dp(8), 0, dp(8));
        root.addView(notif);

        root.addView(section("Envoyer une destination"));
        root.addView(text("Dans Google Maps, Waze ou Contacts : « Partager » → « ROADGUARD Link ». L'autoradio lance le guidage.", 15, MUTED, false));

        root.addView(section("Autoradios associés"));
        peers = new LinearLayout(this);
        peers.setOrientation(LinearLayout.VERTICAL);
        root.addView(peers);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
        handleShare(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShare(intent);
    }

    /** Partage reçu : envoyer la destination à l'autoradio connecté. */
    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String all = (subject == null ? "" : subject + "\n") + (text == null ? "" : text);
        if (LinkService.sendDestination(all)) toast("Destination envoyée à l'autoradio.");
        else toast("L'autoradio n'est pas connecté : démarre la liaison puis partage à nouveau.");
        setIntent(new Intent(this, LinkActivity.class));
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private void refresh() {
        status.setText("État : " + LinkService.status);
        toggle.setText(LinkService.running ? "Arrêter la liaison" : "Démarrer la liaison");
        peers.removeAllViews();
        SharedPreferences p = LinkService.prefs(this);
        boolean any = false;
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (!e.getKey().startsWith("peer_") || e.getKey().startsWith("peer_name_")) continue;
            any = true;
            String id = e.getKey().substring(5);
            String name = p.getString("peer_name_" + id, "Autoradio");
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(8), dp(10));
            row.setBackground(round(PANEL));
            TextView label = text(name.isEmpty() ? "Autoradio" : name, 17, Color.WHITE, false);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
            Button forget = new Button(this);
            forget.setText("Oublier");
            forget.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Oublier cet autoradio ?")
                .setMessage("Il faudra refaire l'association avec un nouveau code.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Oublier", (d, w) -> {
                    p.edit().remove("peer_" + id).remove("peer_name_" + id).apply();
                    refresh();
                }).show());
            row.addView(forget);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(8);
            peers.addView(row, lp);
        }
        if (!any) peers.addView(text("Aucun pour l'instant.", 15, MUTED, false));
        handler.postDelayed(this::refresh, 1500);
    }

    private TextView section(String title) {
        TextView t = text(title, 19, ACCENT, true);
        t.setPadding(0, dp(24), 0, dp(6));
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(Color.WHITE);
        b.setBackground(round(ACCENT));
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(54));
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    private GradientDrawable round(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        return d;
    }

    private void toast(String s) { android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
