//proba pegar esto es QrCodeUtil, a ver si asi te deja de dar ese error
package Utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para crear PNGs de QR. 🔒 Guarda automáticamente los QR en la
 * carpeta del usuario (user.home).
 */
public class QRCodeUtil {

    private static final int DEFAULT_SIZE = 512;

    /**
     * Carpeta base donde se guardarán los QR. Usa el home del usuario actual,
     * para que funcione en cualquier PC.
     */
    private static final String FORCED_BASE_DIR
            = System.getProperty("user.home") + "/Residencial/qrcodes";

    public static String generarQRDesdeToken(String token) throws WriterException, IOException {
        return generarQRDesdeToken(token, null, null, DEFAULT_SIZE);
    }

    public static String generarQRDesdeToken(String token, String directorio, String nombreArchivo, int size)
            throws WriterException, IOException {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token vacío.");
        }

        String safeName = (nombreArchivo != null && !nombreArchivo.trim().isEmpty())
                ? sanitizeFileName(nombreArchivo)
                : "qr_" + shortHash(token) + "_" + nowTag() + ".png";

        Path base = resolveBaseDir(directorio);
        Files.createDirectories(base);

        Path destino = base.resolve(safeName);
        try {
            Files.deleteIfExists(destino);
        } catch (Exception ignore) {
        }

        escribirQR(token, destino, size);
        return destino.toAbsolutePath().toString();
    }

    public static String generarQRDesdeTexto(String data) throws WriterException, IOException {
        return generarQRDesdeTexto(data, null, null, DEFAULT_SIZE);
    }

    public static String generarQRDesdeTexto(String data, String directorio, String nombreArchivo, int size)
            throws WriterException, IOException {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Contenido vacío.");
        }

        String safeName = (nombreArchivo != null && !nombreArchivo.trim().isEmpty())
                ? sanitizeFileName(nombreArchivo)
                : "qr_text_" + shortHash(data) + "_" + nowTag() + ".png";

        Path base = resolveBaseDir(directorio);
        Files.createDirectories(base);

        Path destino = base.resolve(safeName);
        try {
            Files.deleteIfExists(destino);
        } catch (Exception ignore) {
        }

        escribirQR(data, destino, size);
        return destino.toAbsolutePath().toString();
    }

    @Deprecated
    public static String generarQR(String nombre, String apellido, String correo, String lote, Integer numeroCasa)
            throws WriterException, IOException {
        String texto = String.format(
                "Nombre: %s%nApellido: %s%nCorreo: %s%nLote: %s%nNo. Casa: %s",
                nvl(nombre), nvl(apellido), nvl(correo), nvl(lote),
                (numeroCasa == null ? "-" : String.valueOf(numeroCasa))
        );

        String nombreArchivo = String.format("L_%s_Casa_%s.png",
                safe(nvl(lote)),
                safe(numeroCasa == null ? "-" : String.valueOf(numeroCasa)));

        return generarQRDesdeTexto(texto, null, nombreArchivo, DEFAULT_SIZE);
    }

    /* ===================== Internos ===================== */
    private static void escribirQR(String data, Path destino, int size)
            throws WriterException, IOException {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, size, size);
        MatrixToImageWriter.writeToPath(matrix, "PNG", destino);
    }

    private static Path resolveBaseDir(String preferido) {
        if (preferido != null && !preferido.trim().isEmpty()) {
            return Paths.get(preferido.trim());
        }
        return Paths.get(FORCED_BASE_DIR.trim());
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private static String nowTag() {
        return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    }

    private static String shortHash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(20);
            for (int i = 0; i < 10; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return s.replaceAll("[^a-zA-Z0-9_-]", "").substring(0, Math.min(12, s.length()));
        }
    }

    private static String nvl(String s) {
        return (s == null) ? "" : s;
    }

    private static String safe(String s) {
        return s.replaceAll("\\s+", "_");
    }
}