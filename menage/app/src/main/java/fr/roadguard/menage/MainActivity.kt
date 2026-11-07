package fr.roadguard.menage

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * Ménage : trouve ce qui prend de la place pour rien sur le téléphone et le met à la corbeille d'Android
 * (récupérable 30 jours). Rien n'est supprimé sans que l'on ait coché puis confirmé.
 *
 * Catégories : doublons (contenu identique), gros fichiers, rafales de photos, captures d'écran,
 * médias des messageries, vieux téléchargements, installateurs APK.
 */
class MainActivity : Activity() {

    // ---------- Données ----------

    enum class Kind { IMAGE, VIDEO, AUDIO, OTHER }

    class Item(
        val id: Long, val uri: Uri, val name: String, val size: Long, val modified: Long, val taken: Long,
        val kind: Kind, val path: String, val mime: String,
    ) {
        /** Date de la photo (prise de vue), sinon de modification, en millisecondes. */
        val time: Long get() = if (taken > 0) taken else modified
    }

    /** Une catégorie : ses fichiers, ceux cochés d'office et, pour les doublons et rafales, le numéro de groupe. */
    data class Category(
        val emoji: String, val title: String, val hint: String, val items: List<Item>,
        val preselected: Set<Long>, val groups: Map<Long, Int> = emptyMap(),
    ) {
        val bytes: Long get() = items.sumOf { it.size }
    }

    private val io = Executors.newSingleThreadExecutor()
    private val thumbPool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    private var cats: List<Category> = emptyList()
    private var scanning = false
    private var autoScanned = false
    private var scanStatus = ""
    private var freedTotal = 0L
    private var current: Category? = null
    private val selected = HashSet<Long>()

    // ---------- Écran ----------

    private val bg = Color.parseColor("#0B0F15")
    private val panel = Color.parseColor("#161C25")
    private val green = Color.parseColor("#1DB36B")
    private val red = Color.parseColor("#E53935")
    private val grey = Color.parseColor("#9FB0C3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (current == null) showHome()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        showHome()
    }

    // Retour : de la catégorie vers l'accueil (Android 13+ : OnBackInvokedCallback ; avant : onBackPressed).
    private var backRegistered = false
    private val backCallback: Any? by lazy {
        if (Build.VERSION.SDK_INT >= 33) android.window.OnBackInvokedCallback { showHome() } else null
    }

    private fun setBackToHome(on: Boolean) {
        if (Build.VERSION.SDK_INT < 33 || on == backRegistered) return
        val cb = backCallback as android.window.OnBackInvokedCallback
        if (on) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb)
        else onBackInvokedDispatcher.unregisterOnBackInvokedCallback(cb)
        backRegistered = on
    }

    @Deprecated("Android 12 et avant")
    override fun onBackPressed() {
        if (current != null) showHome() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    // ---------- Autorisations ----------

    private fun mediaPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun granted(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    private fun hasFullMedia() = mediaPermissions().any(::granted)

    /** Android 14 : l'utilisateur n'a autorisé que quelques photos. */
    private fun hasPartialMedia() = Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    private fun hasAllFiles() = Environment.isExternalStorageManager()

    // ---------- Accueil ----------

    private fun showHome() {
        current = null
        selected.clear()
        setBackToHome(false)
        val root = column().apply { setPadding(dp(20), dp(28), dp(20), dp(28)) }
        root.addView(text("🧹 Ménage", 30f, Color.WHITE, bold = true))
        root.addView(text("Trouve ce qui prend de la place pour rien. Tu choisis, tout part à la corbeille (récupérable 30 jours).", 15f, grey))
        root.addView(storageLine())
        if (freedTotal > 0) root.addView(text("✔ ${human(freedTotal)} libérés pendant cette séance", 15f, green, bold = true).also { it.setPadding(0, dp(6), 0, 0) })

        val canRead = hasFullMedia() || hasPartialMedia() || hasAllFiles()
        if (!canRead) {
            root.addView(card(Color.parseColor("#B26A00")).apply {
                addView(text("Autorisation nécessaire", 18f, Color.WHITE, bold = true))
                addView(text("Pour regarder tes photos, vidéos et fichiers, Ménage doit y avoir accès. Rien ne quitte le téléphone.", 15f, Color.WHITE))
                addView(button("Autoriser", Color.parseColor("#33000000")) { requestPermissions(mediaPermissions(), 1) })
            })
        } else if (hasPartialMedia() && !hasFullMedia() && !hasAllFiles()) {
            root.addView(card(Color.parseColor("#B26A00")).apply {
                addView(text("Accès partiel", 18f, Color.WHITE, bold = true))
                addView(text("Seules les photos que tu as choisies sont visibles. Pour tout analyser, choisis « Tout autoriser ».", 15f, Color.WHITE))
                addView(button("Tout autoriser", Color.parseColor("#33000000")) { requestPermissions(mediaPermissions(), 1) })
            })
        }
        if (canRead && !hasAllFiles()) {
            root.addView(card(panel).apply {
                addView(text("Documents et APK (facultatif)", 17f, Color.WHITE, bold = true))
                addView(text("Pour voir aussi les PDF, documents et installateurs APK téléchargés, autorise « l'accès à tous les fichiers ».", 14f, grey))
                addView(button("Autoriser l'accès à tous les fichiers", Color.parseColor("#2A3442")) {
                    runCatching { startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))) }
                        .onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
                })
            })
        }

        if (canRead) {
            if (scanning) {
                root.addView(card(panel).apply { addView(text("🔍 $scanStatus", 16f, Color.WHITE)) })
            } else {
                root.addView(button(if (cats.isEmpty()) "🔍 Analyser le téléphone" else "🔄 Analyser de nouveau", green) { scan() }
                    .also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = dp(14) })
                if (cats.isEmpty() && !autoScanned) { autoScanned = true; scan() } // première ouverture : analyse tout de suite
            }
        }

        if (cats.isNotEmpty() && !scanning) {
            val total = cats.sumOf { c -> c.items.filter { it.id in c.preselected }.sumOf { it.size } }
            root.addView(text(if (total > 0) "Déjà repéré comme inutile : ${human(total)} (cochés d'office, à vérifier)" else "Résultats :",
                15f, Color.WHITE, bold = true).also { it.setPadding(0, dp(16), 0, 0) })
            cats.forEach { c -> root.addView(categoryCard(c)) }
            root.addView(text("Appuie sur une catégorie pour voir les fichiers. Les fichiers mis à la corbeille se récupèrent pendant 30 jours " +
                "(appli Galerie ou Photos → Corbeille).", 13f, Color.parseColor("#7F8EA0")).also { it.setPadding(0, dp(14), 0, 0) })
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(bg); addView(root) })
    }

    private fun storageLine(): View {
        val s = runCatching { StatFs(Environment.getExternalStorageDirectory().path) }.getOrNull()
        val t = if (s == null) "" else {
            val total = s.totalBytes; val free = s.availableBytes
            "Stockage : ${human(total - free)} utilisés sur ${human(total)} · ${human(free)} libres"
        }
        return text(t, 14f, grey).also { it.setPadding(0, dp(10), 0, 0) }
    }

    private fun categoryCard(c: Category): View = card(panel).apply {
        val pre = c.items.filter { it.id in c.preselected }
        addView(text("${c.emoji}  ${c.title}", 19f, Color.WHITE, bold = true))
        addView(text(if (c.items.isEmpty()) "Rien à nettoyer ✓" else "${c.items.size} fichiers · ${human(c.bytes)}" +
            if (pre.isNotEmpty()) " · ${pre.size} cochés d'office (${human(pre.sumOf { it.size })})" else "", 15f, if (c.items.isEmpty()) green else grey))
        if (c.items.isNotEmpty()) setOnClickListener { showCategory(c) }
    }

    // ---------- Analyse ----------

    private fun scan() {
        if (scanning) return
        scanning = true; scanStatus = "Analyse en cours…"
        showHome()
        io.execute {
            val result = runCatching {
                val items = readAll { n -> status("Analyse : $n fichiers lus…") }
                buildCategories(items)
            }
            main.post {
                scanning = false
                result.onSuccess { cats = it }.onFailure { toast("Analyse impossible : ${it.message}") }
                if (current == null) showHome()
            }
        }
    }

    private fun status(s: String) = main.post {
        scanStatus = s
        if (current == null && scanning) showHome()
    }

    /** Tous les fichiers visibles : photos, vidéos et sons (et documents si « tous les fichiers » est autorisé). */
    private fun readAll(progress: (Int) -> Unit): List<Item> {
        val out = ArrayList<Item>()
        val cols = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.DATE_TAKEN, MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.VOLUME_NAME,
        )
        val where = "${MediaStore.MediaColumns.SIZE} > 0 AND ${MediaStore.MediaColumns.MIME_TYPE} IS NOT NULL"
        contentResolver.query(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), cols, where, null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(cols[0]); val iName = c.getColumnIndexOrThrow(cols[1]); val iSize = c.getColumnIndexOrThrow(cols[2])
            val iMod = c.getColumnIndexOrThrow(cols[3]); val iTaken = c.getColumnIndexOrThrow(cols[4]); val iType = c.getColumnIndexOrThrow(cols[5])
            val iPath = c.getColumnIndexOrThrow(cols[6]); val iMime = c.getColumnIndexOrThrow(cols[7]); val iVol = c.getColumnIndexOrThrow(cols[8])
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val volume = c.getString(iVol) ?: MediaStore.VOLUME_EXTERNAL
                val kind = when (c.getInt(iType)) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> Kind.IMAGE
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> Kind.VIDEO
                    MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> Kind.AUDIO
                    else -> Kind.OTHER
                }
                // Adresse propre au type (indispensable pour la corbeille d'Android).
                val base = when (kind) {
                    Kind.IMAGE -> MediaStore.Images.Media.getContentUri(volume)
                    Kind.VIDEO -> MediaStore.Video.Media.getContentUri(volume)
                    Kind.AUDIO -> MediaStore.Audio.Media.getContentUri(volume)
                    Kind.OTHER -> MediaStore.Files.getContentUri(volume)
                }
                out.add(Item(
                    id = id, uri = ContentUris.withAppendedId(base, id), name = c.getString(iName) ?: "?",
                    size = c.getLong(iSize), modified = c.getLong(iMod) * 1000, taken = if (c.isNull(iTaken)) 0 else c.getLong(iTaken),
                    kind = kind, path = c.getString(iPath) ?: "", mime = c.getString(iMime) ?: "",
                ))
                if (out.size % 500 == 0) progress(out.size)
            }
        }
        return out
    }

    private fun buildCategories(items: List<Item>): List<Category> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val result = ArrayList<Category>()
        var group = 0

        // Doublons : même taille, puis même contenu (empreinte). On garde l'original (appareil photo, puis le plus ancien).
        val dupItems = ArrayList<Item>(); val dupPre = HashSet<Long>(); val dupGroups = HashMap<Long, Int>()
        val sameSize = items.filter { it.size >= 10_240 }.groupBy { it.size }.values.filter { it.size > 1 }
        sameSize.forEachIndexed { i, list ->
            if (i % 10 == 0) status("Recherche des doublons… ${i * 100 / sameSize.size.coerceAtLeast(1)} %")
            list.groupBy { fingerprint(it) }.forEach { (fp, same) ->
                if (fp == null || same.size < 2) return@forEach
                group++
                val ordered = same.sortedWith(compareBy<Item>({ if (it.path.startsWith("DCIM", ignoreCase = true)) 0 else 1 }, { it.time }))
                ordered.forEachIndexed { k, x -> dupItems.add(x); dupGroups[x.id] = group; if (k > 0) dupPre.add(x.id) }
            }
        }
        result.add(Category("👯", "Doublons", "Fichiers identiques. Le premier de chaque groupe (l'original) est gardé, les copies sont cochées.",
            dupItems, dupPre, dupGroups))

        // Gros fichiers.
        val big = items.filter { it.size >= 50L * 1_048_576 }.sortedByDescending { it.size }
        result.add(Category("🐘", "Gros fichiers (plus de 50 Mo)", "Du plus gros au plus petit. Rien n'est coché : choisis ce dont tu n'as plus besoin.", big, emptySet()))

        // Rafales : photos prises à moins de 2 secondes d'intervalle dans le même dossier. On garde la plus lourde (la plus détaillée).
        status("Recherche des rafales…")
        val burstItems = ArrayList<Item>(); val burstPre = HashSet<Long>(); val burstGroups = HashMap<Long, Int>()
        var run = ArrayList<Item>()
        fun flush() {
            if (run.size >= 2) {
                group++
                val keep = run.maxByOrNull { it.size }
                run.forEach { burstItems.add(it); burstGroups[it.id] = group; if (it !== keep) burstPre.add(it.id) }
            }
            run = ArrayList()
        }
        val photos = items.filter { it.kind == Kind.IMAGE && it.time > 0 && !isScreenshot(it) }.sortedBy { it.time }
        for (p in photos) {
            val last = run.lastOrNull()
            if (last != null && (p.time - last.time > 2_000 || p.path != last.path)) flush()
            run.add(p)
        }
        flush()
        result.add(Category("📸", "Rafales et photos en série", "Photos prises à la suite. La plus nette (la plus lourde) de chaque série est gardée.",
            burstItems, burstPre, burstGroups))

        // Captures d'écran : cochées d'office après 30 jours.
        val shots = items.filter(::isScreenshot).sortedBy { it.time }
        result.add(Category("📱", "Captures d'écran", "Les captures de plus de 30 jours sont cochées.", shots,
            shots.filter { now - it.time > 30 * day }.map { it.id }.toSet()))

        // Messageries.
        val apps = listOf("whatsapp", "telegram", "messenger", "signal", "snapchat", "instagram")
        val chat = items.filter { i -> apps.any { i.path.contains(it, ignoreCase = true) } }.sortedByDescending { it.size }
        result.add(Category("💬", "Reçus sur WhatsApp, Telegram…", "Photos, vidéos et vocaux des messageries, du plus gros au plus petit.", chat, emptySet()))

        // Vieux téléchargements.
        val oldDl = items.filter { it.path.startsWith("Download", ignoreCase = true) && now - it.modified > 90 * day && !isApk(it) }
            .sortedByDescending { it.size }
        result.add(Category("📥", "Téléchargements de plus de 3 mois", "Fichiers du dossier Téléchargements non modifiés depuis 3 mois.", oldDl, emptySet()))

        // Installateurs APK : inutiles une fois l'appli installée.
        val apks = items.filter(::isApk).sortedByDescending { it.size }
        result.add(Category("📦", "Installateurs d'applis (APK)", "Une fois l'appli installée, le fichier .apk ne sert plus : tous cochés.", apks, apks.map { it.id }.toSet()))

        return result
    }

    private fun isScreenshot(i: Item) = i.path.contains("screenshot", ignoreCase = true) || i.name.startsWith("Screenshot", ignoreCase = true)
    private fun isApk(i: Item) = i.mime == "application/vnd.android.package-archive" || i.name.endsWith(".apk", ignoreCase = true)

    /** Empreinte du contenu : tout le fichier jusqu'à 8 Mo, sinon le début et la fin (1 Mo chacun). */
    private fun fingerprint(item: Item): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-1")
        val buf = ByteArray(65_536)
        val input = contentResolver.openInputStream(item.uri) ?: return@runCatching null
        input.use {
            if (item.size <= 8L * 1_048_576) readUpTo(it, md, buf, Long.MAX_VALUE)
            else {
                readUpTo(it, md, buf, 1_048_576)
                skipFully(it, item.size - 2L * 1_048_576)
                readUpTo(it, md, buf, 1_048_576)
            }
        }
        md.digest().joinToString("") { b -> "%02x".format(b) }
    }.getOrNull()

    private fun readUpTo(input: InputStream, md: MessageDigest, buf: ByteArray, max: Long) {
        var left = max
        while (left > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (n <= 0) return
            md.update(buf, 0, n)
            left -= n
        }
    }

    private fun skipFully(input: InputStream, count: Long) {
        var left = count
        while (left > 0) {
            val n = input.skip(left)
            if (n > 0) left -= n else { if (input.read() < 0) return; left-- }
        }
    }

    // ---------- Catégorie : grille de vignettes ----------

    private var adapter: GridAdapter? = null
    private lateinit var trashButton: Button

    private fun showCategory(c: Category) {
        current = c
        selected.clear(); selected.addAll(c.preselected)
        setBackToHome(true)
        val root = column().apply { setPadding(dp(10), dp(18), dp(10), dp(10)); setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(button("◂", Color.parseColor("#2A3442")) { showHome() })
        top.addView(text("${c.emoji} ${c.title}", 19f, Color.WHITE, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        root.addView(top)
        root.addView(text(c.hint + " Appui court : cocher. Appui long : ouvrir.", 13f, grey).also { it.setPadding(dp(4), dp(6), dp(4), dp(6)) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Tout cocher", Color.parseColor("#2A3442")) { selected.addAll(c.items.map { it.id }); refreshSelection() })
        actions.addView(button("Tout décocher", Color.parseColor("#2A3442")) { selected.clear(); refreshSelection() })
        root.addView(actions)

        val grid = GridView(this).apply {
            numColumns = 3
            horizontalSpacing = dp(4); verticalSpacing = dp(4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) }
        }
        val a = GridAdapter(c.items, c.groups)
        adapter = a
        grid.adapter = a
        grid.setOnItemClickListener { _, _, pos, _ ->
            val id = c.items[pos].id
            if (!selected.remove(id)) selected.add(id)
            refreshSelection()
        }
        grid.setOnItemLongClickListener { _, _, pos, _ -> open(c.items[pos]); true }
        root.addView(grid)

        trashButton = button("", red) { confirmTrash() }.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
            textSize = 17f
        }
        root.addView(trashButton)
        setContentView(root)
        refreshSelection()
    }

    private fun refreshSelection() {
        val c = current ?: return
        val chosen = c.items.filter { it.id in selected }
        trashButton.text = if (chosen.isEmpty()) "Coche des fichiers à mettre à la corbeille"
            else "🗑 Mettre à la corbeille : ${chosen.size} · ${human(chosen.sumOf { it.size })}"
        trashButton.isEnabled = chosen.isNotEmpty()
        trashButton.alpha = if (chosen.isEmpty()) 0.5f else 1f
        adapter?.notifyDataSetChanged()
    }

    private fun open(item: Item) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(item.uri, item.mime.ifBlank { "*/*" })
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }.onFailure { toast("Aucune appli pour ouvrir « ${item.name} »") }
    }

    private val cellSize: Int get() = (resources.displayMetrics.widthPixels - dp(20) - dp(8)) / 3

    private val thumbs = object : LruCache<Long, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private val loading = HashSet<Long>()
    private val boundCells = HashMap<Long, Cell>()

    private fun loadThumb(item: Item) {
        if (!loading.add(item.id)) return
        thumbPool.execute {
            val bmp = runCatching { contentResolver.loadThumbnail(item.uri, Size(256, 256), null) }.getOrNull()
            main.post {
                loading.remove(item.id)
                if (bmp != null) {
                    thumbs.put(item.id, bmp)
                    boundCells[item.id]?.takeIf { it.itemId == item.id }?.img?.setImageBitmap(bmp)
                }
            }
        }
    }

    private inner class GridAdapter(val items: List<Item>, val groups: Map<Long, Int>) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = items[position].id
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val cell = convertView as? Cell ?: Cell()
            cell.bind(items[position], groups[items[position].id])
            return cell
        }
    }

    /** Case de la grille : vignette, coche en haut à droite, taille (et groupe) en bas. */
    private inner class Cell : FrameLayout(this@MainActivity) {
        val img = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Color.parseColor("#1B2230")) }
        val icon = TextView(context).apply { textSize = 30f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
        val check = TextView(context).apply { textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD }
        val label = TextView(context).apply {
            textSize = 11f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#B3000000"))
            setPadding(dp(4), dp(2), dp(4), dp(2)); maxLines = 2
        }
        var itemId = -1L

        init {
            layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellSize)
            addView(img, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(icon, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(check, FrameLayout.LayoutParams(dp(30), dp(30), Gravity.TOP or Gravity.END).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
            addView(label, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        }

        fun bind(item: Item, group: Int?) {
            itemId = item.id
            boundCells[item.id] = this
            val on = item.id in selected
            check.text = if (on) "✓" else ""
            check.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (on) red else Color.parseColor("#66000000"))
                setStroke(dp(2), Color.WHITE)
            }
            foreground = if (on) GradientDrawable().apply { setStroke(dp(3), red); setColor(Color.parseColor("#33E53935")) } else null
            val media = item.kind == Kind.IMAGE || item.kind == Kind.VIDEO
            val head = when {
                group == null -> ""
                on -> "Groupe $group · "
                else -> "Groupe $group · gardé · "
            }
            label.text = head + human(item.size) + if (media) "" else "\n" + item.name
            if (media) {
                icon.text = if (item.kind == Kind.VIDEO) "▶" else ""
                val cached = thumbs.get(item.id)
                img.setImageBitmap(cached)
                if (cached == null) loadThumb(item)
            } else {
                img.setImageBitmap(null)
                icon.text = when { item.kind == Kind.AUDIO -> "🎵"; isApk(item) -> "📦"; else -> "📄" }
            }
        }
    }

    // ---------- Corbeille ----------

    private val trashQueue = ArrayDeque<List<Item>>()
    private var inFlight: List<Item> = emptyList()
    private val trashedNow = ArrayList<Item>()
    private var othersToDelete: List<Item> = emptyList()

    private fun confirmTrash() {
        val c = current ?: return
        val chosen = c.items.filter { it.id in selected }
        if (chosen.isEmpty()) return
        val media = chosen.filter { it.kind != Kind.OTHER }
        othersToDelete = chosen.filter { it.kind == Kind.OTHER }
        trashedNow.clear(); trashQueue.clear()
        media.chunked(300).forEach { trashQueue.addLast(it) } // Android confirme par lots
        if (trashQueue.isEmpty()) deleteOthers() else nextTrash()
    }

    private fun nextTrash() {
        val batch = trashQueue.removeFirstOrNull()
        if (batch == null) { deleteOthers(); return }
        runCatching {
            val pi = MediaStore.createTrashRequest(contentResolver, batch.map { it.uri }, true)
            inFlight = batch
            startIntentSenderForResult(pi.intentSender, REQ_TRASH, null, 0, 0, 0)
        }.onFailure { toast("Corbeille impossible : ${it.message}"); trashQueue.clear(); finishTrash() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_TRASH) return
        if (resultCode == RESULT_OK) { trashedNow.addAll(inFlight); nextTrash() }
        else { trashQueue.clear(); othersToDelete = emptyList(); finishTrash() }
    }

    /** Documents et APK : pas de corbeille d'Android pour eux, suppression définitive après confirmation. */
    private fun deleteOthers() {
        val others = othersToDelete
        if (others.isEmpty()) { finishTrash(); return }
        AlertDialog.Builder(this)
            .setTitle("Supprimer définitivement ?")
            .setMessage("${others.size} document(s) ou APK (${human(others.sumOf { it.size })}) n'ont pas de corbeille : ils seront supprimés pour de bon.")
            .setPositiveButton("Supprimer") { _, _ ->
                others.forEach { item -> if (runCatching { contentResolver.delete(item.uri, null, null) }.getOrDefault(0) > 0) trashedNow.add(item) }
                othersToDelete = emptyList(); finishTrash()
            }
            .setNegativeButton("Annuler") { _, _ -> othersToDelete = emptyList(); finishTrash() }
            .setOnCancelListener { othersToDelete = emptyList(); finishTrash() }
            .show()
    }

    private fun finishTrash() {
        if (trashedNow.isEmpty()) return
        val gone = trashedNow.map { it.id }.toHashSet()
        val bytes = trashedNow.sumOf { it.size }
        freedTotal += bytes
        trashedNow.clear()
        // Les fichiers partis disparaissent de toutes les catégories.
        cats = cats.map { c ->
            c.copy(items = c.items.filter { it.id !in gone }, preselected = c.preselected - gone, groups = c.groups.filterKeys { it !in gone })
        }
        toast("✔ ${human(bytes)} libérés")
        val c = current
        if (c != null) {
            val updated = cats.firstOrNull { it.title == c.title }
            if (updated != null && updated.items.isNotEmpty()) showCategory(updated) else showHome()
        } else showHome()
    }

    // ---------- Petits outils ----------

    private fun human(b: Long): String = when {
        b >= 1_073_741_824L -> "%.1f Go".format(b / 1_073_741_824.0)
        b >= 1_048_576L -> "%.0f Mo".format(b / 1_048_576.0)
        else -> "%.0f Ko".format(b / 1024.0)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun card(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(color) }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
    }

    private fun button(label: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); isAllCaps = false; textSize = 16f; gravity = Gravity.CENTER
        background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(color) }
        setPadding(dp(18), dp(10), dp(18), dp(10))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) }
        setOnClickListener { onClick() }
    }

    companion object { private const val REQ_TRASH = 7 }
}
