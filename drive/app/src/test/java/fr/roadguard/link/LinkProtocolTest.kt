package fr.roadguard.link

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LinkProtocolTest {
    private class FakePeers(var code: String?, val known: MutableMap<String, ByteArray> = HashMap()) : LinkProtocol.Peers {
        override fun secretFor(id: String) = known[id]
        override fun pendingCode() = code
        override fun paired(id: String, name: String, secret: ByteArray) { known[id] = secret }
    }

    private val sockets = ArrayList<java.net.Socket>()

    /** Relie un client et un serveur par une vraie connexion TCP locale. */
    private fun <C, S> connect(client: (java.io.InputStream, java.io.OutputStream) -> C,
                               server: (java.io.InputStream, java.io.OutputStream) -> S): Pair<Result<C>, Result<S>> {
        val listener = java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())
        val pool = Executors.newFixedThreadPool(2)
        val fs = pool.submit(Callable {
            val sock = listener.accept().also { sockets += it }
            runCatching { server(sock.getInputStream(), sock.getOutputStream()) }.also { if (it.isFailure) sock.close() }
        })
        val fc = pool.submit(Callable {
            val sock = java.net.Socket(java.net.InetAddress.getLoopbackAddress(), listener.localPort).also { sockets += it }
            sock.soTimeout = 15_000
            runCatching { client(sock.getInputStream(), sock.getOutputStream()) }.also { if (it.isFailure) sock.close() }
        })
        val r = fc.get(30, TimeUnit.SECONDS) to fs.get(30, TimeUnit.SECONDS)
        pool.shutdown()
        listener.close()
        return r
    }

    @org.junit.After fun closeSockets() { sockets.forEach { runCatching { it.close() } } }

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
