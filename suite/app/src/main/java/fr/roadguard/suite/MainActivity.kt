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
import rikka.shizuku.Shizuku
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

    // Pilote « pour Android Auto » : installation par l'installateur d'Android, marquée Play Store (voir PlayMark).
    private val prefs by lazy { getSharedPreferences("suite", MODE_PRIVATE) }
    /** Fenêtre d'installation d'Android ouverte pour Pilote : on vérifie la marque au retour. */
    private var autoPending = false
    private var autoSawPause = false
    private var autoViaShell = false
    private var autoRetried = false
    private var autoBefore = 0L
    private var autoAttempt = 0
    private var showAutoGuide = false
    /** Résultat du bouton « Diagnostic Android Auto » (origine notée par Android et verdict d'Android Auto). */
    private var diag: String? = null
    private val binderReceived = Shizuku.OnBinderReceivedListener { main.post { render() } }
    private val binderDead = Shizuku.OnBinderDeadListener { main.post { render() } }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> main.post { render() } }

    private data class Module(val key: String, val pkg: String, val name: String, val text: String, val color: Int)
    private val modules = listOf(
        Module("gps", "fr.roadguard.gps", "Pilote", "GPS, radars, trafic et communauté sur le téléphone.", Color.parseColor("#1E88FF")),
        Module("drive", "fr.roadguard.drive", "Pilote Drive", "L'appli pour l'autoradio : accueil, carte, radars, musique.", Color.parseColor("#E53935")),
    )

    /** Pilote Suite elle-même : elle se met à jour comme les autres (plus besoin de la réinstaller par son lien). */
    private val self = Module("suite", "fr.roadguard.suite", "Pilote Suite", "Cette appli : elle installe et met à jour les autres.", Color.parseColor("#8E9BAE"))

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
        // Shizuku (installation de Pilote pour Android Auto) : l'écran suit son état (démarré, autorisé).
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceived)
            Shizuku.addBinderDeadListener(binderDead)
            Shizuku.addRequestPermissionResultListener(permissionResult)
        }
        load()
    }

    override fun onDestroy() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceived)
            Shizuku.removeBinderDeadListener(binderDead)
            Shizuku.removeRequestPermissionResultListener(permissionResult)
        }
        super.onDestroy()
    }

    override fun onPause() { super.onPause(); if (autoPending) autoSawPause = true }

    override fun onResume() {
        super.onResume()
        // Retour de la fenêtre d'installation d'Android : on laisse Android enregistrer, puis on vérifie la marque.
        if (autoPending && autoSawPause) {
            autoSawPause = false
            val attempt = autoAttempt
            main.postDelayed({ if (attempt == autoAttempt) verifyAuto() }, 800)
        }
        render()
    }

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
        // Pilote Suite à mettre à jour : sa carte passe en premier (les nouveautés d'installation sont dedans).
        val selfUpdate = (m?.optJSONObject(self.key)?.optLong("version") ?: 0L) > (installedVersion(self.pkg) ?: Long.MAX_VALUE)
        (if (selfUpdate) listOf(self) + modules else modules + self).forEach { mod ->
            val info = m?.optJSONObject(mod.key)
            val have = installedVersion(mod.pkg)
            val avail = info?.optLong("version")
            val isSelf = mod.key == self.key
            list.addView(card(Color.parseColor("#161C25")).apply {
                val recommended = !isSelf && (mod.key == "drive") == looksLikeCarUnit
                addView(text(mod.name + if (recommended) "  ·  recommandé ici" else "", if (isSelf) 18f else 22f, mod.color, bold = true))
                addView(text(if (isSelf && selfUpdate) "Mets-la à jour en premier, puis rouvre-la." else mod.text, 15f, Color.parseColor("#D5DCE5")))
                val state = when {
                    info == null -> "Pas encore disponible sur le serveur"
                    have == null -> "Non installée · ${mb(info.optLong("size"))}"
                    avail != null && avail > have -> "Mise à jour disponible (${info.optInt("build")}) · ${mb(info.optLong("size"))}"
                    else -> "À jour"
                }
                // Installation terminée pendant qu'on était dans la fenêtre d'Android : on referme la barre.
                if (active == mod.key && phase == 2 && !autoPending && have != null && avail != null && have >= avail) finishDownload()
                if (active == mod.key) { addView(progressSection(mod)); return@apply }
                addView(text(state, 14f, Color.parseColor("#9FB0C3")))
                // Pilote sur un téléphone : installation « pour Android Auto » (marque Play Store) quand Shizuku est prêt.
                val auto = mod.key == "gps" && !looksLikeCarUnit
                val marked = auto && have != null && PlayMark.marked(context, mod.pkg)
                val update = info != null && (have == null || (avail != null && avail > have))
                // Pilote est dans Android Auto mais Shizuku ne tourne pas : cette mise à jour l'en retirerait.
                val losesAuto = update && marked && prefs.getBoolean(KEY_SHELL_KEEPS, true) && !PlayMark.shizukuReady()
                if (losesAuto && active == null) {
                    addView(text("⚠ Shizuku n'est pas démarré : mettre à jour maintenant retirerait Pilote d'Android Auto. Démarre Shizuku, puis reviens ici.",
                        14f, Color.parseColor("#FFB74D")).also { it.setPadding(0, dp(8), 0, 0) })
                }
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, 0) }
                if (update && info != null) {
                    if (active != null) row.addView(text("Attends la fin du téléchargement en cours…", 14f, Color.parseColor("#9FB0C3")))
                    else if (losesAuto) row.addView(button("Ouvrir Shizuku", mod.color) { openShizuku() })
                    else row.addView(button(if (have == null) "Installer" else "Mettre à jour", mod.color) { download(mod, info) })
                }
                if (have != null && !isSelf) row.addView(button("Ouvrir", Color.parseColor("#2A3442")) {
                    packageManager.getLaunchIntentForPackage(mod.pkg)?.let { startActivity(it) }
                })
                addView(row)
                if (losesAuto && active == null && info != null) addView(button("Mettre à jour sans Android Auto", Color.parseColor("#2A3442")) { download(mod, info) }
                    .also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = dp(8) })
                if (auto && info != null) addView(autoSection(mod, info, have != null, marked))
            })
        }
        list.addView(text("Android peut afficher « appli inconnue » (Play Protect) : touche « Installer quand même ». Les applis sont signées par Nathan Lacroix ; les mises à jour gardent tes réglages.",
            13f, Color.parseColor("#7F8EA0")).also { it.setPadding(0, dp(14), 0, 0) })
    }

    private fun download(mod: Module, info: JSONObject) {
        if (!packageManager.canRequestPackageInstalls()) { render(); return }
        if (active != null) return
        val build = info.optInt("build"); val chunks = info.optInt("chunks"); val sha = info.optString("sha256")
        // Pilote sur un téléphone : passe par l'installateur d'Android avec la marque Play Store (pour Android Auto).
        val auto = mod.key == "gps" && !looksLikeCarUnit
        active = mod.key; phase = 0; doneBytes = 0; totalBytes = info.optLong("size")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // pas de mise en veille pendant le téléchargement
        status.text = "${mod.name} : en cours de téléchargement…"
        render()
        io.execute {
            try {
                val file = if (auto) File(cacheDir, ApkProvider.NAME) else File(cacheDir, "${mod.key}-$build.apk")
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
                if (auto) main.post { startAutoInstall() } else install(file)
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

    // ---------- Pilote pour Android Auto : installation marquée « Play Store » ----------

    private fun lastUpdate(pkg: String): Long = runCatching { packageManager.getPackageInfo(pkg, 0).lastUpdateTime }.getOrDefault(0L)

    /** APK de Pilote prêt (cache/install.apk) : ouvre l'installateur d'Android, par Shizuku s'il est prêt. */
    private fun startAutoInstall() {
        autoRetried = false
        autoBefore = lastUpdate(GPS)
        launchAuto(shell = PlayMark.shizukuReady() && prefs.getBoolean(KEY_SHELL_KEEPS, true))
    }

    private fun launchAuto(shell: Boolean) {
        autoPending = true; autoSawPause = false; autoViaShell = shell
        val attempt = ++autoAttempt
        val uri = ApkProvider.uri()
        PlayMark.grantToInstallers(this, uri)
        if (!shell) { launchAsApp(uri); return }
        val installer = PlayMark.installerPackage(this)
        var answered = false
        // Shizuku n'a pas pu ouvrir l'installateur : ouverture normale (Pilote s'installe quand même).
        val fallback = { if (attempt == autoAttempt && autoPending && !autoSawPause) launchAsApp(uri) }
        if (Build.VERSION.SDK_INT <= 34) {
            // Android 14 et avant : le shell ouvre l'installateur avec Pilote Suite comme destinataire du résultat.
            val intent = PlayMark.shellIntent(uri, installer)
            val token = window.attributes.token
            PlayMark.withShell(this, { it.startForResult(intent, token, REQ_AUTO) }) { out -> answered = true; if (out != "ok") fallback() }
        } else {
            PlayMark.withShell(this, { it.run(PlayMark.shellCommand(uri, installer)) }) { out ->
                answered = true
                if (out == null || out.contains("Error") || out.contains("[exit ")) fallback()
            }
        }
        // Shizuku ne répond pas (arrêté entre-temps) : on n'attend pas indéfiniment.
        main.postDelayed({ if (!answered) fallback() }, 15_000)
    }

    /** Installation ouverte par Pilote Suite elle-même (marque demandée, gardée seulement par certains téléphones). */
    private fun launchAsApp(uri: Uri) {
        autoViaShell = false
        @Suppress("DEPRECATION")
        val ok = runCatching { startActivityForResult(PlayMark.appIntent(uri, GPS), REQ_AUTO) }.isSuccess
        if (ok) return
        // Dernier recours : l'installation classique (sans marque).
        autoPending = false
        val file = File(cacheDir, ApkProvider.NAME)
        io.execute {
            runCatching { install(file) }.onFailure { e -> main.post { finishDownload(); status.text = "Pilote : échec (${e.message})"; render() } }
        }
    }

    /** Au retour de l'installateur : Pilote est-il installé, et avec la marque qu'Android Auto accepte ? */
    private fun verifyAuto() {
        if (!autoPending) return
        val now = lastUpdate(GPS)
        if (now == 0L || now == autoBefore) {
            endAuto(if (autoRetried) "Pilote est installé, mais pas encore visible dans Android Auto." else "Pilote : installation annulée ou refusée.",
                installed = false)
            return
        }
        if (PlayMark.marked(this, GPS)) {
            prefs.edit().putBoolean(KEY_SHELL_KEEPS, autoViaShell).remove(KEY_NO_MARK).apply()
            endAuto("Pilote installé ✔ · prêt pour Android Auto. Force l'arrêt d'Android Auto, puis rebranche le téléphone.")
            return
        }
        // Installé, mais l'installateur noté n'est pas le Play Store (cas courant : la fenêtre d'Android ignore
        // l'étiquette demandée). L'initiateur, lui, est bon. On réécrit juste l'installateur via Shizuku (shell).
        if (!autoRetried && PlayMark.shizukuReady()) {
            autoRetried = true
            status.text = "Dernière étape pour Android Auto : correction de l'étiquette Play Store…"
            render()
            PlayMark.withShell(this, { it.setInstaller(GPS, PlayMark.VENDING) }) { out ->
                if (!autoPending) return@withShell
                if (PlayMark.marked(this, GPS)) {
                    prefs.edit().putBoolean(KEY_SHELL_KEEPS, autoViaShell).remove(KEY_NO_MARK).apply()
                    endAuto("Pilote installé ✔ · prêt pour Android Auto. Force l'arrêt d'Android Auto, puis rebranche le téléphone.")
                } else {
                    prefs.edit().putBoolean(KEY_NO_MARK, true).apply()
                    endAuto("Pilote installé ✔, mais ce téléphone refuse l'étiquette Play Store" +
                        (if (out != null && out != "ok") " ($out)" else "") +
                        ". Sans elle, Android Auto ne l'affiche pas — seul le Play Store peut la poser ici.")
                }
            }
            return
        }
        prefs.edit().putBoolean(KEY_NO_MARK, true).apply()
        endAuto(if (PlayMark.shizukuReady()) "Pilote installé ✔ · ce téléphone n'a pas gardé la marque pour Android Auto."
            else "Pilote installé ✔ · démarre Shizuku pour l'activer dans Android Auto.")
    }

    private fun endAuto(message: String, installed: Boolean = true) {
        autoPending = false
        autoAttempt++
        // Annulé : le fichier reste (l'installateur peut encore être ouvert) ; il sera remplacé au prochain téléchargement.
        if (installed) File(cacheDir, ApkProvider.NAME).delete()
        finishDownload()
        status.text = message
        render()
    }

    /**
     * Pourquoi Android Auto n'affiche pas Pilote : l'origine de l'installation notée par Android et, si Shizuku tourne,
     * les lignes du validateur d'Android Auto dans le journal du téléphone (« Package DENIED; raison [paquet] »).
     * Pour un verdict frais : forcer l'arrêt d'Android Auto, ouvrir « Personnaliser le lanceur », puis lancer ceci.
     */
    private fun runDiagnostic() {
        val (initiating, installer) = PlayMark.source(this, GPS)
        val head = "Pilote ${installedVersion(GPS)} · installateur=$installer · initiateur=$initiating\n" +
            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n"
        if (!PlayMark.shizukuReady()) {
            diag = head + "Shizuku n'est pas démarré : démarre-le pour lire aussi le verdict d'Android Auto."
            render(); return
        }
        diag = head + "Lecture en cours…"
        render()
        val script = """
            dumpsys package fr.roadguard.gps | grep -E 'installerPackageName|initiatingPackageName|originatingPackageName|updateOwnerPackageName|packageSource' | head -n 8
            echo '--- Android Auto'
            dumpsys package com.google.android.projection.gearhead | grep -m 1 versionName
            echo '--- verdict (journal)'
            logcat -d -v brief | grep -E 'CAR\.VALIDATOR|Validator' | grep -i -E 'roadguard|DENIED|not connected' | tail -n 25
            true
        """.trimIndent()
        PlayMark.withShell(this, { it.run(arrayOf("/system/bin/sh", "-c", script)) }) { out ->
            diag = head + (out?.trim()?.ifEmpty { null } ?: "Shizuku n'a pas répondu : relance-le.")
            render()
        }
    }

    /** Corrige l'étiquette d'un Pilote déjà installé : réécrit l'installateur en Play Store, sans réinstaller. */
    private fun fixMark() {
        if (!PlayMark.shizukuReady()) { status.text = "Démarre Shizuku et autorise Pilote Suite d'abord."; return }
        status.text = "Correction de l'étiquette Play Store…"
        PlayMark.withShell(this, { it.setInstaller(GPS, PlayMark.VENDING) }) { out ->
            if (PlayMark.marked(this, GPS)) {
                prefs.edit().remove(KEY_NO_MARK).apply()
                status.text = "Étiquette posée ✔ · force l'arrêt d'Android Auto, puis rebranche le téléphone."
            } else {
                status.text = "Ce téléphone a refusé l'étiquette" + (if (out != null && out != "ok") " ($out)" else "") + "."
            }
            render()
        }
    }

    private fun openShizuku() {
        val launch = packageManager.getLaunchIntentForPackage(PlayMark.SHIZUKU)
        if (launch != null) { startActivity(launch); return }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${PlayMark.SHIZUKU}"))) }
            .onFailure { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${PlayMark.SHIZUKU}"))) } }
    }

    /** Sous la carte de Pilote : état d'Android Auto, et le guide pour l'activer (Shizuku). */
    private fun autoSection(mod: Module, info: JSONObject, installed: Boolean, marked: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0)
        val grey = Color.parseColor("#9FB0C3"); val green = Color.parseColor("#2ED573")
        fun spaced(v: View) = v.also { (it.layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(8); addView(it) }
        // Ce qu'Android a noté pour Pilote (utile pour comprendre un refus d'Android Auto).
        fun details() { if (installed) PlayMark.source(context, mod.pkg).let { (initiating, installer) ->
            addView(text("Origine notée par Android : installateur ${installer ?: "aucun"}, initiateur ${initiating ?: "aucun"} · Android ${Build.VERSION.RELEASE}, ${Build.MANUFACTURER} ${Build.MODEL}",
                12f, Color.parseColor("#7F8EA0")).also { it.setPadding(0, dp(8), 0, 0) }) } }
        // Diagnostic : ce qu'Android a noté et, avec Shizuku, le verdict écrit par Android Auto dans le journal du téléphone.
        fun diagnostic() {
            if (!installed) return
            spaced(button("Diagnostic Android Auto", Color.parseColor("#2A3442")) { runDiagnostic() })
            diag?.let { d ->
                addView(TextView(context).apply {
                    text = d; textSize = 11f; setTextColor(Color.parseColor("#C7D2DE")); typeface = Typeface.MONOSPACE
                    setTextIsSelectable(true); setPadding(0, dp(8), 0, 0)
                })
                spaced(button("Copier", Color.parseColor("#2A3442")) {
                    (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(android.content.ClipData.newPlainText("Diagnostic Android Auto", d))
                    status.text = "Diagnostic copié."
                })
            }
        }
        if (marked) {
            addView(text("🚗 Android Auto : prêt ✔  Pilote porte la marque « Play Store ».", 14f, green))
            addView(text("Pilote n'apparaît pas dans la voiture ? Android Auto ne relit ses applis qu'après un arrêt : touche le bouton, puis « Forcer l'arrêt », et rebranche le téléphone. Vérifie aussi « Personnaliser le lanceur d'applications » dans ses réglages.",
                14f, Color.parseColor("#D5DCE5")).also { it.setPadding(0, dp(6), 0, 0) })
            spaced(button("Ouvrir la fiche d'Android Auto", Color.parseColor("#2A3442")) {
                runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${PlayMark.ANDROID_AUTO}"))) }
                    .onFailure { status.text = "Android Auto n'est pas installé sur cet appareil." }
            })
            details()
            diagnostic()
            return@apply
        }
        addView(text(if (installed) "🚗 Android Auto : Pilote n'y apparaît pas encore." else "🚗 Android Auto : pour que Pilote s'y affiche, prépare-le avant d'installer.", 14f, grey))
        // Pilote déjà installé avec le bon initiateur : il suffit de corriger l'installateur, sans réinstaller.
        if (installed) {
            val (initiating, _) = PlayMark.source(context, mod.pkg)
            if (initiating == PlayMark.VENDING || initiating == PlayMark.GOOGLE_INSTALLER) {
                addView(text("Il ne manque que l'étiquette « Play Store » sur l'installateur. Shizuku peut la poser sans réinstaller Pilote.",
                    14f, Color.parseColor("#D5DCE5")).also { it.setPadding(0, dp(8), 0, 0) })
                spaced(button(if (PlayMark.shizukuReady()) "Corriger l'étiquette Play Store" else "Démarrer Shizuku puis corriger",
                    if (PlayMark.shizukuReady()) mod.color else Color.parseColor("#3A4654")) {
                    if (PlayMark.shizukuReady()) fixMark() else openShizuku()
                })
            }
        }
        details()
        diagnostic()
        if (!showAutoGuide) {
            spaced(button("Afficher Pilote dans Android Auto", Color.parseColor("#2A3442")) { showAutoGuide = true; render() })
            return@apply
        }
        val inst = PlayMark.shizukuInstalled(context); val running = PlayMark.shizukuRunning(); val ready = running && PlayMark.shizukuGranted()
        fun step(n: Int, t: String, done: Boolean) = addView(text((if (done) "✔ " else "$n. ") + t, 14f, if (done) green else Color.WHITE).also { it.setPadding(0, dp(8), 0, 0) })
        addView(text("Android Auto n'affiche un GPS que s'il le croit installé par le Play Store. Pilote Suite sait faire cette installation spéciale avec l'appli gratuite Shizuku. À préparer une seule fois :",
            14f, Color.parseColor("#D5DCE5")).also { it.setPadding(0, dp(8), 0, 0) })
        step(1, "Installe l'appli Shizuku.", inst)
        if (!inst) spaced(button("Installer Shizuku", mod.color) { openShizuku() })
        step(2, "Active les options pour les développeurs : Paramètres → À propos du téléphone → touche 7 fois « Numéro de build ».", running)
        step(3, "Connecte le téléphone au Wi-Fi. Ouvre Shizuku et lance l'association (débogage sans fil). Puis Paramètres → Options pour les développeurs → Débogage sans fil → « Associer l'appareil avec un code », et tape ce code dans la notification de Shizuku.", running)
        step(4, "Dans Shizuku, touche « Démarrer ».", running)
        if (inst && !running) spaced(button("Ouvrir Shizuku", mod.color) { openShizuku() })
        step(5, "Autorise Pilote Suite à utiliser Shizuku.", ready)
        if (running && !ready) spaced(button("Autoriser Pilote Suite", mod.color) {
            runCatching { Shizuku.requestPermission(REQ_SHIZUKU) }.onFailure { status.text = "Shizuku ne répond pas : relance-le." }
        })
        step(6, if (installed) "Réinstalle Pilote (tes réglages sont gardés)." else "Installe Pilote.", false)
        if (active == null) spaced(button(if (installed) "Réinstaller Pilote pour Android Auto" else "Installer Pilote pour Android Auto",
            if (ready) mod.color else Color.parseColor("#3A4654")) {
            if (PlayMark.shizukuReady()) { prefs.edit().remove(KEY_NO_MARK).apply(); download(mod, info) }
            else status.text = "Termine d'abord les étapes 1 à 5 (Shizuku démarré et Pilote Suite autorisée)."
        })
        addView(text("Après un redémarrage du téléphone, rallume le débogage sans fil et relance Shizuku avant de mettre Pilote à jour. " +
            "Méthode du projet libre UnknownAuto (licence MIT). Google peut la bloquer à tout moment.", 12f, Color.parseColor("#7F8EA0")).also { it.setPadding(0, dp(10), 0, 0) })
        spaced(button("Fermer", Color.parseColor("#2A3442")) { showAutoGuide = false; render() })
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

    companion object {
        private const val ACTION_STATUS = "fr.roadguard.suite.INSTALL_STATUS"
        private const val GPS = "fr.roadguard.gps"
        private const val REQ_AUTO = 41
        private const val REQ_SHIZUKU = 42
        /** Sur ce téléphone, la marque est gardée quand c'est le shell (Shizuku) qui ouvre l'installateur (cas courant). */
        private const val KEY_SHELL_KEEPS = "shell_keeps_mark"
        /** Les deux chemins ont échoué sur ce téléphone : on ne redemande plus une deuxième confirmation à chaque mise à jour. */
        private const val KEY_NO_MARK = "no_mark"
    }
}
