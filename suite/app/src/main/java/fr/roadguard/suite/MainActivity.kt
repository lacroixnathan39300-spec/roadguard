package fr.roadguard.suite

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * Pilote Suite : installe et met à jour Pilote (téléphone) et Pilote Drive (autoradio) depuis le serveur
 * ROADGUARD, sans Play Store. Les APK arrivent en morceaux, sont vérifiés (empreinte SHA-256), puis installés
 * par l'installateur d'Android.
 */
class MainActivity : Activity() {
    private val server = "https://roadguard.roadguard-app.workers.dev"
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var manifest: JSONObject? = null
    private lateinit var list: LinearLayout
    private lateinit var status: TextView

    // Téléchargement en cours (une appli à la fois) : grande barre de progression dans la carte de l'appli.
    private var active: String? = null
    /** 0 : téléchargement, 1 : vérification du fichier, 2 : installation (fenêtre d'Android). */
    private var phase = 0
    @Volatile private var doneBytes = 0L
    private var totalBytes = 0L
    private var bar: ProgressBar? = null
    private var barPct: TextView? = null
    private var barText: TextView? = null

    private data class Module(val key: String, val pkg: String, val name: String, val text: String, val color: Int)
    private val modules = listOf(
        Module("gps", "fr.roadguard.gps", "Pilote", "GPS, radars, trafic et communauté sur le téléphone.", Color.parseColor("#1E88FF")),
        Module("drive", "fr.roadguard.drive", "Pilote Drive", "L'appli pour l'autoradio : accueil, carte, radars, musique.", Color.parseColor("#E53935")),
    )

    /** Autoradio : écran en paysage pas plus haut que 600 px (ex. 1024×600). */
    private val looksLikeCarUnit: Boolean
        get() = resources.displayMetrics.let { it.widthPixels > it.heightPixels && it.heightPixels <= 720 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B0F15")
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(28), dp(20), dp(20)); setBackgroundColor(Color.parseColor("#0B0F15")) }
        root.addView(text("Pilote Suite", 30f, Color.WHITE, bold = true))
        root.addView(text("Installe et mets à jour Pilote et Pilote Drive, sans Play Store.", 16f, Color.parseColor("#9FB0C3")))
        status = text("Recherche des dernières versions…", 14f, Color.parseColor("#9FB0C3")).also { it.setPadding(0, dp(10), 0, dp(6)) }
        root.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(ScrollView(this).apply { addView(root); setBackgroundColor(Color.parseColor("#0B0F15")) })
        handleInstallResult(intent)
        load()
    }

    override fun onResume() { super.onResume(); render() }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleInstallResult(intent) }

    private fun load() = io.execute {
        val m = runCatching { JSONObject(String(get("$server/v1/apps"))) }.getOrNull()
        main.post {
            manifest = m
            status.text = if (m == null) "Serveur injoignable : vérifie la connexion Internet." else "Dernières versions disponibles :"
            render()
        }
    }

    private fun installedVersion(pkg: String): Long? = runCatching {
        val i = packageManager.getPackageInfo(pkg, 0)
        if (Build.VERSION.SDK_INT >= 28) i.longVersionCode else @Suppress("DEPRECATION") i.versionCode.toLong()
    }.getOrNull()

    private fun render() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        if (!packageManager.canRequestPackageInstalls()) {
            list.addView(card(Color.parseColor("#B26A00")).apply {
                addView(text("Autorisation nécessaire", 18f, Color.WHITE, bold = true))
                addView(text("Android doit autoriser Pilote Suite à installer des applis (« sources inconnues »).", 15f, Color.WHITE))
                addView(button("Autoriser", Color.parseColor("#33000000")) {
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                })
            })
        }
        val m = manifest
        modules.forEach { mod ->
            val info = m?.optJSONObject(mod.key)
            val have = installedVersion(mod.pkg)
            val avail = info?.optLong("version")
            list.addView(card(Color.parseColor("#161C25")).apply {
                val recommended = (mod.key == "drive") == looksLikeCarUnit
                addView(text(mod.name + if (recommended) "  ·  recommandé ici" else "", 22f, mod.color, bold = true))
                addView(text(mod.text, 15f, Color.parseColor("#D5DCE5")))
                val state = when {
                    info == null -> "Pas encore disponible sur le serveur"
                    have == null -> "Non installée · ${mb(info.optLong("size"))}"
                    avail != null && avail > have -> "Mise à jour disponible (${info.optInt("build")}) · ${mb(info.optLong("size"))}"
                    else -> "À jour"
                }
                // Installation terminée pendant qu'on était dans la fenêtre d'Android : on referme la barre.
                if (active == mod.key && phase == 2 && have != null && avail != null && have >= avail) finishDownload()
                if (active == mod.key) { addView(progressSection(mod)); return@apply }
                addView(text(state, 14f, Color.parseColor("#9FB0C3")))
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, 0) }
                if (info != null && (have == null || (avail != null && avail > have))) {
                    if (active == null) row.addView(button(if (have == null) "Installer" else "Mettre à jour", mod.color) { download(mod, info) })
                    else row.addView(text("Attends la fin du téléchargement en cours…", 14f, Color.parseColor("#9FB0C3")))
                }
                if (have != null) row.addView(button("Ouvrir", Color.parseColor("#2A3442")) {
                    packageManager.getLaunchIntentForPackage(mod.pkg)?.let { startActivity(it) }
                })
                addView(row)
            })
        }
        list.addView(text("Android peut afficher « appli inconnue » (Play Protect) : touche « Installer quand même ». Les applis sont signées par Nathan Lacroix ; les mises à jour gardent tes réglages.",
            13f, Color.parseColor("#7F8EA0")).also { it.setPadding(0, dp(14), 0, 0) })
    }

    private fun download(mod: Module, info: JSONObject) {
        if (!packageManager.canRequestPackageInstalls()) { render(); return }
        if (active != null) return
        val build = info.optInt("build"); val chunks = info.optInt("chunks"); val sha = info.optString("sha256")
        active = mod.key; phase = 0; doneBytes = 0; totalBytes = info.optLong("size")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // pas de mise en veille pendant le téléchargement
        status.text = "${mod.name} : en cours de téléchargement…"
        render()
        io.execute {
            try {
                val file = File(cacheDir, "${mod.key}-$build.apk")
                val digest = MessageDigest.getInstance("SHA-256")
                var lastUi = 0L
                file.outputStream().use { out ->
                    for (i in 0 until chunks) {
                        val before = doneBytes
                        var part: ByteArray? = null
                        var error: Exception? = null
                        for (attempt in 1..3) {
                            doneBytes = before
                            try {
                                part = get("$server/v1/apps/${mod.key}/$build/$i") { n ->
                                    doneBytes += n
                                    val now = System.currentTimeMillis()
                                    if (now - lastUi > 120) { lastUi = now; main.post { updateProgress() } }
                                }
                                break
                            } catch (e: Exception) { error = e }
                        }
                        val bytes = part ?: throw (error ?: IllegalStateException("téléchargement interrompu"))
                        digest.update(bytes); out.write(bytes)
                        doneBytes = before + bytes.size
                        main.post { updateProgress() }
                    }
                }
                main.post { phase = 1; render() }
                val got = digest.digest().joinToString("") { "%02x".format(it) }
                if (sha.isNotBlank() && got != sha) throw IllegalStateException("fichier abîmé, réessaie")
                main.post { phase = 2; status.text = "${mod.name} : installation…"; render() }
                install(file)
            } catch (e: Exception) {
                main.post { finishDownload(); status.text = "${mod.name} : échec (${e.message})"; render() }
            }
        }
    }

    private fun finishDownload() {
        active = null; phase = 0; bar = null; barPct = null; barText = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Bloc « En cours de téléchargement » : titre, pourcentage, grosse barre, Mo reçus. */
    private fun progressSection(mod: Module) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, dp(2))
        val head = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(text(when (phase) {
            0 -> "⬇ En cours de téléchargement…"
            1 -> "🔎 Vérification du fichier…"
            else -> "📲 Installation en cours…"
        }, 18f, Color.WHITE, bold = true).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        barPct = text("", 20f, mod.color, bold = true).also { head.addView(it) }
        addView(head)
        bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            isIndeterminate = phase != 0
            progressDrawable = barDrawable(mod.color)
            indeterminateTintList = ColorStateList.valueOf(mod.color)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(18)).apply { topMargin = dp(10); bottomMargin = dp(8) }
        }.also { addView(it) }
        barText = text("", 14f, Color.parseColor("#C7D2DE")).also { addView(it) }
        updateProgress()
    }

    private fun updateProgress() {
        val f = if (totalBytes > 0) (doneBytes * 1000 / totalBytes).toInt().coerceIn(0, 1000) else 0
        bar?.progress = f
        barPct?.text = if (phase == 0) "${f / 10} %" else ""
        barText?.text = when (phase) {
            0 -> "${mb1(doneBytes)} sur ${mb(totalBytes)} · garde l'écran allumé et ne ferme pas Pilote Suite"
            1 -> "Le fichier est contrôlé avant l'installation (sécurité)."
            else -> "Confirme dans la fenêtre d'Android (« Installer », ou « Mettre à jour »)."
        }
    }

    private fun barDrawable(color: Int): LayerDrawable {
        val track = GradientDrawable().apply { cornerRadius = dp(9).toFloat(); setColor(Color.parseColor("#2A3442")) }
        val fill = ClipDrawable(GradientDrawable().apply { cornerRadius = dp(9).toFloat(); setColor(color) }, Gravity.START, ClipDrawable.HORIZONTAL)
        return LayerDrawable(arrayOf(track, fill)).apply { setId(0, android.R.id.background); setId(1, android.R.id.progress) }
    }

    private fun install(apk: File) {
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("app.apk", 0, apk.length()).use { out -> apk.inputStream().use { it.copyTo(out) }; session.fsync(out) }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getActivity(this, id, Intent(this, MainActivity::class.java).setAction(ACTION_STATUS), flags)
            session.commit(pi.intentSender)
        }
        apk.delete()
    }

    private fun handleInstallResult(intent: Intent?) {
        if (intent?.action != ACTION_STATUS) return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION") val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> { finishDownload(); status.text = "Installé ✔"; render() }
            else -> {
                finishDownload()
                status.text = "Installation annulée ou refusée : " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "")
                render()
            }
        }
    }

    /** Requête bloquante ; [onBytes] : octets reçus au fur et à mesure (barre de progression). */
    private fun get(url: String, onBytes: ((Int) -> Unit)? = null): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 60_000
        c.setRequestProperty("User-Agent", "Pilote-Suite/1")
        try {
            if (c.responseCode !in 200..299) throw IllegalStateException("serveur ${c.responseCode}")
            val out = java.io.ByteArrayOutputStream(maxOf(c.contentLength, 0))
            val buf = ByteArray(64 * 1024)
            c.inputStream.use { input ->
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n); onBytes?.invoke(n)
                }
            }
            return out.toByteArray()
        } finally { c.disconnect() }
    }

    private fun mb(bytes: Long) = "%.0f Mo".format(bytes / 1_048_576.0)
    private fun mb1(bytes: Long) = "%.1f Mo".format(java.util.Locale.FRANCE, bytes / 1_048_576.0)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun card(bg: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(bg) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
    }
    private fun button(label: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); isAllCaps = false; textSize = 16f; gravity = Gravity.CENTER
        background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(color) }
        setPadding(dp(18), dp(10), dp(18), dp(10))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(10) }
        setOnClickListener { onClick() }
    }

    companion object { private const val ACTION_STATUS = "fr.roadguard.suite.INSTALL_STATUS" }
}
