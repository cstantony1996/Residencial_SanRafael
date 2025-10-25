package VehiculoDAO;

import Conexion_DB.Conexion;
import Vehiculo.Vehiculo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehiculoDAO {

    public List<Vehiculo> listarPorUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, placa, marca, modelo, color, activo, creado_en "
                + "FROM vehiculos WHERE usuario_id = ? AND activo = 1 ORDER BY id";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Vehiculo> list = new ArrayList<>();
                while (rs.next()) {
                    Vehiculo v = new Vehiculo(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            rs.getString("placa"),
                            rs.getString("marca"),
                            rs.getString("modelo"),
                            rs.getString("color"),
                            rs.getBoolean("activo")
                    );
                    v.setCreadoEn(rs.getTimestamp("creado_en"));
                    list.add(v);
                }
                return list;
            }
        }
    }

    public boolean insertar(Vehiculo v) throws SQLException {
        String sql = "INSERT INTO vehiculos (usuario_id, placa, marca, modelo, color) VALUES (?,?,?,?,?)";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, v.getUsuarioId());
            ps.setString(2, normPlaca(v.getPlaca()));
            ps.setString(3, v.getMarca());
            ps.setString(4, v.getModelo());
            ps.setString(5, v.getColor());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean actualizar(Vehiculo v) throws SQLException {
        String sql = "UPDATE vehiculos SET placa=?, marca=?, modelo=?, color=? WHERE id=? AND usuario_id=?";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, normPlaca(v.getPlaca()));
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            ps.setString(4, v.getColor());
            ps.setInt(5, v.getId());
            ps.setInt(6, v.getUsuarioId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean desactivarPorId(int id, int usuarioId) throws SQLException {
        String sql = "UPDATE vehiculos SET activo=0 WHERE id=? AND usuario_id=?";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, usuarioId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean placaDelUsuario(int usuarioId, String placa) throws SQLException {
        String sql = "SELECT 1 FROM vehiculos WHERE usuario_id=? AND placa=? AND activo=1";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, normPlaca(placa));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existePlacaActiva(String placa) throws SQLException {
        String sql = "SELECT 1 FROM vehiculos WHERE placa=? AND activo=1";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, normPlaca(placa));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarActivosPorUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vehiculos WHERE usuario_id=? AND activo=1";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static String normPlaca(String p) {
        if (p == null) {
            return null;
        }
        return p.trim().toUpperCase().replace(" ", "");
    }
}
