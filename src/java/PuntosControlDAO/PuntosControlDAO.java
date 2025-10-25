package PuntosControlDAO;

import Conexion_DB.Conexion;
import PuntosControl.Punto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PuntosControlDAO {

    /**
     * Lista puntos activos para poblar el combo de la garita.
     */
    public List<Punto> listarActivos() throws SQLException {
        String sql = "SELECT id, nombre, tipo FROM puntos_control WHERE activo=1 ORDER BY id";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            List<Punto> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Punto(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("tipo")
                ));
            }
            return list;
        }
    }

    /**
     * Verifica existencia (y activo=1) para evitar romper la FK en accesos.
     */
    public boolean existe(int id) throws SQLException {
        String sql = "SELECT 1 FROM puntos_control WHERE id=? AND activo=1";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // (Opcional) obtener por id
    public Punto obtener(int id) throws SQLException {
        String sql = "SELECT id, nombre, tipo FROM puntos_control WHERE id=?";
        try (Connection cn = Conexion.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Punto(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("tipo")
                    );
                }
                return null;
            }
        }
    }
}
