package Chat.dao;

import java.sql.*;
import java.util.*;

import Conexion_DB.Conexion;

public class UserLookupDAO {

    public static class UserCore {

        public int id;
        public String nombre;
        public String correo;
        public String lote;
        public String numeroCasa;
        public boolean activo;
        public String rol; // "RESIDENTE" | "AGENTE"/"GUARDIA"
    }

    // Devuelve el usuario por id 
    public UserCore getById(int id) {
        final String sql = "SELECT id, nombre, correo, lote, numero_casa, UPPER(rol) AS rol FROM usuarios WHERE id=?";
        try (Connection c = Conexion.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserCore u = new UserCore();
                    u.id = rs.getInt(1);
                    u.nombre = rs.getString(2);
                    u.correo = rs.getString(3);
                    u.lote = rs.getString(4);
                    u.numeroCasa = rs.getString(5);
                    u.rol = rs.getString(6);   // "AGENTE", "RESIDENTE", "ADMINISTRADOR" (en mayúsculas)
                    u.activo = true;           // compatibilidad
                    return u;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean esGuardiaRegistradoActivo(int id) {
        // considera 'agente', 'guardia' o 'seguridad' sin importar mayúsculas
        final String sql = "SELECT 1 FROM usuarios WHERE id=? AND UPPER(rol) IN ('AGENTE','GUARDIA','SEGURIDAD')";
        try (Connection c = Conexion.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public java.util.List<UserCore> listarGuardiasActivos() {
        final String sql
                = "SELECT id, nombre, correo, lote, numero_casa, UPPER(rol) AS rol "
                + "FROM usuarios WHERE UPPER(rol) IN ('AGENTE','GUARDIA','SEGURIDAD') "
                + "ORDER BY nombre";
        java.util.List<UserCore> out = new java.util.ArrayList<>();
        try (Connection c = Conexion.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserCore u = new UserCore();
                    u.id = rs.getInt(1);
                    u.nombre = rs.getString(2);
                    u.correo = rs.getString(3);
                    u.lote = rs.getString(4);
                    u.numeroCasa = rs.getString(5);
                    u.rol = rs.getString(6);
                    u.activo = true;
                    out.add(u);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }

}
