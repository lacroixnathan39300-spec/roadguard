package fr.roadguard.drive.link

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import fr.roadguard.link.LinkProtocol
import java.nio.ByteBuffer

/**
 * Décodage matériel H.264 de l'écran du téléphone, affiché directement sur une surface.
 * Démarre à la première configuration reçue ; ignore les images reçues avant.
 */
class VideoDecoder(private val surface: Surface, private val width: Int, private val height: Int) {
    private var codec: MediaCodec? = null
    private val info = MediaCodec.BufferInfo()
    @Volatile var started = false; private set

    @Synchronized
    fun feed(type: Int, data: ByteArray) {
        if (type == LinkProtocol.BIN_VIDEO_CONFIG) {
            if (!started) start(data)
            return
        }
        val c = codec ?: return
        runCatching {
            val index = c.dequeueInputBuffer(20_000)
            if (index >= 0) {
                c.getInputBuffer(index)?.apply { clear(); put(data) }
                c.queueInputBuffer(index, 0, data.size, System.nanoTime() / 1000, 0)
            }
        }
    }

    private fun start(config: ByteArray) {
        runCatching {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setByteBuffer("csd-0", ByteBuffer.wrap(config))
            val c = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            // Faible latence (pris en compte par les décodeurs qui le gèrent).
            format.setInteger("low-latency", 1)
            format.setInteger(MediaFormat.KEY_PRIORITY, 0)
            c.configure(format, surface, null, 0)
            c.start()
            codec = c
            started = true
            // Les images décodées sont affichées dès qu'elles sortent, sans attendre la suivante.
            Thread({
                val i = MediaCodec.BufferInfo()
                while (codec === c) {
                    val out = try { c.dequeueOutputBuffer(i, 10_000) } catch (e: Exception) { break }
                    if (out >= 0) runCatching { c.releaseOutputBuffer(out, true) }
                }
            }, "roadguard-video-out").apply { isDaemon = true; start() }
        }
    }

    private fun drain(c: MediaCodec) {
        while (true) {
            val out = c.dequeueOutputBuffer(info, 0)
            if (out >= 0) c.releaseOutputBuffer(out, true) else if (out == MediaCodec.INFO_TRY_AGAIN_LATER) return
        }
    }

    @Synchronized
    fun release() {
        started = false
        codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
        codec = null
    }
}
