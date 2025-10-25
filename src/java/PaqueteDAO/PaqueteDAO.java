package PaqueteDAO;

import Conexion_DB.Conexion;
import modelo.entidad.Paquete;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaqueteDAO {

        public int crearRecepcion(Paquete p) {
                String sql = "INSERT INTO paquetes (numero_guia, id_usuario_dest, fecha_recepcion, id_agente_receptor, observaciones) "
                        + "VALUES (?, ?, NOW(), ?, ?)";

                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, p.getNumeroGuia());
                        ps.setInt(2, p.getIdUsuarioDest());
                        if (p.getIdAgenteReceptor() == null) {
                                ps.setNull(3, Types.INTEGER);
                        } else {
                                ps.setInt(3, p.getIdAgenteReceptor());
                        }
                        ps.setString(4, p.getObservaciones());
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) {
                                        return rs.getInt(1);
                                }
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error al registrar recepción de paquete", e);
                }
                return 0;
        }

        public List<Paquete> listarPendientes(String filtro) {
                String base
                        = "SELECT p.id_paquete, p.numero_guia, p.id_usuario_dest, p.fecha_recepcion, "
                        + "       u.nombre AS nombre, u.apellidos AS apellidos, u.numero_casa, u.lote "
                        + "FROM paquetes p "
                        + "JOIN usuarios u ON u.id = p.id_usuario_dest "
                        + "WHERE (p.entregado = 0 OR p.entregado IS NULL) "
                        + // <-- clave
                        "  AND (p.fecha_entrega IS NULL)";                      // <-- doble seguro

                String order = " ORDER BY p.fecha_recepcion DESC";
                StringBuilder sql = new StringBuilder(base);
                List<Paquete> lista = new ArrayList<>();

                if (filtro != null && !filtro.trim().isEmpty()) {
                        sql.append(" AND (p.numero_guia LIKE ? OR u.nombre LIKE ? OR u.apellidos LIKE ? OR u.numero_casa LIKE ? OR u.lote LIKE ?)");
                }
                sql.append(order);

                try (Connection cn = Conexion_DB.Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql.toString())) {

                        if (filtro != null && !filtro.trim().isEmpty()) {
                                String like = "%" + filtro.trim() + "%";
                                ps.setString(1, like);
                                ps.setString(2, like);
                                ps.setString(3, like);
                                ps.setString(4, like);
                                ps.setString(5, like);
                        }

                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        Paquete p = new Paquete();
                                        p.setIdPaquete(rs.getInt("id_paquete"));
                                        p.setNumeroGuia(rs.getString("numero_guia"));
                                        p.setIdUsuarioDest(rs.getInt("id_usuario_dest"));
                                        p.setFechaRecepcion(rs.getTimestamp("fecha_recepcion"));
                                        p.setNombreDestinatario(rs.getString("nombre"));
                                        p.setApellidosDestinatario(rs.getString("apellidos"));
                                        p.setNumeroCasa((Integer) rs.getObject("numero_casa"));
                                        p.setLote(rs.getString("lote"));
                                        lista.add(p);
                                }
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error al listar pendientes", e);
                }
                return lista;
        }

// PaqueteDAO.java
        public boolean marcarEntregado(int idPaquete, int idAgente) {
                final String sql
                        = "UPDATE paquetes "
                        + "SET id_agente_entrega = ?, "
                        + "    fecha_entrega = NOW(), "
                        + "    entregado = 1 "
                        + // <- clave
                        "WHERE id_paquete = ? "
                        + "  AND (entregado = 0 OR entregado IS NULL)";

                try (Connection cn = Conexion_DB.Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, idAgente);
                        ps.setInt(2, idPaquete);
                        return ps.executeUpdate() > 0;
                } catch (SQLException e) {
                        throw new RuntimeException("Error marcando entrega: " + e.getMessage(), e);
                }
        }

        public java.sql.Timestamp marcarEntregadoYRetornarFecha(int idPaquete, int idAgente) {
                final String UPD
                        = "UPDATE paquetes "
                        + "SET id_agente_entrega=?, fecha_entrega=NOW() "
                        + "WHERE id_paquete=? AND fecha_entrega IS NULL";
                final String SEL
                        = "SELECT fecha_entrega FROM paquetes WHERE id_paquete=?";

                try (Connection cn = Conexion_DB.Conexion.getConnection()) {
                        cn.setAutoCommit(false);
                        try (PreparedStatement ps = cn.prepareStatement(UPD)) {
                                ps.setInt(1, idAgente);
                                ps.setInt(2, idPaquete);
                                int n = ps.executeUpdate();
                                if (n == 0) {
                                        cn.rollback();
                                        return null;
                                } // ya estaba entregado o no existe

                                try (PreparedStatement ps2 = cn.prepareStatement(SEL)) {
                                        ps2.setInt(1, idPaquete);
                                        try (ResultSet rs = ps2.executeQuery()) {
                                                cn.commit();
                                                return rs.next() ? rs.getTimestamp(1) : null;
                                        }
                                }
                        } catch (SQLException e) {
                                cn.rollback();
                                throw e;
                        } finally {
                                cn.setAutoCommit(true);
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error marcando entrega: " + e.getMessage(), e);
                }
        }

}
