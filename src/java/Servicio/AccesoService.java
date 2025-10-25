package Servicio;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;

import Conexion_DB.Conexion;
import Utils.TokenQRUtil;
import Utils.CorreoUtil;

/**
 * Lógica de validación de accesos. - validarPorTexto: busca usuario por
 * correo+lote+numero_casa (correo case-insensitive). - validarPorToken: valida
 * token, chequea estado en qr_tokens, descuenta uso si aplica (FA02), registra
 * accesos en accesos_log, dispara notificación (RN2) y escribe bitácora. -
 * placaDelUsuario / contarVehiculosActivos: utilidades para reglas de negocio.
 */
public class AccesoService {

        /**
         * Datos usados para RN2.
         */
        public static class DatosNotificacion {

                public String emailResidente;  // destinatario: residente propietario
                public String nombrePersona;   // "persona" a la que se emitió el QR (visitante o residente)
                public String validezTexto;    // "permanente" | "N usos" | "válido hasta dd/MM/yyyy HH:mm"
        }

        /**
         * Valida por texto (correo/lote/casa).
         */
        public Integer validarPorTexto(String correo, String lote, Integer numeroCasa) throws SQLException {
                if (correo == null || lote == null || numeroCasa == null) {
                        return null;
                }

                final String sql = "SELECT id FROM usuarios WHERE UPPER(correo)=UPPER(?) AND lote=? AND numero_casa=?";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setString(1, correo.trim());
                        ps.setString(2, lote.trim());
                        ps.setInt(3, numeroCasa);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? rs.getInt(1) : null;
                        }
                }
        }

        /**
         * Versión compat (sin request). Si quieres bitácora contextual, usa la
         * sobrecargada con HttpServletRequest.
         */
        public Integer validarPorToken(String token) throws Exception {
                return validarPorToken(token, null);
        }

        /**
         * Valida un TOKEN de QR: 1) Verifica firma (HMAC) y expiración embebida
         * si la trae. 2) Lee qr_tokens (estado/usos/expira) y verifica reglas.
         * 3) Descuenta 1 uso si aplica (FA02). 4) Registra accesos_log, escribe
         * bitácora y dispara RN2 (sin bloquear, RN01).
         *
         * @return usuarioId (propietario/residente) si aprobado, null si
         * rechazado.
         */
        public Integer validarPorToken(String token, HttpServletRequest req) throws Exception {
                if (token == null || token.trim().isEmpty()) {
                        // Bitácora: denegado (token vacío)
                        Servicio.BitacoraService.log(req, "AccesoQR", Servicio.BitacoraService.Accion.QR_DENEGADO,
                                "qr_tokens", null, "Token vacío", null, null, false, null);
                        registrarLog(token, null, "RECHAZADO", "Token vacío", null, null);
                        return null;
                }

                if (!TokenQRUtil.validar(token)) {
                        Servicio.BitacoraService.log(req, "AccesoQR", Servicio.BitacoraService.Accion.QR_DENEGADO,
                                "qr_tokens", token, "Token inválido (HMAC/estructura)", null, null, false, null);
                        registrarLog(token, null, "RECHAZADO", "Token inválido", null, null);
                        return null;
                }

                String tipo = null;           // 'residente' | 'visitante'
                Integer usuarioId = null;
                Integer visitaId = null;
                Integer usosRestantes = null;
                Timestamp expiraEn = null;
                String estado = null;

                final Timestamp now = new Timestamp(System.currentTimeMillis());

                try (Connection cn = Conexion.getConnection()) {
                        cn.setAutoCommit(false);

                        // FOR UPDATE para consistencia (evita condiciones de carrera al descontar usos)
                        final String sel = "SELECT id, tipo, usuario_id, visita_id, usos_restantes, expira_en, estado "
                                + "FROM qr_tokens WHERE token=? FOR UPDATE";
                        try (PreparedStatement ps = cn.prepareStatement(sel)) {
                                ps.setString(1, token);
                                try (ResultSet rs = ps.executeQuery()) {
                                        if (!rs.next()) {
                                                cn.rollback();
                                                Servicio.BitacoraService.log(req, "AccesoQR", Servicio.BitacoraService.Accion.QR_DENEGADO,
                                                        "qr_tokens", token, "Token no registrado", null, null, false, null);
                                                registrarLog(token, null, "RECHAZADO", "Token no registrado", null, null);
                                                return null;
                                        }
                                        tipo = rs.getString("tipo");
                                        usuarioId = getInteger(rs, "usuario_id");
                                        visitaId = getInteger(rs, "visita_id");
                                        usosRestantes = getInteger(rs, "usos_restantes");
                                        expiraEn = rs.getTimestamp("expira_en");
                                        estado = rs.getString("estado");
                                }
                        }

                        // Estado debe ser ACTIVO
                        if (!"activo".equalsIgnoreCase(estado)) {
                                cn.rollback();
                                Servicio.BitacoraService.log(req, "AccesoQR", Servicio.BitacoraService.Accion.QR_DENEGADO,
                                        "qr_tokens", token, "Token no activo (" + estado + ")", null, null, false, null);
                                registrarLog(token, usuarioId, "RECHAZADO", "Token no activo (" + estado + ")", tipo, null);
                                return null;
                        }

                        // ¿Expirado por tiempo?
                        if (expiraEn != null && expiraEn.before(now)) {
                                actualizarEstadoEnTransaccion(cn, token, "invalido");
                                if ("visitante".equalsIgnoreCase(tipo) && visitaId != null) {
                                        marcarVisitaInactiva(cn, visitaId);
                                }
                                cn.commit();
                                Servicio.BitacoraService.log(req, "AccesoQR", Servicio.BitacoraService.Accion.QR_DENEGADO,
                                        "qr_tokens", token, "Token expirado", null, null, false, null);
                                registrarLog(token, usuarioId, "RECHAZADO", "Token expirado", tipo, null);
                                return null;
                        }

                        // ¿Tiene contador de usos?
                        if (usosRestantes != null) {
                                int nuevo = usosRestantes - 1;

                                // Descontar
                                try (PreparedStatement ps = cn.prepareStatement(
                                        "UPDATE qr_tokens SET usos_restantes=? WHERE token=?")) {
                                        ps.setInt(1, nuevo);
                                        ps.setString(2, token);
                                        ps.executeUpdate();
                                }

                                // Si ya quedó en 0 → marcar usado e inactivar visita
                                if (nuevo <= 0) {
                                        actualizarEstadoEnTransaccion(cn, token, "usado");
                                        if ("visitante".equalsIgnoreCase(tipo) && visitaId != null) {
                                                marcarVisitaInactiva(cn, visitaId);
                                        }
                                }
                        }

                        cn.commit();
                }

                // ===== Aprobado =====
                registrarLog(token, usuarioId, "APROBADO", null, tipo, null);
                Servicio.BitacoraService.log(req, "AccesoQR", Servicio.BitacoraService.Accion.QR_VALIDADO,
                        "qr_tokens", token, "Acceso permitido", null, null, true, null);

                // RN2
                notificarAccesoAutorizado(usuarioId, token);

                return usuarioId;
        }

        /* ===== helpers usados arriba ===== */
        private void actualizarEstadoEnTransaccion(Connection cn, String token, String nuevoEstado) throws SQLException {
                try (PreparedStatement ps = cn.prepareStatement("UPDATE qr_tokens SET estado=? WHERE token=?")) {
                        ps.setString(1, nuevoEstado);
                        ps.setString(2, token);
                        ps.executeUpdate();
                }
        }

        private void marcarVisitaInactiva(Connection cn, int visitaId) throws SQLException {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE visitas SET estado='inactivo', cancelado_en=NOW() "
                        + "WHERE id=? AND estado='activo'")) {
                        ps.setInt(1, visitaId);
                        ps.executeUpdate();
                }
        }

        /**
         * ¿La placa pertenece al usuario y está activa?
         */
        public boolean placaDelUsuario(Integer usuarioId, String placa) throws SQLException {
                if (usuarioId == null) {
                        return false;
                }
                String p = normalizarPlaca(placa);
                if (p == null) {
                        return false;
                }

                final String sql = "SELECT 1 FROM vehiculos WHERE usuario_id=? AND placa=? AND activo=1";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, usuarioId);
                        ps.setString(2, p);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next();
                        }
                }
        }

        /**
         * Cantidad de vehículos activos del usuario.
         */
        public int contarVehiculosActivos(int usuarioId) throws SQLException {
                final String sql = "SELECT COUNT(*) FROM vehiculos WHERE usuario_id=? AND activo=1";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, usuarioId);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? rs.getInt(1) : 0;
                        }
                }
        }

        /* ================== Notificación (RN2) ================== */
        /**
         * Dispara la notificación al residente (RN2). No bloquea el flujo
         * (RN01). - Si el token es de visita (kind=V), el "nombrePersona" será
         * el NOMBRE DEL VISITANTE. - Si el token es de usuario (kind=U),
         * "nombrePersona" será el nombre del RESIDENTE. - El destinatario
         * siempre es el correo del RESIDENTE propietario.
         */
        public void notificarAccesoAutorizado(Integer usuarioId, String tokenOrNull) {
                if (usuarioId == null) {
                        return;
                }
                try {
                        DatosNotificacion dn = cargarDatosNotificacion(usuarioId, tokenOrNull);
                        if (dn == null || dn.emailResidente == null) {
                                return;
                        }

                        LocalDateTime ahora = LocalDateTime.now();
                        String validez = (dn.validezTexto == null || dn.validezTexto.trim().isEmpty())
                                ? "permanente" : dn.validezTexto;

                        // RN2 (texto exacto del CU)
                        CorreoUtil.enviarNotificacionAcceso(
                                dn.emailResidente,
                                (dn.nombrePersona == null || dn.nombrePersona.isEmpty()) ? "Residente" : dn.nombrePersona,
                                ahora,
                                validez
                        );
                } catch (Exception e) {
                        // RN01: no bloquear por fallo de correo
                        e.printStackTrace();
                }
        }

        /**
         * Carga email destinatario (residente), nombre de "persona" a la que se
         * emitió el QR, y la leyenda de validez. Usa TokenQRUtil para
         * distinguir visita (V) o usuario (U).
         */
        private DatosNotificacion cargarDatosNotificacion(int usuarioIdFallback, String tokenOrNull) throws SQLException {
                DatosNotificacion dn = new DatosNotificacion();
                dn.validezTexto = "permanente";

                try (Connection cn = Conexion.getConnection()) {

                        Integer visitaId = null;
                        Integer usuarioIdFromToken = null;

                        if (tokenOrNull != null && !tokenOrNull.trim().isEmpty()) {
                                try {
                                        visitaId = TokenQRUtil.obtenerVisitaId(tokenOrNull);
                                } catch (Exception ignore) {
                                }
                                try {
                                        usuarioIdFromToken = TokenQRUtil.obtenerUsuarioId(tokenOrNull);
                                } catch (Exception ignore) {
                                }
                        }

                        if (visitaId != null) {
                                // === Token de VISITA: persona = nombre del visitante; destinatario = correo del residente owner ===
                                Integer residenteId = null;
                                String nombreVisitante = null;
                                Integer usosV = null;
                                Timestamp expV = null;

                                final String sqlV = "SELECT nombre, residente_id, usos_restantes, expira_en FROM visitas WHERE id=? LIMIT 1";
                                try (PreparedStatement ps = cn.prepareStatement(sqlV)) {
                                        ps.setInt(1, visitaId);
                                        try (ResultSet rs = ps.executeQuery()) {
                                                if (rs.next()) {
                                                        nombreVisitante = nvlTrim(rs.getString("nombre"));
                                                        residenteId = getInteger(rs, "residente_id");
                                                        usosV = getInteger(rs, "usos_restantes");
                                                        expV = rs.getTimestamp("expira_en");
                                                }
                                        }
                                }

                                if (residenteId == null) {
                                        residenteId = usuarioIdFallback;
                                }

                                final String sqlU = "SELECT correo FROM usuarios WHERE id=? LIMIT 1";
                                try (PreparedStatement ps = cn.prepareStatement(sqlU)) {
                                        ps.setInt(1, residenteId);
                                        try (ResultSet rs = ps.executeQuery()) {
                                                if (rs.next()) {
                                                        dn.emailResidente = nvlTrim(rs.getString("correo"));
                                                }
                                        }
                                }

                                String val = formatValidez(usosV, expV);
                                if (val == null && tokenOrNull != null) {
                                        Integer usosT = null;
                                        Timestamp expT = null;
                                        final String sqlT = "SELECT usos_restantes, expira_en FROM qr_tokens WHERE token=? LIMIT 1";
                                        try (PreparedStatement ps = cn.prepareStatement(sqlT)) {
                                                ps.setString(1, tokenOrNull);
                                                try (ResultSet rs = ps.executeQuery()) {
                                                        if (rs.next()) {
                                                                usosT = getInteger(rs, "usos_restantes");
                                                                expT = rs.getTimestamp("expira_en");
                                                        }
                                                }
                                        }
                                        val = formatValidez(usosT, expT);
                                }

                                dn.nombrePersona = (nombreVisitante == null ? "Visitante" : nombreVisitante);
                                dn.validezTexto = (val == null ? "permanente" : val);
                                return dn;
                        }

                        // === Token de USUARIO (o sin token): persona = nombre del residente; destinatario = su correo ===
                        Integer usuarioId = (usuarioIdFromToken != null) ? usuarioIdFromToken : usuarioIdFallback;

                        final String sqlUsr = "SELECT correo, COALESCE(TRIM(CONCAT(nombre,' ',apellidos)),'') AS nombre "
                                + "FROM usuarios WHERE id=? LIMIT 1";
                        try (PreparedStatement ps = cn.prepareStatement(sqlUsr)) {
                                ps.setInt(1, usuarioId);
                                try (ResultSet rs = ps.executeQuery()) {
                                        if (rs.next()) {
                                                dn.emailResidente = nvlTrim(rs.getString("correo"));
                                                dn.nombrePersona = nvlTrim(rs.getString("nombre"));
                                        }
                                }
                        }

                        if (tokenOrNull != null && !tokenOrNull.trim().isEmpty()) {
                                Integer usos = null;
                                Timestamp exp = null;
                                final String sqlTk = "SELECT usos_restantes, expira_en FROM qr_tokens WHERE token=? LIMIT 1";
                                try (PreparedStatement ps = cn.prepareStatement(sqlTk)) {
                                        ps.setString(1, tokenOrNull);
                                        try (ResultSet rs = ps.executeQuery()) {
                                                if (rs.next()) {
                                                        usos = getInteger(rs, "usos_restantes");
                                                        exp = rs.getTimestamp("expira_en");
                                                }
                                        }
                                }
                                String val = formatValidez(usos, exp);
                                if (val != null) {
                                        dn.validezTexto = val;
                                }
                        }
                }

                return dn;
        }

        /* ================== Log & Estado ================== */
        private void registrarLog(String token, Integer usuarioId, String resultado,
                String motivo, String tipo, String metadataJson) {
                final String sql = "INSERT INTO accesos_log(token, usuario_id, tipo, resultado, motivo, usado_en, metadata) "
                        + "VALUES (?,?,?,?,?,NOW(),?)";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setString(1, token);
                        if (usuarioId == null) {
                                ps.setNull(2, Types.INTEGER);
                        } else {
                                ps.setInt(2, usuarioId);
                        }
                        ps.setString(3, tipo != null ? tipo : inferTipo(token));
                        ps.setString(4, resultado);
                        if (motivo == null) {
                                ps.setNull(5, Types.VARCHAR);
                        } else {
                                ps.setString(5, motivo);
                        }
                        if (metadataJson == null) {
                                ps.setNull(6, Types.LONGVARCHAR);
                        } else {
                                ps.setString(6, metadataJson);
                        }
                        ps.executeUpdate();
                } catch (SQLException e) {
                        System.err.println("[WARN] registrarLog: " + e.getMessage());
                }
        }

        private void actualizarEstado(String token, String nuevoEstado) {
                final String sql = "UPDATE qr_tokens SET estado=? WHERE token=?";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setString(1, nuevoEstado);
                        ps.setString(2, token);
                        ps.executeUpdate();
                } catch (SQLException e) {
                        System.err.println("[WARN] actualizarEstado: " + e.getMessage());
                }
        }

        private String inferTipo(String token) {
                final String sql = "SELECT tipo FROM qr_tokens WHERE token=? LIMIT 1";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setString(1, token);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? rs.getString(1) : "residente";
                        }
                } catch (SQLException e) {
                        return "residente";
                }
        }

        /* ================== Helpers ================== */
        private static String normalizarPlaca(String placa) {
                if (placa == null) {
                        return null;
                }
                String p = placa.trim().toUpperCase().replace(" ", "");
                return p.isEmpty() ? null : p;
        }

        private static String nvlTrim(String s) {
                return (s == null) ? null : (s.trim().isEmpty() ? null : s.trim());
        }

        private static Integer getInteger(ResultSet rs, String col) {
                try {
                        int v = rs.getInt(col);
                        return rs.wasNull() ? null : v;
                } catch (SQLException e) {
                        return null;
                }
        }

        private static String formatValidez(Integer usosRestantes, Timestamp expiraEn) {
                if (usosRestantes != null) {
                        return usosRestantes + " usos";
                }
                if (expiraEn != null) {
                        LocalDateTime dt = expiraEn.toLocalDateTime();
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        return "válido hasta " + dt.format(fmt);
                }
                return null;
        }
}
