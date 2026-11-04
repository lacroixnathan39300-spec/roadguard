package fr.roadguard.link;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * ROADGUARD Link : protocole commun au téléphone (appli GPS, serveur) et à l'autoradio (DRIVE, client).
 *
 * Trames : longueur (4 octets) + contenu. La poignée de main est en clair (identifiants et nonces),
 * tout le reste est chiffré et authentifié en AES-256-GCM avec une clé de session propre à chaque
 * connexion. Association : DRIVE affiche un code à 6 chiffres, saisi sur le téléphone ; le téléphone
 * envoie alors un secret permanent chiffré avec une clé dérivée de ce code. Les messages portent un
 * numéro de séquence croissant (pas de rejeu).
 */
public final class LinkProtocol {
    public static final int VERSION = 1;
    public static final int PORT = 47390;
    public static final String SERVICE_TYPE = "_roadguard._tcp.";
    public static final int MAX_FRAME = 512 * 1024;
    static final int PAIRING_ITERATIONS = 20_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LinkProtocol() {}

    // ---- Trames --------------------------------------------------------------------------

    public static void writeFrame(DataOutputStream out, byte[] payload) throws IOException {
        if (payload.length > MAX_FRAME) throw new IOException("Trame trop grande");
        synchronized (out) {
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
        }
    }

    public static byte[] readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_FRAME) throw new IOException("Trame invalide");
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    // ---- Cryptographie -------------------------------------------------------------------

    public static byte[] random(int size) {
        byte[] b = new byte[size];
        RANDOM.nextBytes(b);
        return b;
    }

    /** Code d'association à 6 chiffres affiché par DRIVE. */
    public static String newPairingCode() {
        return String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }

    static byte[] pairingKey(String code, byte[] clientNonce, byte[] serverNonce) throws GeneralSecurityException {
        byte[] salt = concat(utf8("rg-pair"), clientNonce, serverNonce);
        PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, PAIRING_ITERATIONS, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    static byte[] sessionKey(byte[] secret, byte[] clientNonce, byte[] serverNonce) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(concat(utf8("rg-session"), clientNonce, serverNonce));
    }

    static byte[] seal(byte[] key, byte[] plain) throws GeneralSecurityException {
        byte[] iv = random(12);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return concat(iv, c.doFinal(plain));
    }

    static byte[] open(byte[] key, byte[] sealed) throws GeneralSecurityException {
        if (sealed.length < 12 + 16) throw new GeneralSecurityException("Trame chiffrée trop courte");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, sealed, 0, 12));
        return c.doFinal(sealed, 12, sealed.length - 12);
    }

    /** Empreinte courte d'un secret, pour reconnaître un appareil associé sans exposer le secret. */
    public static String fingerprint(byte[] secret) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(secret);
            return b64(new byte[]{d[0], d[1], d[2], d[3], d[4], d[5]});
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    public static byte[] unb64(String s) { return Base64.getDecoder().decode(s); }
    static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    static byte[] concat(byte[]... parts) {
        int n = 0;
        for (byte[] p : parts) n += p.length;
        byte[] out = new byte[n];
        int at = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, out, at, p.length); at += p.length; }
        return out;
    }

    // ---- Session chiffrée ----------------------------------------------------------------

    /** Connexion établie : envoi et réception de messages JSON chiffrés. */
    public static final class Session {
        public final String peerId;
        public final String peerName;
        /** Secret permanent (nouveau si cette connexion était une association). */
        public final byte[] secret;
        private final byte[] key;
        private final DataInputStream in;
        private final DataOutputStream out;
        private long sendSeq;
        private long recvSeq = -1;

        Session(String peerId, String peerName, byte[] secret, byte[] key, DataInputStream in, DataOutputStream out) {
            this.peerId = peerId; this.peerName = peerName; this.secret = secret; this.key = key; this.in = in; this.out = out;
        }

        public void send(JSONObject message) throws IOException {
            try {
                synchronized (out) {
                    message.put("s", sendSeq++);
                    writeFrame(out, seal(key, utf8(message.toString())));
                }
            } catch (GeneralSecurityException | JSONException e) {
                throw new IOException("Envoi impossible", e);
            }
        }

        /** Ferme la connexion (les deux sens). */
        public void close() throws IOException {
            try { in.close(); } finally { out.close(); }
        }

        public JSONObject receive() throws IOException {
            try {
                JSONObject m = new JSONObject(new String(open(key, readFrame(in)), StandardCharsets.UTF_8));
                long seq = m.optLong("s", -1);
                if (seq <= recvSeq) throw new IOException("Message rejoué ou désordonné");
                recvSeq = seq;
                return m;
            } catch (GeneralSecurityException e) {
                throw new IOException("Message non authentifié", e);
            } catch (JSONException e) {
                throw new IOException("Message illisible", e);
            }
        }
    }

    // ---- Poignée de main -----------------------------------------------------------------

    /** Erreur d'association ou d'authentification (code faux, appareil inconnu…). */
    public static final class AuthException extends IOException {
        public AuthException(String message) { super(message); }
    }

    /**
     * Côté autoradio (client). [secret] = secret connu pour ce téléphone, ou null pour une association
     * avec [code].
     */
    public static Session clientHandshake(InputStream rawIn, OutputStream rawOut, String myId, String myName,
                                          byte[] secret, String code) throws IOException {
        DataInputStream in = new DataInputStream(rawIn);
        DataOutputStream out = new DataOutputStream(rawOut);
        try {
            byte[] cn = random(16);
            JSONObject hello = new JSONObject().put("t", "hello").put("v", VERSION).put("id", myId).put("name", myName)
                .put("n", b64(cn)).put("pair", secret == null);
            writeFrame(out, utf8(hello.toString()));
            JSONObject reply = new JSONObject(new String(readFrame(in), StandardCharsets.UTF_8));
            if (!"hello".equals(reply.optString("t"))) throw new AuthException(reply.optString("error", "Réponse inattendue"));
            if (reply.optInt("v") != VERSION) throw new AuthException("Versions incompatibles : mettre à jour les deux applis");
            byte[] sn = unb64(reply.getString("n"));
            byte[] s = secret;
            if (s == null) {
                if (code == null) throw new AuthException("Code d'association manquant");
                byte[] pk = pairingKey(code, cn, sn);
                try {
                    s = open(pk, readFrame(in));
                } catch (GeneralSecurityException e) {
                    throw new AuthException("Code incorrect");
                }
            }
            byte[] key = sessionKey(s, cn, sn);
            Session session = new Session(reply.getString("id"), reply.optString("name"), s, key, in, out);
            session.send(new JSONObject().put("t", "auth"));
            JSONObject ok = session.receive();
            if (!"ok".equals(ok.optString("t"))) throw new AuthException("Authentification refusée");
            return session;
        } catch (JSONException | GeneralSecurityException e) {
            throw new IOException("Poignée de main invalide", e);
        } catch (IOException e) {
            throw e;
        }
    }

    /** Ce que le serveur sait des autoradios déjà associés. */
    public interface Peers {
        /** Secret enregistré pour cet autoradio, ou null. */
        byte[] secretFor(String id);
        /** Code saisi sur le téléphone pour une association en cours, ou null. */
        String pendingCode();
        /** Nouvel autoradio associé : enregistrer son secret. */
        void paired(String id, String name, byte[] secret);
    }

    /** Côté téléphone (serveur). */
    public static Session serverHandshake(InputStream rawIn, OutputStream rawOut, String myId, String myName, Peers peers)
        throws IOException {
        DataInputStream in = new DataInputStream(rawIn);
        DataOutputStream out = new DataOutputStream(rawOut);
        try {
            JSONObject hello = new JSONObject(new String(readFrame(in), StandardCharsets.UTF_8));
            if (!"hello".equals(hello.optString("t"))) throw new AuthException("Message inattendu");
            String peerId = hello.getString("id");
            byte[] cn = unb64(hello.getString("n"));
            if (cn.length != 16) throw new AuthException("Nonce invalide");
            boolean pairing = hello.optBoolean("pair");
            byte[] secret = pairing ? null : peers.secretFor(peerId);
            String code = pairing ? peers.pendingCode() : null;
            if (hello.optInt("v") != VERSION) {
                writeFrame(out, utf8(new JSONObject().put("t", "error").put("error", "Versions incompatibles").toString()));
                throw new AuthException("Versions incompatibles");
            }
            if (!pairing && secret == null || pairing && code == null) {
                String why = pairing ? "Saisis d'abord le code sur le téléphone" : "Autoradio inconnu : refaire l'association";
                writeFrame(out, utf8(new JSONObject().put("t", "error").put("error", why).toString()));
                throw new AuthException(why);
            }
            byte[] sn = random(16);
            writeFrame(out, utf8(new JSONObject().put("t", "hello").put("v", VERSION).put("id", myId).put("name", myName)
                .put("n", b64(sn)).toString()));
            if (pairing) {
                secret = random(32);
                writeFrame(out, seal(pairingKey(code, cn, sn), secret));
            }
            Session session = new Session(peerId, hello.optString("name"), secret, sessionKey(secret, cn, sn), in, out);
            JSONObject auth;
            try {
                auth = session.receive();
            } catch (IOException e) {
                throw new AuthException(pairing ? "Code incorrect" : "Authentification refusée");
            }
            if (!"auth".equals(auth.optString("t"))) throw new AuthException("Authentification refusée");
            if (pairing) peers.paired(peerId, hello.optString("name"), secret);
            session.send(new JSONObject().put("t", "ok"));
            return session;
        } catch (JSONException | GeneralSecurityException e) {
            throw new IOException("Poignée de main invalide", e);
        }
    }
}
