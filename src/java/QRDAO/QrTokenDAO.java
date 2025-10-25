package QRDAO;

import Conexion_DB.Conexion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla qr_tokens (ver esquema provisto por ti). Columnas: id,
 * token, tipo, usuario_id, visita_id, creado_en, expira_en, usos_restantes,
 * estado
 */
public class QrTokenDAO {

    /* ===================== Modelo ===================== */
    public static class QRToken {

        private int id;
        private String token;
        private String tipo;                 // 'residente' | 'visitante'
        private Integer usuarioId;
        private Integer visitaId;
        private LocalDateTime creadoEn;
        private LocalDateTime expiraEn;     // null = permanente
        private Integer usosRestantes;      // null = ilimitado
        private String estado;              // 'activo','invalido','revocado','usado'

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public Integer getUsuarioId() {
            return usuarioId;
        }

        public void setUsuarioId(Integer usuarioId) {
            this.usuarioId = usuarioId;
        }

        public Integer getVisitaId() {
            return visitaId;
        }

        public void setVisitaId(Integer visitaId) {
            this.visitaId = visitaId;
        }

        public LocalDateTime getCreadoEn() {
            return creadoEn;
        }

        public void setCreadoEn(LocalDateTime creadoEn) {
            this.creadoEn = creadoEn;
        }

        public LocalDateTime getExpiraEn() {
            return expiraEn;
        }

        public void setExpiraEn(LocalDateTime expiraEn) {
            this.expiraEn = expiraEn;
        }

        public Integer getUsosRestantes() {
            return usosRestantes;
        }

        public void setUsosRestantes(Integer usosRestantes) {
            this.usosRestantes = usosRestantes;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    /* Inserts*/
    /**
     * Inserta token permanente de RESIDENTE (sin expira ni usos).
     */
    public void insertResidentToken(String token, int usuarioId) throws SQLException {
        final String sql
                = "INSERT INTO qr_tokens (token, tipo, usuario_id, visita_id, creado_en, expira_en, usos_restantes, estado) "
                + "VALUES (?, 'residente', ?, NULL, NOW(), NULL, NULL, 'activo')";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        }
    }

    /**
     * Inserta token de VISITANTE por FECHA (con expira_en).
     */
    public void insertVisitTokenByDate(String token, int usuarioId, Timestamp expiraEn) throws SQLException {
        final String sql
                = "INSERT INTO qr_tokens (token, tipo, usuario_id, visita_id, creado_en, expira_en, usos_restantes, estado) "
                + "VALUES (?, 'visitante', ?, NULL, NOW(), ?, NULL, 'activo')";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, usuarioId);
            ps.setTimestamp(3, expiraEn);
            ps.executeUpdate();
        }
    }

    /**
     * Inserta token de VISITANTE por INTENTOS (usos_restantes).
     */
    public void insertVisitTokenByAttempts(String token, int usuarioId, int usos) throws SQLException {
        final String sql
                = "INSERT INTO qr_tokens (token, tipo, usuario_id, visita_id, creado_en, expira_en, usos_restantes, estado) "
                + "VALUES (?, 'visitante', ?, NULL, NOW(), NULL, ?, 'activo')";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, usuarioId);
            ps.setInt(3, usos);
            ps.executeUpdate();
        }
    }

    /**
     * Vincula un token a una visita específica.
     */
    public void linkTokenToVisit(String token, int visitaId) throws SQLException {
        final String sql = "UPDATE qr_tokens SET visita_id=? WHERE token=?";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, visitaId);
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    /* ===================== Queries ===================== */
    /**
     * Devuelve el token ACTIVO de residente más reciente del usuario (o null).
     */
    public String getActiveResidentTokenByUser(int usuarioId) throws SQLException {
        final String sql
                = "SELECT token FROM qr_tokens "
                + "WHERE usuario_id=? AND tipo='residente' AND estado='activo' "
                + "ORDER BY id DESC LIMIT 1";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Obtiene un registro completo por token.
     */
    public QRToken getByToken(String token) throws SQLException {
        final String sql
                = "SELECT id, token, tipo, usuario_id, visita_id, creado_en, expira_en, usos_restantes, estado "
                + "FROM qr_tokens WHERE token=?";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Lista tokens por usuario (más recientes primero).
     */
    public List<QRToken> listByUsuario(int usuarioId) throws SQLException {
        final String sql
                = "SELECT id, token, tipo, usuario_id, visita_id, creado_en, expira_en, usos_restantes, estado "
                + "FROM qr_tokens WHERE usuario_id=? ORDER BY id DESC";
        List<QRToken> out = new ArrayList<>();
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    /* ===================== Mutaciones de estado ===================== */
    /**
     * Revoca todos los tokens de RESIDENTE activos del usuario.
     */
    public void revokeResidentTokens(int usuarioId) throws SQLException {
        final String sql
                = "UPDATE qr_tokens SET estado='revocado' "
                + "WHERE usuario_id=? AND tipo='residente' AND estado='activo'";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        }
    }

    /**
     * Revoca tokens por visita (cancelar visita).
     */
    public void revokeByVisitId(int visitaId) throws SQLException {
        final String sql
                = "UPDATE qr_tokens SET estado='revocado' "
                + "WHERE visita_id=? AND estado='activo'";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, visitaId);
            ps.executeUpdate();
        }
    }

    /**
     * Revoca un token específico.
     */
    public void revokeByToken(String token) throws SQLException {
        final String sql = "UPDATE qr_tokens SET estado='revocado' WHERE token=? AND estado='activo'";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    /**
     * Descuenta 1 uso (para tokens por intentos). Retorna true si se descontó.
     */
    public boolean consumeAttempt(String token) throws SQLException {
        final String sql
                = "UPDATE qr_tokens SET usos_restantes=usos_restantes-1 "
                + "WHERE token=? AND estado='activo' AND usos_restantes IS NOT NULL AND usos_restantes>0";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marca 'invalido' si el token ya expiró (y estaba activo).
     */
    public int invalidateIfExpired(String token) throws SQLException {
        final String sql
                = "UPDATE qr_tokens SET estado='invalido' "
                + "WHERE token=? AND estado='activo' AND expira_en IS NOT NULL AND expira_en <= NOW()";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeUpdate();
        }
    }

    /**
     * Cambia el estado explícitamente.
     */
    public void updateEstado(String token, String nuevoEstado) throws SQLException {
        final String sql = "UPDATE qr_tokens SET estado=? WHERE token=?";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    /* ===================== Mapper ===================== */
    private QRToken mapRow(ResultSet rs) throws SQLException {
        QRToken t = new QRToken();
        t.setId(rs.getInt("id"));
        t.setToken(rs.getString("token"));
        t.setTipo(rs.getString("tipo"));

        int uid = rs.getInt("usuario_id");
        t.setUsuarioId(rs.wasNull() ? null : uid);

        int vid = rs.getInt("visita_id");
        t.setVisitaId(rs.wasNull() ? null : vid);

        Timestamp creado = rs.getTimestamp("creado_en");
        if (creado != null) {
            t.setCreadoEn(creado.toLocalDateTime());
        }

        Timestamp expira = rs.getTimestamp("expira_en");
        if (expira != null) {
            t.setExpiraEn(expira.toLocalDateTime());
        }

        int usos = rs.getInt("usos_restantes");
        t.setUsosRestantes(rs.wasNull() ? null : usos);

        t.setEstado(rs.getString("estado"));
        return t;
    }
}
