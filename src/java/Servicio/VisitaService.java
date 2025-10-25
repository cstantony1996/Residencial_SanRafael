package Servicio;

import Conexion_DB.Conexion;
import Utils.TokenQRUtil;
import Utils.QRCodeUtil;
import Utils.CorreoUtil;
import modelo.Visita;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class VisitaService {

    /* ===================== Crear visita + notificar ===================== */
    public Visita crearVisitaYNotificar(
            int residenteId,
            String nombreVisitante,
            String dpi,
            String correoVisitante,
            String tipoQr, // "intentos" | "tiempo"
            Integer intentos, // si "intentos"
            Timestamp expira, // si "tiempo"
            String baseAppUrl, // por si luego quieres armar link
            String nombreResidente,
            String correoResidente
    ) throws Exception {

        if (nombreVisitante == null || nombreVisitante.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del visitante es obligatorio.");
        }
        if (correoVisitante == null || !correoVisitante.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Correo del visitante inválido.");
        }
        if (tipoQr == null || (!tipoQr.equalsIgnoreCase("intentos") && !tipoQr.equalsIgnoreCase("tiempo"))) {
            throw new IllegalArgumentException("Tipo de QR inválido.");
        }
        if ("intentos".equalsIgnoreCase(tipoQr) && (intentos == null || intentos < 2)) {
            throw new IllegalArgumentException("Intentos debe ser ≥ 2.");
        }
        if ("tiempo".equalsIgnoreCase(tipoQr) && (expira == null || expira.toInstant().isBefore(java.time.Instant.now()))) {
            throw new IllegalArgumentException("Fecha/hora de expiración inválida.");
        }

        Connection cn = null;
        try {
            cn = Conexion.getConnection();
            cn.setAutoCommit(false);

            // 1) Insertar en visitas (usa columnas reales: nombre, correo, etc.)
            long visitaId;
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO visitas (residente_id, nombre, dpi, correo, tipo_qr, intentos, expira_en, estado) "
                    + "VALUES (?,?,?,?,?,?,?, 'activo')",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, residenteId);
                ps.setString(2, nombreVisitante.trim());
                if (dpi == null || dpi.trim().isEmpty()) {
                    ps.setNull(3, Types.VARCHAR);
                } else {
                    ps.setString(3, dpi.trim());
                }
                ps.setString(4, correoVisitante.trim());
                ps.setString(5, tipoQr.toLowerCase());

                if ("intentos".equalsIgnoreCase(tipoQr)) {
                    ps.setInt(6, intentos);
                    ps.setNull(7, Types.TIMESTAMP);
                } else {
                    ps.setNull(6, Types.INTEGER);
                    ps.setTimestamp(7, expira);
                }

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("No se obtuvo ID de visita.");
                    }
                    visitaId = rs.getLong(1);
                }
            }

            // 2) Generar token de visita
            long expEpoch = ("tiempo".equalsIgnoreCase(tipoQr) && expira != null)
                    ? expira.getTime() / 1000L
                    : 0L;
            String token = TokenQRUtil.generarTokenVisita((int) visitaId, expEpoch);

            // 3) Guardar token en visitas
            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE visitas SET token=? WHERE id=?")) {
                ps.setString(1, token);
                ps.setLong(2, visitaId);
                ps.executeUpdate();
            }

            // 4) Registrar también en qr_tokens (para control de usos/expiración)
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO qr_tokens(token, tipo, usuario_id, visita_id, expira_en, usos_restantes, estado, creado_en) "
                    + "VALUES (?,?,?,?,?,?, 'activo', NOW())")) {
                ps.setString(1, token);
                ps.setString(2, "visitante");
                ps.setInt(3, residenteId);
                ps.setLong(4, visitaId);
                if ("tiempo".equalsIgnoreCase(tipoQr)) {
                    ps.setTimestamp(5, expira);
                    ps.setNull(6, Types.INTEGER);
                } else {
                    ps.setNull(5, Types.TIMESTAMP);
                    ps.setInt(6, intentos);
                }
                ps.executeUpdate();
            }

            cn.commit();

            // 5) Construir texto de validez
            String validezTexto = buildValidezTexto(tipoQr, intentos, expira);

            // 6) Generar PNG del QR y enviar correos
            String rutaPng = QRCodeUtil.generarQRDesdeToken(token);

            // Correo al visitante (con QR adjunto)
            CorreoUtil.enviarQRVisitaAVisitante(
                    correoVisitante, nombreVisitante, nombreResidente, rutaPng, validezTexto);

            // Aviso al residente (sin adjunto)
            CorreoUtil.enviarAvisoResidenteVisita(
                    correoResidente, nombreResidente, nombreVisitante, validezTexto);

            // 7) devolver entidad
            Visita v = new Visita();
            v.setId((int) visitaId);
            v.setResidenteId(residenteId);
            v.setNombre(nombreVisitante);
            v.setDpi(dpi);
            v.setCorreo(correoVisitante);
            v.setTipoQr(tipoQr.toLowerCase());
            v.setIntentos("intentos".equalsIgnoreCase(tipoQr) ? intentos : null);
            v.setExpiraEn("tiempo".equalsIgnoreCase(tipoQr) ? expira : null);
            v.setToken(token);
            v.setEstado("activo");
            return v;

        } catch (Exception ex) {
            if (cn != null) {
                try {
                    cn.rollback();
                } catch (Exception ignore) {
                }
            }
            throw ex;
        } finally {
            if (cn != null) {
                try {
                    cn.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /* ===================== Cancelar visita ===================== */
    public boolean cancelarVisita(long visitaId, int residenteId) throws SQLException {
        Connection cn = null;
        try {
            cn = Conexion.getConnection();
            cn.setAutoCommit(false);

            String token = null;
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT token FROM visitas WHERE id=? AND residente_id=? AND estado='activo' FOR UPDATE")) {
                ps.setLong(1, visitaId);
                ps.setInt(2, residenteId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        token = rs.getString("token");
                    } else {
                        cn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE visitas SET estado='cancelado', cancelado_en=NOW() WHERE id=?")) {
                ps.setLong(1, visitaId);
                ps.executeUpdate();
            }

            if (token != null && !token.trim().isEmpty()) {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE qr_tokens SET estado='revocado' WHERE token=? AND estado='activo'")) {
                    ps.setString(1, token);
                    ps.executeUpdate();
                }
            }

            cn.commit();
            return true;
        } catch (SQLException e) {
            if (cn != null) {
                try {
                    cn.rollback();
                } catch (Exception ignore) {
                }
            }
            throw e;
        } finally {
            if (cn != null) {
                try {
                    cn.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /* ===================== Helpers ===================== */
    private static String buildValidezTexto(String tipoQr, Integer intentos, Timestamp expira) {
        if ("intentos".equalsIgnoreCase(tipoQr) && intentos != null) {
            return intentos + (intentos == 1 ? " intento" : " intentos");
        }
        if ("tiempo".equalsIgnoreCase(tipoQr) && expira != null) {
            LocalDateTime dt = LocalDateTime.ofInstant(expira.toInstant(), ZoneId.systemDefault());
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return "válido hasta " + dt.format(fmt);
        }
        return "permanente";
    }
}
