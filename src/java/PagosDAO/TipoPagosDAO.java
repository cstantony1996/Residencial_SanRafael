package PagosDAO;

import java.sql.*;
import java.util.*;
import modelo.TipoPago;
import Conexion_DB.Conexion;

public class TipoPagosDAO {

        public List<TipoPago> listarActivos() {
                String sql = "SELECT id, codigo, nombre, monto_base, recurrente, activo FROM catalogo_tipo_pago WHERE activo=1 ORDER BY nombre";
                List<TipoPago> list = new ArrayList<>();
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                                list.add(map(rs));
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error listanto tipos de pago", e);
                }

                return list;
        }

        public TipoPago buscarPorId(int id) {
                String sql = "SELECT id, codigo, nombre, monto_base, recurrente, activo FROM catalogo_tipo_pago WHERE id=?";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        return map(rs);
                                }
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error buscando tipo de pago", e);
                }

                return null;
        }

        public TipoPago buscarPorCodigo(String codigo) {
                String sql = "SELECT id, codigo, nombre, monto_base, recurrente, activo FROM catalogo_tipo_pago WHERE codigo=?";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setString(1, codigo);
                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        return map(rs);
                                }
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error buscando tipo por código", e);
                }

                return null;
        }

        private TipoPago map(ResultSet rs) throws SQLException {
                TipoPago t = new TipoPago();
                t.setId(rs.getInt("id"));
                t.setCodigo(rs.getString("codigo"));
                t.setNombre(rs.getString("nombre"));
                t.setMontoBase(rs.getBigDecimal("monto_base"));
                t.setRecurrente(rs.getBoolean("recurrente"));
                t.setActivo(rs.getBoolean("activo"));
                return t;
        }

}
