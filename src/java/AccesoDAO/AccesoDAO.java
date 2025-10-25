package AccesoDAO;

import java.sql.*;
import Conexion_DB.Conexion;

public class AccesoDAO {

    // LÍMITES típicos en BD (ajústalos a tu DDL real)
    private static final int MAX_IP = 45;   // IPv4/IPv6
    private static final int MAX_USER_AGENT = 255;  // navegadores largos
    // Si datos_qr es VARCHAR en tu esquema, define un límite (p.ej. 1000).
    // Si es TEXT/LONGTEXT no necesitas cortar, pero lo dejo por seguridad:
    private static final int MAX_DATOS_QR = 4000;

    public long registrarAcceso(Integer usuarioId, String tipo, int puntoControlId,
            String resultado, String motivo, String direccion,
            String datosQR, Integer guardiaUsuarioId,
            String ip, String userAgent) throws SQLException {

        final String sql = "INSERT INTO accesos("
                + "usuario_id, tipo, punto_control_id, resultado, motivo_denegacion, "
                + "direccion, datos_qr, guardia_usuario_id, ip_origen, user_agent"
                + ") VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1 usuario_id
            if (usuarioId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, usuarioId);
            }
            // 2 tipo
            ps.setString(2, tipo);
            // 3 punto_control_id
            ps.setInt(3, puntoControlId);
            // 4 resultado
            ps.setString(4, resultado);
            // 5 motivo_denegacion
            if (motivo == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, motivo);
            }
            // 6 direccion ("entrada" | "salida")
            ps.setString(6, direccion);
            // 7 datos_qr (puede ser grande)
            if (datosQR == null) {
                ps.setNull(7, Types.LONGVARCHAR);
            } else {
                ps.setString(7, cut(datosQR, MAX_DATOS_QR));
            }
            // 8 guardia_usuario_id
            if (guardiaUsuarioId == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, guardiaUsuarioId);
            }
            // 9 ip_origen
            ps.setString(9, cut(ip, MAX_IP));
            // 10 user_agent
            ps.setString(10, cut(userAgent, MAX_USER_AGENT));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    // --- helpers ---
    private static String cut(String s, int max) {
        if (s == null) {
            return null;
        }
        return (s.length() > max) ? s.substring(0, max) : s;
    }
}
