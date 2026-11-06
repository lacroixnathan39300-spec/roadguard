package fr.roadguard.drive.link

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Une connexion ouverte avec le téléphone (Wi-Fi ou USB). */
interface LinkTransport {
    val input: InputStream
    val output: OutputStream
    val label: String
    fun close()
}

/**
 * Liaison par câble USB avec le protocole « accessoire » ouvert d'Android (celui qu'utilise Android Auto
 * filaire) : le poste est l'hôte USB, il bascule le téléphone en mode accessoire, puis échange les données
 * sur deux canaux « bulk ». Le téléphone se recharge pendant ce temps.
 */
object UsbTransport {
    private const val GOOGLE_VID = 0x18D1
    private val ACCESSORY_PIDS = setOf(0x2D00, 0x2D01, 0x2D04, 0x2D05)
    private const val ACTION_PERMISSION = "fr.roadguard.drive.USB_PERMISSION"
    const val MANUFACTURER = "ROADGUARD"
    const val MODEL = "Link"

    private fun isAccessory(d: UsbDevice) = d.vendorId == GOOGLE_VID && d.productId in ACCESSORY_PIDS

    /** Un appareil USB qui ressemble à un téléphone (pas un hub, une clé ou une caméra) est-il branché ? */
    fun candidate(context: Context): UsbDevice? {
        val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usb.deviceList.values
        return devices.firstOrNull(::isAccessory) ?: devices.firstOrNull { d ->
            d.deviceClass != UsbConstants.USB_CLASS_HUB && d.deviceClass != UsbConstants.USB_CLASS_MASS_STORAGE &&
                (0 until d.interfaceCount).none { d.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE } &&
                // Téléphones Android : interface MTP/PTP (photo) ou « constructeur » (MTP, ADB).
                (0 until d.interfaceCount).any { d.getInterface(it).interfaceClass in setOf(UsbConstants.USB_CLASS_VENDOR_SPEC, UsbConstants.USB_CLASS_STILL_IMAGE) } &&
                key(d) !in rejected(context)
        }
    }

    private fun key(d: UsbDevice) = "%04x:%04x".format(d.vendorId, d.productId)

    /** Appareils USB qui ne sont pas des téléphones compatibles (module GPS interne…) : on ne les redemande plus. */
    private fun rejected(context: Context): Set<String> =
        context.getSharedPreferences("link", Context.MODE_PRIVATE).getStringSet("usb_rejected", emptySet()).orEmpty()

    private fun reject(context: Context, d: UsbDevice) {
        val prefs = context.getSharedPreferences("link", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("usb_rejected", rejected(context) + key(d)).apply()
    }

    /** Oublier les refus (bouton « Réessayer l'USB »). */
    fun resetRejected(context: Context) {
        context.getSharedPreferences("link", Context.MODE_PRIVATE).edit().remove("usb_rejected").apply()
    }

    /** Ouvre la liaison USB si un téléphone est branché, sinon null. */
    suspend fun open(context: Context): LinkTransport? {
        val usb = context.getSystemService(Context.USB_SERVICE) as UsbManager
        var device = candidate(context) ?: return null
        if (!isAccessory(device)) {
            if (!permission(context, usb, device)) { reject(context, device); return null }
            val conn = usb.openDevice(device) ?: return null
            try {
                if (!switchToAccessory(conn)) { reject(context, device); return null }
            } finally {
                conn.close()
            }
            // Le téléphone se reconnecte sous une nouvelle identité (mode accessoire).
            device = withTimeoutOrNull(6_000) {
                var found: UsbDevice? = null
                while (found == null) { delay(250); found = usb.deviceList.values.firstOrNull(::isAccessory) }
                found
            } ?: return null
        }
        if (!permission(context, usb, device)) return null
        val conn = usb.openDevice(device) ?: return null
        val iface: UsbInterface = device.getInterface(0)
        if (!conn.claimInterface(iface, true)) { conn.close(); return null }
        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
            if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep else outEp = ep
        }
        if (inEp == null || outEp == null) { conn.close(); return null }
        return BulkTransport(conn, iface, inEp, outEp)
    }

    /** Commandes 51/52/53 du protocole accessoire : version, identité, démarrage. */
    private fun switchToAccessory(conn: UsbDeviceConnection): Boolean {
        val version = ByteArray(2)
        val n = conn.controlTransfer(0xC0, 51, 0, 0, version, 2, 1000)
        if (n != 2 || (version[0].toInt() or (version[1].toInt() shl 8)) < 1) return false
        val strings = listOf(MANUFACTURER, MODEL, "Liaison ROADGUARD DRIVE", "1", "https://roadguard.local", "RG-DRIVE")
        strings.forEachIndexed { i, s ->
            val bytes = (s + "\u0000").toByteArray()
            if (conn.controlTransfer(0x40, 52, 0, i, bytes, bytes.size, 1000) < 0) return false
        }
        return conn.controlTransfer(0x40, 53, 0, 0, null, 0, 1000) >= 0
    }

    /** Autorisation USB demandée à l'utilisateur (une fois, avec « toujours » cochable). */
    private suspend fun permission(context: Context, usb: UsbManager, device: UsbDevice): Boolean {
        if (usb.hasPermission(device)) return true
        val result = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                result.complete(i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
            }
        }
        val filter = IntentFilter(ACTION_PERMISSION)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(receiver, filter)
        try {
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val intent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_PERMISSION).setPackage(context.packageName), flags)
            usb.requestPermission(device, intent)
            return withTimeoutOrNull(60_000) { result.await() } ?: false
        } finally {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private class BulkTransport(
        private val conn: UsbDeviceConnection,
        private val iface: UsbInterface,
        private val inEp: UsbEndpoint,
        private val outEp: UsbEndpoint,
    ) : LinkTransport {
        override val label = "câble USB"
        @Volatile private var closed = false

        override val input: InputStream = object : InputStream() {
            private val buf = ByteArray(16_384)
            private var pos = 0
            private var len = 0
            private fun fill(): Boolean {
                val since = System.currentTimeMillis()
                while (!closed) {
                    val n = conn.bulkTransfer(inEp, buf, buf.size, 1000)
                    if (n > 0) { pos = 0; len = n; return true }
                    // n < 0 : délai dépassé sans données (normal) ou câble débranché.
                    if (n < 0 && UsbTransport.unplugged(conn)) return false
                    // Téléphone muet depuis 20 s (appli arrêtée, câble branché) : comme en Wi-Fi, on relance la liaison
                    // au lieu d'attendre indéfiniment (le téléphone répond aux « ping » toutes les 5 s).
                    if (System.currentTimeMillis() - since > 20_000) throw IOException("Le téléphone ne répond plus (câble USB)")
                }
                return false
            }
            override fun read(): Int {
                if (pos >= len && !fill()) return -1
                return buf[pos++].toInt() and 0xff
            }
            override fun read(b: ByteArray, off: Int, l: Int): Int {
                if (l == 0) return 0
                if (pos >= len && !fill()) return -1
                val n = minOf(l, len - pos)
                System.arraycopy(buf, pos, b, off, n)
                pos += n
                return n
            }
        }

        override val output: OutputStream = object : OutputStream() {
            override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)
            override fun write(b: ByteArray, off: Int, l: Int) {
                var at = off
                val end = off + l
                while (at < end) {
                    val chunk = minOf(16_384, end - at)
                    // API 27 : pas de variante avec décalage, on copie le morceau.
                    val part = if (at == 0 && chunk == b.size) b else b.copyOfRange(at, at + chunk)
                    val n = conn.bulkTransfer(outEp, part, chunk, 3000)
                    if (n <= 0) throw IOException("Câble USB débranché")
                    at += n
                }
            }
        }

        override fun close() {
            closed = true
            runCatching { conn.releaseInterface(iface) }
            runCatching { conn.close() }
        }
    }

    /** Le descripteur ne répond plus : l'appareil a disparu. */
    private fun unplugged(conn: UsbDeviceConnection): Boolean = conn.rawDescriptors == null
}
