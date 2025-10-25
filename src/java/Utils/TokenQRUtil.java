package Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Utilidad para crear/validar tokens HMAC-SHA256 para QR.
 *
 * Formato del payload (Base64 URL-safe, sin padding):
 *   kind|subjectId|expEpochSec|version
 * donde:
 *   kind:   "U" (usuario/residente) | "V" (visita)
 *   subjectId: id del usuario o de la visita (según kind)
 *   expEpochSec: 0 = sin expiración embebida; >0 = epoch seconds
 *   version: entero para posibles cambios en el esquema
 *
 * Token final: base64url(payload) + "." + base64url(hmac(payload))
 *
 * Nota: Aunque el token puede traer expEpochSec, la expiración/uso por intentos
 *       se recomienda validarla en BD (tabla qr_tokens).
 */
public class TokenQRUtil {

    // ==========================
    // Configuración del secreto
    // ==========================
    private static byte[] secretBytes() {
        // 1) Variable de entorno
        String b64 = System.getenv("QR_HMAC_SECRET_B64");
        // 2) Propiedad JVM (-Dqr.hmac.secret.b64=...)
        if (b64 == null) b64 = System.getProperty("qr.hmac.secret.b64");
        if (b64 == null || b64.trim().isEmpty()) {
            throw new IllegalStateException(
                "Secreto HMAC no configurado (QR_HMAC_SECRET_B64 / -Dqr.hmac.secret.b64)."
            );
        }
        try {
            return Base64.getUrlDecoder().decode(b64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                "Formato inválido del secreto HMAC (debe ser Base64 URL-safe).", ex
            );
        }
    }

    private static byte[] hmac(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretBytes(), "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static boolean constantTimeEq(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    private static String b64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] b64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static String buildPayload(String kind, int subjectId, long expEpochSec, int version) {
        return kind + "|" + subjectId + "|" + expEpochSec + "|" + version;
    }

    // ==========================
    // Creación de tokens
    // ==========================

    /** Token permanente para RESIDENTE (sin expiración embebida). */
    public static String generarTokenResidente(int usuarioId) throws Exception {
        return generarTokenGenerico("U", usuarioId, 0L, 1);
    }

    /** Token para VISITA, con expiración embebida opcional (puedes pasar 0 si no deseas embebida). */
    public static String generarTokenVisita(int visitaId, long expEpochSec) throws Exception {
        return generarTokenGenerico("V", visitaId, expEpochSec, 1);
    }

    /** Creador genérico (por si necesitas otro tipo en el futuro). */
    public static String generarTokenGenerico(String kind, int subjectId, long expEpochSec, int version) throws Exception {
        String payload = buildPayload(kind, subjectId, expEpochSec, version);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] sig = hmac(payloadBytes);
        return b64Url(payloadBytes) + "." + b64Url(sig);
    }

    // ==========================
    // Compatibilidad hacia atrás
    // ==========================

    /** (Compat) Genera token con el esquema antiguo (asume kind="U"). */
    public static String generarToken(int userId, long expEpochSec, int version) throws Exception {
        return generarTokenGenerico("U", userId, expEpochSec, version);
    }

    /** (Compat) Valida y devuelve userId SOLO si kind="U"; de lo contrario, null. */
    public static Integer validarYObtenerUsuario(String token) throws Exception {
        if (!validar(token)) return null;
        Parsed p = parse(token);
        if (p == null) return null;
        return "U".equals(p.kind) ? p.subjectId : null;
    }

    // ==========================
    // Validación / Parsing
    // ==========================

    /** Valida firma y expiración embebida (si expEpochSec>0). */
    public static boolean validar(String token) throws Exception {
        Parsed p = parse(token);
        if (p == null) return false;
        // Si trae expiración embebida (>0), chequearla:
        if (p.expEpochSec > 0) {
            long now = System.currentTimeMillis() / 1000L;
            if (now > p.expEpochSec) return false;
        }
        return true;
    }

    /** Devuelve el ID de usuario si kind="U"; en otro caso null. */
    public static Integer obtenerUsuarioId(String token) throws Exception {
        Parsed p = parse(token);
        if (p == null) return null;
        return "U".equals(p.kind) ? p.subjectId : null;
    }

    /** Devuelve el ID de visita si kind="V"; en otro caso null. */
    public static Integer obtenerVisitaId(String token) throws Exception {
        Parsed p = parse(token);
        if (p == null) return null;
        return "V".equals(p.kind) ? p.subjectId : null;
    }

    /** Lee la expiración embebida del token (0 si no trae). */
    public static long obtenerExpEpochSec(String token) throws Exception {
        Parsed p = parse(token);
        return (p == null) ? 0L : p.expEpochSec;
    }

    /** Lee versión del token. */
    public static int obtenerVersion(String token) throws Exception {
        Parsed p = parse(token);
        return (p == null) ? -1 : p.version;
    }

    // ==========================
    // Internos
    // ==========================

    private static class Parsed {
        String kind;       // "U" | "V"
        int subjectId;     // usuarioId o visitaId
        long expEpochSec;  // 0 = sin exp
        int version;
    }

    private static Parsed parse(String token) throws Exception {
        if (token == null) return null;
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return null;

        String p64 = token.substring(0, dot);
        String s64 = token.substring(dot + 1);

        byte[] payloadBytes;
        byte[] firma;
        try {
            payloadBytes = b64UrlDecode(p64);
            firma = b64UrlDecode(s64);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Verificar HMAC
        byte[] esperado = hmac(payloadBytes);
        if (!constantTimeEq(firma, esperado)) return null;

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] campos = payload.split("\\|");
        if (campos.length < 4) return null;

        Parsed out = new Parsed();
        out.kind = campos[0];
        try {
            out.subjectId = Integer.parseInt(campos[1]);
            out.expEpochSec = Long.parseLong(campos[2]);
            out.version = Integer.parseInt(campos[3]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (!"U".equals(out.kind) && !"V".equals(out.kind)) {
            // Kind desconocido -> inválido
            return null;
        }
        return out;
    }
}
