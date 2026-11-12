package fr.roadguard.suite

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import rikka.shizuku.Shizuku

/**
 * Installation de Pilote « marquée Play Store », pour qu'Android Auto l'affiche.
 *
 * Android Auto n'affiche une appli de navigation (bibliothèque « Android for Cars ») que si le téléphone la croit
 * installée par le Play Store : le réglage « Sources inconnues » d'Android Auto ne couvre pas ces applis. La marque
 * n'est pas dans l'APK, c'est Android qui la note à l'installation. Pour l'obtenir sans Play Store, l'installateur
 * d'Android doit être ouvert avec « installateur = com.android.vending », ce qu'il n'accepte que d'un appelant
 * privilégié : le shell (comme adb), que l'appli Shizuku prête à Pilote Suite.
 *
 * Méthode et vérification reprises d'UnknownAuto (https://github.com/GitMag/UnknownAuto), licence MIT,
 * Copyright (c) 2026 GitMag. Voir suite/LICENCES-TIERS.md.
 */
object PlayMark {
    const val VENDING = "com.android.vending"
    const val SHIZUKU = "moe.shizuku.privileged.api"
    /** Installateur du système des téléphones Google (le seul initiateur qu'Android Auto accepte, avec le Play Store). */
    const val GOOGLE_INSTALLER = "com.google.android.packageinstaller"
    /** L'appli Android Auto du téléphone. */
    const val ANDROID_AUTO = "com.google.android.projection.gearhead"
    private const val MIME = "application/vnd.android.package-archive"
    private val main = Handler(Looper.getMainLooper())

    /**
     * Le contrôle qu'Android Auto applique à l'origine d'une appli (analyse de son validateur, version 17.7 :
     * github.com/AndreyPavlenko/Fermata/issues/846). À partir d'Android 11, il lit d'abord l'initiateur ; l'installateur
     * noté ne compte que si l'initiateur est l'installateur de Google, et il doit alors être le Play Store.
     * Plus strict que le contrôle d'UnknownAuto (qui accepte tout « packageinstaller »).
     */
    fun accepts(initiating: String?, installer: String?, sdk: Int): Boolean {
        if (sdk < 30) return installer == VENDING
        return initiating == VENDING || (initiating == GOOGLE_INSTALLER && installer == VENDING)
    }

    /** Origine de l'installation de [pkg] notée par Android : (initiateur, installateur) ; initiateur null avant Android 11. */
    fun source(c: Context, pkg: String): Pair<String?, String?> = runCatching<Pair<String?, String?>> {
        if (Build.VERSION.SDK_INT >= 30) {
            val info = c.packageManager.getInstallSourceInfo(pkg)
            Pair(info.initiatingPackageName, info.installingPackageName)
        } else legacySource(c, pkg)
    }.getOrDefault(Pair(null, null))

    @Suppress("DEPRECATION")
    private fun legacySource(c: Context, pkg: String): Pair<String?, String?> = Pair(null, c.packageManager.getInstallerPackageName(pkg))

    /** L'appli [pkg] installée porte-t-elle la marque qu'Android Auto accepte ? */
    fun marked(c: Context, pkg: String): Boolean = source(c, pkg).let { (initiating, installer) -> accepts(initiating, installer, Build.VERSION.SDK_INT) }

    private fun installed(c: Context, pkg: String) = runCatching { c.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    // ---------- Shizuku ----------
    fun shizukuInstalled(c: Context) = installed(c, SHIZUKU)
    fun shizukuRunning(): Boolean = runCatching { Shizuku.pingBinder() && !Shizuku.isPreV11() }.getOrDefault(false)
    fun shizukuGranted(): Boolean = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    /** Shizuku démarré et Pilote Suite autorisée : l'installation spéciale est possible. */
    fun shizukuReady(): Boolean = shizukuRunning() && shizukuGranted()

    // ---------- Installateur d'Android ----------
    /** Paquet de l'installateur du système (Google sur la plupart des téléphones). */
    fun installerPackage(c: Context): String =
        listOf(GOOGLE_INSTALLER, "com.android.packageinstaller").firstOrNull { installed(c, it) } ?: GOOGLE_INSTALLER

    /** L'URI de l'APK doit être lisible par l'installateur (le shell, lui, ne peut pas donner ce droit). */
    fun grantToInstallers(c: Context, uri: Uri) {
        for (p in listOf(GOOGLE_INSTALLER, "com.android.packageinstaller", VENDING)) {
            runCatching { c.grantUriPermission(p, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    /**
     * Installation ouverte par Pilote Suite elle-même, avec la marque demandée. Sur la plupart des téléphones,
     * l'installateur ignore la marque (installation normale) ; certains Android 17 la gardent justement ainsi.
     */
    fun appIntent(uri: Uri, target: String): Intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(uri, MIME)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        putExtra(Intent.EXTRA_RETURN_RESULT, true)
        putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", VENDING)
        putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", VENDING)
        putExtra("android.intent.extra.INSTALL_REASON", 1)
        putExtra(Intent.EXTRA_REFERRER_NAME, "android-app://$VENDING")
        putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://$VENDING"))
        putExtra("android.intent.extra.ORIGINATING_URI", Uri.parse("https://play.google.com/store/apps/details?id=$target"))
        if (Build.VERSION.SDK_INT >= 34) putExtra("android.content.pm.extra.REQUEST_UPDATE_OWNERSHIP", true)
    }

    /** Même ouverture, par le shell, pour un résultat (Android 14 et avant). Pas de drapeau de droit : le shell ne peut pas le donner. */
    fun shellIntent(uri: Uri, installerPkg: String): Intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        component = ComponentName(installerPkg, "com.android.packageinstaller.InstallStart")
        setDataAndType(uri, MIME)
        putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", VENDING)
        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        putExtra("android.intent.extra.INSTALL_REASON", 1)
        putExtra(Intent.EXTRA_REFERRER_NAME, "android-app://$VENDING")
        putExtra(Intent.EXTRA_RETURN_RESULT, true)
    }

    /** Commande que le shell lance (Android 15 et plus) : l'installateur du système sur notre fichier, marqué Play Store. */
    fun shellCommand(uri: Uri, installerPkg: String): Array<String> = arrayOf(
        "/system/bin/am", "start",
        "-n", "$installerPkg/com.android.packageinstaller.InstallStart",
        "-a", "android.intent.action.INSTALL_PACKAGE",
        "-d", uri.toString(),
        "-t", MIME,
        "--es", "android.intent.extra.INSTALLER_PACKAGE_NAME", VENDING,
        "--ez", "android.intent.extra.NOT_UNKNOWN_SOURCE", "true",
        "--ei", "android.intent.extra.INSTALL_REASON", "1",
        "--es", "android.intent.extra.REFERRER_NAME", "android-app://$VENDING",
    )

    /**
     * Relie le service « shell » par Shizuku, y fait un appel, puis le relâche ; [done] reçoit le résultat sur le fil
     * principal (null si l'appel a échoué ou si le service n'a pas pu être relié).
     */
    /**
     * Lance une commande sous l'identité « shell » directement via Shizuku (Shizuku.newProcess), SANS notre propre
     * service utilisateur. Important sur HyperOS/MIUI, qui bloque souvent le processus d'un service utilisateur
     * (bindUserService) mais laisse passer newProcess. Rend la sortie (+ « [exit N] » si échec), ou null si Shizuku
     * n'a pas pu lancer la commande. À appeler hors du fil principal (bloquant).
     */
    fun shizukuExec(cmd: Array<String>): String? {
        return try {
            if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return null
            val m = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            val proc = m.invoke(null, cmd, null, null) as Process
            val out = proc.inputStream.bufferedReader().readText()
            val err = proc.errorStream.bufferedReader().readText()
            val code = proc.waitFor()
            val text = (out + err).trim()
            when {
                code == 0 && text.isEmpty() -> "ok"
                code == 0 -> text
                else -> (if (text.isEmpty()) "" else "$text ") + "[exit $code]"
            }
        } catch (t: Throwable) {
            "newProcess erreur: $t"
        }
    }

    /**
     * Réécrit l'installateur noté de [pkg] vers le Play Store par Shizuku, sans service utilisateur (bloqué par
     * HyperOS). Essaie deux voies et rend un compte rendu détaillé (jamais null) pour le diagnostic :
     *  1) appel binder direct au gestionnaire de paquets via ShizukuBinderWrapper (ne lance aucun processus) ;
     *  2) commande `pm set-installer` via Shizuku.newProcess.
     */
    fun setInstallerViaShizuku(pkg: String): String {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return "Shizuku non relié (pingBinder=false)"
        val a = runCatching { setInstallerDirect(pkg) }.getOrElse { "binder exception: $it" }
        if (marked2(pkg)) return "ok (binder)"
        val b = shizukuExec(arrayOf("/system/bin/sh", "-c",
            "pm set-installer $pkg $VENDING 2>&1 || cmd package set-installer $pkg $VENDING 2>&1")) ?: "newProcess: pas de réponse"
        if (marked2(pkg)) return "ok (pm)"
        return "$a | $b"
    }

    private var appCtx: Context? = null
    fun init(c: Context) { appCtx = c.applicationContext }
    private fun marked2(pkg: String): Boolean { val c = appCtx ?: return false; return marked(c, pkg) }

    /** Appel binder direct (identité shell via Shizuku) à IPackageManager.setInstallerPackageName. Rend un compte rendu. */
    private fun setInstallerDirect(pkg: String): String {
        return try {
            val binder = rikka.shizuku.SystemServiceHelper.getSystemService("package")
                ?: return "service 'package' introuvable"
            val wrapped = rikka.shizuku.ShizukuBinderWrapper(binder)
            val pm = Class.forName("android.content.pm.IPackageManager\$Stub")
                .getMethod("asInterface", IBinder::class.java).invoke(null, wrapped)
            pm.javaClass.getMethod("setInstallerPackageName", String::class.java, String::class.java).invoke(pm, pkg, VENDING)
            "binder ok"
        } catch (e: java.lang.reflect.InvocationTargetException) {
            "binder refusé: " + (e.cause?.toString() ?: e.toString())
        } catch (t: Throwable) {
            "binder erreur: $t"
        }
    }

    fun withShell(c: Context, call: (IShellService) -> String?, done: (String?) -> Unit) {
        // Sécurité : si le service ne se relie pas (Shizuku arrêté, autorisation absente, processus qui ne démarre pas),
        // on ne reste pas bloqué — [done] est appelé une seule fois, avec null, au bout du délai.
        val settled = java.util.concurrent.atomic.AtomicBoolean(false)
        fun finish(out: String?) { if (settled.compareAndSet(false, true)) main.post { done(out) } }
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) { finish(null); return }
        val args = Shizuku.UserServiceArgs(ComponentName(c.packageName, ShellService::class.java.name))
            .daemon(false).processNameSuffix("shell").debuggable(false).version(1)
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val self = this
                Thread {
                    val out = runCatching { call(IShellService.Stub.asInterface(binder)) }.getOrNull()
                    finish(out)
                    runCatching { Shizuku.unbindUserService(args, self, true) }
                }.start()
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
        }
        runCatching { Shizuku.bindUserService(args, conn) }.onFailure { finish(null) }
        main.postDelayed({ finish(null) }, 12_000)
    }
}
