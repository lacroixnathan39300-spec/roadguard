package fr.roadguard.drive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors
import kotlinx.coroutines.delay
import java.io.File

/** Fond d'écran et logo de démarrage choisis par l'utilisateur (images copiées dans l'appli). */
object Personalize {
    fun wallpaperFile(c: Context) = File(c.filesDir, "wallpaper.jpg")
    fun logoFile(c: Context) = File(c.filesDir, "boot_logo.png")

    /** Copie une image choisie, réduite pour la mémoire du poste (côté long ≤ [maxSide]). */
    fun importImage(c: Context, uri: Uri, target: File, maxSide: Int): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2
        val bmp = c.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        target.outputStream().use { out.compress(if (target.name.endsWith(".png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 88, it) }
        true
    }.getOrDefault(false)

    fun load(file: File): Bitmap? = if (file.isFile) runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() else null
}

/** Fond d'écran derrière l'accueil (assombri pour que les blocs restent lisibles). */
@Composable
fun WallpaperBackground(version: Int) {
    val context = LocalContext.current
    val bmp = remember(version) { Personalize.load(Personalize.wallpaperFile(context)) } ?: return
    Box(Modifier.fillMaxSize()) {
        Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
    }
}

/** Écran de démarrage : logo choisi (ou ROADGUARD) pendant 2,5 s, puis fondu. */
@Composable
fun BootSplash() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(true) }
    val logo = remember { Personalize.load(Personalize.logoFile(context)) }
    LaunchedEffect(Unit) { delay(2500); visible = false }
    AnimatedVisibility(visible, exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (logo != null) Image(logo.asImageBitmap(), null, Modifier.fillMaxSize(0.6f), contentScale = ContentScale.Fit)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pilote Drive", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
                Text("DRIVE", color = DriveColors.Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Réglages : fond d'écran et logo de démarrage. */
@Composable
fun PersonalizeScreen(onChanged: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val pickWallpaper = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            message = if (Personalize.importImage(context, uri, Personalize.wallpaperFile(context), 1600)) "Fond d'écran appliqué" else "Image illisible"
            refresh++; onChanged()
        }
    }
    val pickLogo = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            message = if (Personalize.importImage(context, uri, Personalize.logoFile(context), 900)) "Logo de démarrage enregistré" else "Image illisible"
            refresh++
        }
    }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fond d'écran et logo", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Text("Fond d'écran de l'accueil", color = DriveColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Choisir une image", DriveColors.Accent) { runCatching { pickWallpaper.launch(arrayOf("image/*")) } }
            if (remember(refresh) { Personalize.wallpaperFile(context).isFile }) BigButton("Retirer", Color(0xFF7F1D1D)) {
                Personalize.wallpaperFile(context).delete(); refresh++; onChanged()
            }
        }
        Text("Logo au démarrage (par exemple celui de ta voiture)", color = DriveColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Choisir une image", DriveColors.Accent) { runCatching { pickLogo.launch(arrayOf("image/*")) } }
            if (remember(refresh) { Personalize.logoFile(context).isFile }) BigButton("Retirer", Color(0xFF7F1D1D)) {
                Personalize.logoFile(context).delete(); refresh++
            }
        }
        remember(refresh) { Personalize.load(Personalize.logoFile(context)) }?.let {
            Image(it.asImageBitmap(), null, Modifier.size(140.dp), contentScale = ContentScale.Fit)
        }
        message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 15.sp) }
    }
}
