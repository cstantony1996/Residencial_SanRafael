package Servicio;

import Conexion_DB.Conexion;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Servicio de Bitácora/Auditoría. - Conserva el INSERT y columnas actuales de
 * tu tabla bitacora. - Agrega métodos utilitarios y sobrecargas para facilitar
 * el uso desde controladores/servicios. - Genera y propaga un txid por
 * solicitud para trazar flujos completos.
 */
public final class BitacoraService {

        private BitacoraService() {
        }

        /**
         * Acciones estandarizadas para reportar en bitácora.
         */
        public enum Accion {
                CREAR, EDITAR, ELIMINAR, LOGIN, LOGOUT,
                QR_VALIDADO, QR_DENEGADO, RESERVA, PAGO, PAQUETERIA, REPORTE, OTRO
        }

        // ==============================
        // Configuración y utilidades
        // ==============================
        /**
         * Atributo de request para el ID de transacción.
         */
        public static final String REQ_TXID = "txid";

        /**
         * Límite de tamaño para campos variables (previene errores de
         * longitud).
         */
        private static final int MAX_UA = 255;
        private static final int MAX_URL = 512;
        private static final int MAX_DESCRIPCION = 1000;
        private static final int MAX_DATOS = 4000;      // datos_antes/datos_despues
        private static final int MAX_ERROR = 8000;      // stacktrace

        private static final SecureRandom RAND = new SecureRandom();

        /**
         * Genera un txid hex corto (16 chars).
         */
        private static String generarTxId() {
                byte[] b = new byte[8];
                RAND.nextBytes(b);
                StringBuilder sb = new StringBuilder();
                for (byte x : b) {
                        sb.append(String.format("%02x", x));
                }
                return sb.toString();
        }

        /**
         * Obtiene/crea el txid y lo fija en el request.
         */
        public static String ensureTxId(HttpServletRequest req) {
                if (req == null) {
                        return null;
                }
                Object v = req.getAttribute(REQ_TXID);
                if (v == null) {
                        String tx = generarTxId();
                        req.setAttribute(REQ_TXID, tx);
                        return tx;
                }
                return String.valueOf(v);
        }

        // ==============================
        // API principal (genérica)
        // ==============================
        /**
         * Log genérico y detallado. Úsalo cuando quieras registrar
         * antes/después y/o manejar errores.
         */
        public static void log(
                HttpServletRequest req,
                String modulo,
                Accion accion,
                String entidad,
                Object entidadId,
                String descripcion,
                String datosAntes, // JSON/Texto opcional
                String datosDespues, // JSON/Texto opcional
                boolean ok,
                Throwable error
        ) {
                // Extrae datos de sesión de forma segura
                String txid = req != null ? ensureTxId(req) : null;

                HttpSession ses = (req != null) ? req.getSession(false) : null;
                String dpi = ses != null ? s(ses.getAttribute("usuarioDPI")) : null;
                String nombre = ses != null ? s(ses.getAttribute("usuarioNombre")) : null;
                String rol = ses != null ? s(ses.getAttribute("usuarioRol")) : null;

                String ip = req != null ? nvl(req.getRemoteAddr(), null) : null;
                String ua = req != null ? nvl(req.getHeader("User-Agent"), null) : null;
                String url = req != null ? nvl(req.getRequestURI(), null) : null;
                String err = (error == null) ? null : stack(error);

                // Normaliza y recorta para evitar errores por longitudes en BD
                ua = cut(ua, MAX_UA);
                url = cut(url, MAX_URL);
                descripcion = cut(descripcion, MAX_DESCRIPCION);
                datosAntes = cut(datosAntes, MAX_DATOS);
                datosDespues = cut(datosDespues, MAX_DATOS);
                err = cut(err, MAX_ERROR);

                String sql
                        = "INSERT INTO bitacora "
                        + "(fecha, usuario_dpi, usuario_nombre, rol, ip, url, user_agent, "
                        + " modulo, accion, entidad, entidad_id, descripcion, datos_antes, datos_despues, resultado, error, txid) "
                        + "VALUES (NOW(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {

                        int i = 1;
                        ps.setString(i++, emptyToNull(dpi));
                        ps.setString(i++, emptyToNull(nombre));
                        ps.setString(i++, emptyToNull(rol));
                        ps.setString(i++, emptyToNull(ip));
                        ps.setString(i++, emptyToNull(url));
                        ps.setString(i++, emptyToNull(ua));
                        ps.setString(i++, n(modulo));
                        ps.setString(i++, (accion == null ? Accion.OTRO : accion).name());
                        ps.setString(i++, emptyToNull(entidad));
                        ps.setString(i++, entidadId == null ? null : entidadId.toString());
                        ps.setString(i++, emptyToNull(descripcion));
                        ps.setString(i++, emptyToNull(datosAntes));
                        ps.setString(i++, emptyToNull(datosDespues));
                        ps.setString(i++, ok ? "OK" : "ERROR");
                        ps.setString(i++, emptyToNull(err));
                        ps.setString(i++, emptyToNull(txid));

                        ps.executeUpdate();
                } catch (SQLException e) {
                        // Evita bucles de log si falla la bitácora
                        e.printStackTrace();
                }
        }

        // ==============================
        // Sobrecargas sencillas (calzan con casos típicos)
        // ==============================
        /**
         * Caso simple de éxito sin before/after ni error.
         */
        public static void logOk(HttpServletRequest req, String modulo, Accion accion,
                String entidad, Object entidadId, String descripcion) {
                log(req, modulo, accion, entidad, entidadId, descripcion, null, null, true, null);
        }

        /**
         * Caso simple de error con excepción.
         */
        public static void logError(HttpServletRequest req, String modulo, Accion accion,
                String entidad, Object entidadId, String descripcion, Throwable error) {
                log(req, modulo, accion, entidad, entidadId, descripcion, null, null, false, error);
        }

        /**
         * Cambio con "antes/después" (por ejemplo, editar un registro).
         */
        public static void logCambio(HttpServletRequest req, String modulo, Accion accion,
                String entidad, Object entidadId,
                String descripcion, String datosAntes, String datosDespues, boolean ok) {
                log(req, modulo, accion, entidad, entidadId, descripcion, datosAntes, datosDespues, ok, null);
        }

        /**
         * Registro minimalista SIN HttpServletRequest (útil en tareas batch o
         * cuando no tienes el request). Nota: Como tu tabla no tiene
         * usuario_id, aquí lo pasamos como entidad/entidadId/descripcion.
         */
        public static void registrar(String categoria, String detalle, Object entidadId,
                String ip, String userAgent) {
                // Emula un log "genérico" sin request
                // modulo = categoria (ej. "PAGO_REGISTRADO"), accion = OTRO
                // entidad = categoria también para fácil búsqueda
                // url = null (no hay request), rol/nombre/dpi = null
                String url = null;
                String txid = generarTxId(); // genera un txid efímero para este evento

                String modulo = n(categoria);
                String entidad = n(categoria);

                String ua = cut(userAgent, MAX_UA);
                String desc = cut(detalle, MAX_DESCRIPCION);

                String sql
                        = "INSERT INTO bitacora "
                        + "(fecha, usuario_dpi, usuario_nombre, rol, ip, url, user_agent, "
                        + " modulo, accion, entidad, entidad_id, descripcion, datos_antes, datos_despues, resultado, error, txid) "
                        + "VALUES (NOW(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {

                        int i = 1;
                        ps.setString(i++, null);                       // usuario_dpi
                        ps.setString(i++, null);                       // usuario_nombre
                        ps.setString(i++, null);                       // rol
                        ps.setString(i++, emptyToNull(ip));            // ip
                        ps.setString(i++, emptyToNull(url));           // url
                        ps.setString(i++, emptyToNull(ua));            // user_agent
                        ps.setString(i++, modulo);                     // modulo
                        ps.setString(i++, Accion.OTRO.name());         // accion
                        ps.setString(i++, entidad);                    // entidad
                        ps.setString(i++, entidadId == null ? null : entidadId.toString()); // entidad_id
                        ps.setString(i++, emptyToNull(desc));          // descripcion
                        ps.setString(i++, null);                       // datos_antes
                        ps.setString(i++, null);                       // datos_despues
                        ps.setString(i++, "OK");                       // resultado
                        ps.setString(i++, null);                       // error
                        ps.setString(i++, txid);                       // txid

                        ps.executeUpdate();
                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }

        // ==============================
        // Helpers
        // ==============================
        private static String s(Object o) {
                return o == null ? null : o.toString();
        }

        private static String n(String s) {
                return (s == null || s.trim().isEmpty()) ? "General" : s.trim();
        }

        private static String nvl(String s, String def) {
                return (s == null || s.isEmpty()) ? def : s;
        }

        private static String cut(String s, int max) {
                if (s == null) {
                        return null;
                }
                if (s.length() <= max) {
                        return s;
                }
                return s.substring(0, max);
        }

        private static String emptyToNull(String s) {
                return (s == null || s.trim().isEmpty()) ? null : s.trim();
        }

        private static String stack(Throwable t) {
                try {
                        StringWriter sw = new StringWriter();
                        t.printStackTrace(new PrintWriter(sw));
                        return sw.toString();
                } catch (Exception e) {
                        return t.toString();
                }
        }
}
