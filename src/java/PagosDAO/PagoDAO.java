package PagosDAO;

import Conexion_DB.Conexion;
import modelo.Pago;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {

        public int insert(Pago p) throws SQLException {
                final String sql = "INSERT INTO pagos "
                        + "(usuario_id, tipo_pago_id, mes_anio, monto_base, mora, fecha_pago, observaciones, tarjeta_masked, auth_code, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                        int i = 1;
                        ps.setInt(i++, p.getUsuarioId());
                        ps.setInt(i++, p.getTipoPagoId());

                        if (p.getMesAnio() == null || p.getMesAnio().trim().isEmpty()) {
                                ps.setNull(i++, Types.VARCHAR);
                        } else {
                                ps.setString(i++, p.getMesAnio());
                        }

                        ps.setDouble(i++, p.getMontoBase());
                        ps.setDouble(i++, p.getMora());

                        // fechaPago: si viene null, usamos ahora
                        java.time.LocalDateTime fp = p.getFechaPago();
                        if (fp == null) {
                                fp = java.time.LocalDateTime.now();
                        }
                        ps.setTimestamp(i++, Timestamp.valueOf(fp));

                        ps.setString(i++, p.getObservaciones());
                        ps.setString(i++, p.getTarjetaMasked());
                        ps.setString(i++, p.getAuthCode());
                        ps.setString(i++, p.getStatus().name());

                        ps.executeUpdate();

                        try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) {
                                        return rs.getInt(1);
                                }
                        }
                }
                return 0;
        }

        /**
         * Último mes cobrado de mantenimiento (usa catálogo por código)
         */
        public String findUltimoMesPagoMantenimiento(int usuarioId) throws SQLException {
                final String sql
                        = "SELECT p.mes_anio FROM pagos p "
                        + "JOIN catalogo_tipo_pago t ON t.id = p.tipo_pago_id "
                        + "WHERE p.usuario_id=? AND t.codigo='MANTENIMIENTO' AND p.mes_anio IS NOT NULL "
                        + "ORDER BY p.mes_anio DESC LIMIT 1";
                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setInt(1, usuarioId);
                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        return rs.getString(1);
                                }
                        }
                }
                return null;
        }

        /**
         * Listado con JOIN (para ver nombre/código del tipo).
         */
        public List<Pago> listByUsuario(Integer usuarioId, String tipoCodigo, String desde, String hasta, int size, int offset) throws SQLException {
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT p.*, t.codigo AS tipo_codigo, t.nombre AS tipo_nombre ");
                sb.append("FROM pagos p JOIN catalogo_tipo_pago t ON t.id = p.tipo_pago_id WHERE 1=1 ");
                List<Object> params = new ArrayList<>();

                if (usuarioId != null) {
                        sb.append(" AND p.usuario_id = ?");
                        params.add(usuarioId);
                }
                if (tipoCodigo != null && !tipoCodigo.trim().isEmpty()) {
                        sb.append(" AND t.codigo = ?");
                        params.add(tipoCodigo.trim().toUpperCase());
                }
                if (desde != null && !desde.trim().isEmpty()) {
                        sb.append(" AND p.fecha_pago >= ?");
                        params.add(Timestamp.valueOf(desde.trim() + " 00:00:00"));
                }
                if (hasta != null && !hasta.trim().isEmpty()) {
                        sb.append(" AND p.fecha_pago <= ?");
                        params.add(Timestamp.valueOf(hasta.trim() + " 23:59:59"));
                }

                sb.append(" ORDER BY p.fecha_pago DESC LIMIT ? OFFSET ?");
                params.add(size);
                params.add(offset);

                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sb.toString())) {
                        int i = 1;
                        for (Object o : params) {
                                if (o instanceof Integer) {
                                        ps.setInt(i++, (Integer) o);
                                } else if (o instanceof Timestamp) {
                                        ps.setTimestamp(i++, (Timestamp) o);
                                } else {
                                        ps.setString(i++, o.toString());
                                }
                        }
                        List<Pago> out = new ArrayList<>();
                        try (ResultSet rs = ps.executeQuery()) {

                                while (rs.next()) {
                                        Pago p = new Pago();
                                        p.setId(rs.getInt("id"));
                                        p.setUsuarioId(rs.getInt("usuario_id"));
                                        p.setTipoPagoId(rs.getInt("tipo_pago_id"));
                                        p.setTipo(rs.getString("tipo_codigo"));         // código
                                        p.setTipoNombre(rs.getString("tipo_nombre"));   // <-- añade esto
                                        p.setMesAnio(rs.getString("mes_anio"));
                                        p.setMontoBase(rs.getDouble("monto_base"));
                                        p.setMora(rs.getDouble("mora"));
                                        p.setTotal(rs.getDouble("total"));
                                        p.setFechaPago(rs.getTimestamp("fecha_pago").toLocalDateTime());
                                        p.setObservaciones(rs.getString("observaciones"));
                                        p.setTarjetaMasked(rs.getString("tarjeta_masked"));
                                        p.setAuthCode(rs.getString("auth_code"));
                                        p.setStatus(Pago.Status.valueOf(rs.getString("status")));
                                        out.add(p);
                                }

                        }
                        return out;
                }
        }

        // === NUEVO: lista una página de pagos sin filtros (size, offset) ===
        public List<Pago> listPage(int size, int offset) throws SQLException {
                String sql = "SELECT p.*, t.codigo AS tipo_codigo, t.nombre AS tipo_nombre "
                        + "FROM pagos p JOIN catalogo_tipo_pago t ON t.id = p.tipo_pago_id "
                        + "ORDER BY p.fecha_pago DESC "
                        + "LIMIT ? OFFSET ?";
                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql)) {

                        ps.setInt(1, size);
                        ps.setInt(2, offset);

                        List<Pago> out = new ArrayList<>();
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        Pago p = new Pago();
                                        p.setId(rs.getInt("id"));
                                        p.setUsuarioId(rs.getInt("usuario_id"));
                                        p.setTipoPagoId(rs.getInt("tipo_pago_id"));
                                        p.setTipo(rs.getString("tipo_codigo"));
                                        p.setTipoNombre(rs.getString("tipo_nombre"));
                                        p.setMesAnio(rs.getString("mes_anio"));
                                        p.setMontoBase(rs.getDouble("monto_base"));
                                        p.setMora(rs.getDouble("mora"));
                                        // Si no tienes columna 'total' en BD, calcula:
                                        p.setTotal(p.getMontoBase() + p.getMora());
                                        java.sql.Timestamp ts = rs.getTimestamp("fecha_pago");
                                        p.setFechaPago(ts != null ? ts.toLocalDateTime() : null);
                                        p.setObservaciones(rs.getString("observaciones"));
                                        p.setTarjetaMasked(rs.getString("tarjeta_masked"));
                                        p.setAuthCode(rs.getString("auth_code"));
                                        p.setStatus(Pago.Status.valueOf(rs.getString("status")));
                                        out.add(p);
                                }
                        }
                        return out;
                }
        }

// === NUEVO: cuenta total de filas para calcular páginas ===
        public int countAll() throws SQLException {
                String sql = "SELECT COUNT(*) FROM pagos";
                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                                return rs.getInt(1);
                        }
                        return 0;
                }
        }

}
