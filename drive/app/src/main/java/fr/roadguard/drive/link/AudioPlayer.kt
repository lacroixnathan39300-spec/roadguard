package fr.roadguard.drive.link

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import fr.roadguard.link.LinkProtocol
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Son du téléphone joué par le poste (PCM 48 kHz stéréo). File courte : si la liaison prend du retard,
 * les blocs en trop sont jetés pour garder le son synchronisé (pas d'écho qui s'accumule).
 */
class AudioPlayer {
    private val queue = ArrayBlockingQueue<ByteArray>(12)
    @Volatile private var running = false
    private var thread: Thread? = null

    fun feed(pcm: ByteArray) {
        if (!running) start()
        if (!queue.offer(pcm)) { queue.poll(); queue.offer(pcm) }
    }

    @Synchronized
    private fun start() {
        if (running) return
        running = true
        thread = Thread({
            val min = AudioTrack.getMinBufferSize(LinkProtocol.AUDIO_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(LinkProtocol.AUDIO_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(min, LinkProtocol.AUDIO_RATE / 50 * 4 * 6))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            try {
                track.play()
                while (running) {
                    val chunk = queue.poll(1, TimeUnit.SECONDS) ?: continue
                    track.write(chunk, 0, chunk.size)
                }
            } catch (_: InterruptedException) {
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }, "roadguard-audio-out").apply { start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
        queue.clear()
    }
}
