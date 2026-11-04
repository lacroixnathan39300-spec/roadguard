package fr.roadguard.link

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LinkProtocolTest {
    private class FakePeers(var code: String?, val known: MutableMap<String, ByteArray> = HashMap()) : LinkProtocol.Peers {
        override fun secretFor(id: String) = known[id]
        override fun pendingCode() = code
        override fun paired(id: String, name: String, secret: ByteArray) { known[id] = secret }
    }

    /** Relie un client et un serveur par deux tuyaux en mémoire. */
    private fun <C, S> connect(client: (java.io.InputStream, java.io.OutputStream) -> C,
                               server: (java.io.InputStream, java.io.OutputStream) -> S): Pair<Result<C>, Result<S>> {
        val c2s = PipedOutputStream(); val sIn = PipedInputStream(c2s, 1 shl 20)
        val s2c = PipedOutputStream(); val cIn = PipedInputStream(s2c, 1 shl 20)
        val pool = Executors.newFixedThreadPool(2)
        val fs = pool.submit(Callable { runCatching { server(sIn, s2c) }.also { if (it.isFailure) runCatching { s2c.close() } } })
        val fc = pool.submit(Callable { runCatching { client(cIn, c2s) }.also { if (it.isFailure) runCatching { c2s.close() } } })
        val r = fc.get(20, TimeUnit.SECONDS) to fs.get(20, TimeUnit.SECONDS)
        pool.shutdownNow()
        return r
    }

    @Test fun pairsWithCodeThenReconnectsWithSecretAndExchanges() {
        val peers = FakePeers("123456")
        val (c, s) = connect(
            { i, o -> LinkProtocol.clientHandshake(i, o, "drive-1", "Poste", null, "123456") },
            { i, o -> LinkProtocol.serverHandshake(i, o, "phone-1", "Xiaomi", peers) },
        )
        val client = c.getOrThrow(); val server = s.getOrThrow()
        assertEquals("phone-1", client.peerId)
        assertArrayEquals(client.secret, peers.known["drive-1"])
        server.send(JSONObject().put("t", "loc").put("lat", 46.7))
        assertEquals(46.7, client.receive().getDouble("lat"), 0.0)
        client.send(JSONObject().put("t", "media_cmd").put("cmd", "next"))
        assertEquals("next", server.receive().getString("cmd"))

        // Reconnexion sans code grâce au secret mémorisé.
        peers.code = null
        val secret = client.secret
        val (c2, s2) = connect(
            { i, o -> LinkProtocol.clientHandshake(i, o, "drive-1", "Poste", secret, null) },
            { i, o -> LinkProtocol.serverHandshake(i, o, "phone-1", "Xiaomi", peers) },
        )
        assertTrue(c2.isSuccess); assertTrue(s2.isSuccess)
    }

    @Test fun wrongCodeIsRejectedOnBothSides() {
        val peers = FakePeers("123456")
        val (c, s) = connect(
            { i, o -> LinkProtocol.clientHandshake(i, o, "drive-1", "Poste", null, "654321") },
            { i, o -> LinkProtocol.serverHandshake(i, o, "phone-1", "Xiaomi", peers) },
        )
        assertTrue(c.exceptionOrNull() is LinkProtocol.AuthException)
        assertTrue(s.isFailure)
        assertTrue(peers.known.isEmpty())
    }

    @Test fun unknownOrForgedSecretIsRejected() {
        val peers = FakePeers(null, hashMapOf("drive-1" to LinkProtocol.random(32)))
        val (c, s) = connect(
            { i, o -> LinkProtocol.clientHandshake(i, o, "drive-1", "Poste", LinkProtocol.random(32), null) },
            { i, o -> LinkProtocol.serverHandshake(i, o, "phone-1", "Xiaomi", peers) },
        )
        assertTrue(c.isFailure); assertTrue(s.isFailure)
        val (c2, _) = connect(
            { i, o -> LinkProtocol.clientHandshake(i, o, "drive-2", "Autre", LinkProtocol.random(32), null) },
            { i, o -> LinkProtocol.serverHandshake(i, o, "phone-1", "Xiaomi", peers) },
        )
        assertEquals("Autoradio inconnu : refaire l'association", c2.exceptionOrNull()?.message)
    }

    @Test fun tamperedFrameIsRefused() {
        val key = LinkProtocol.random(32)
        val sealed = LinkProtocol.seal(key, "bonjour".toByteArray())
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()
        try { LinkProtocol.open(key, sealed); fail("trame modifiée acceptée") } catch (_: java.security.GeneralSecurityException) {}
    }

    @Test fun pairingCodeHasSixDigits() {
        repeat(50) { assertTrue(LinkProtocol.newPairingCode().matches(Regex("\\d{6}"))) }
    }
}
