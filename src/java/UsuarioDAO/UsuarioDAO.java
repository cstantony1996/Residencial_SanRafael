package UsuarioDAO;

import java.sql.*;
import Conexion_DB.Conexion;
import Usuario.Usuario;
import java.util.List;
import java.util.ArrayList;

public class UsuarioDAO {

        public List<Usuario> obtenerTodos() throws SQLException {
                List<Usuario> usuarios = new ArrayList<>();
                String sql = "SELECT id, dpi, nombre, apellidos, correo, rol, lote, numero_casa, creado_en, qr_path "
                        + "FROM usuarios WHERE estado = 'activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                                usuarios.add(mapRow(rs));
                        }
                }
                return usuarios;
        }

        public Usuario obtenerPorId(int id) throws SQLException {
                String sql = "SELECT id, dpi, nombre, apellidos, correo, rol, lote, numero_casa, creado_en, qr_path "
                        + "FROM usuarios WHERE id = ? AND estado = 'activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? mapRow(rs) : null;
                        }
                }
        }

        // obtenerUsuario(dpi)
        public Usuario obtenerUsuario(String dpi) throws SQLException {
                String sql = "SELECT id, dpi, nombre, apellidos, correo, rol, lote, numero_casa, creado_en, qr_path "
                        + "FROM usuarios WHERE dpi = ? AND estado = 'activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, dpi);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? mapRow(rs) : null;
                        }
                }
        }

        // existeDpiOCorreo(dpi, correo)
        public boolean existeDpiOCorreo(String dpi, String correo) throws SQLException {
                String sql = "SELECT 1 FROM usuarios WHERE (dpi = ? OR correo = ?) AND estado = 'activo' LIMIT 1";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, dpi);
                        ps.setString(2, correo);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next();
                        }
                }
        }

        public boolean esAgentePorId(int idUsuario) {
                String sql = "SELECT 1 FROM usuarios WHERE id = ? AND LOWER(rol) = 'agente' AND estado = 'activo' LIMIT 1";
                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, idUsuario);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next();
                        }
                } catch (SQLException e) {
                        throw new RuntimeException("Error verificando rol agente", e);
                }
        }

        // insertarUsuario(u) -> boolean
        public boolean insertarUsuario(Usuario usuario) throws SQLException {
                String sql = "INSERT INTO usuarios (dpi, nombre, apellidos, correo, contraseña, rol, lote, numero_casa, creado_en, qr_path, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, 'activo')";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {

                        ps.setString(1, usuario.getDpi());
                        ps.setString(2, usuario.getNombre());
                        ps.setString(3, usuario.getApellidos());
                        ps.setString(4, usuario.getCorreo());
                        ps.setString(5, usuario.getContraseña());
                        ps.setString(6, usuario.getRol());

                        if (usuario.getRol() != null && (usuario.getRol().equals("residente") || usuario.getRol().equals("administrador"))) {
                                ps.setString(7, usuario.getLote());
                                if (usuario.getNumeroCasa() != null) {
                                        ps.setInt(8, usuario.getNumeroCasa());
                                } else {
                                        ps.setNull(8, Types.INTEGER);
                                }
                        } else {
                                ps.setNull(7, Types.VARCHAR);
                                ps.setNull(8, Types.INTEGER);
                        }

                        ps.setString(9, usuario.getRutaQR()); // puede ser null
                        return ps.executeUpdate() > 0;
                }
        }

        // actualizarUsuario(u) -> boolean
        public boolean actualizarUsuario(Usuario usuario) throws SQLException {
                String sql = "UPDATE usuarios "
                        + "SET dpi=?, nombre=?, apellidos=?, correo=?, rol=?, lote=?, numero_casa=?, qr_path=? "
                        + "WHERE id=?";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {

                        ps.setString(1, usuario.getDpi());
                        ps.setString(2, usuario.getNombre());
                        ps.setString(3, usuario.getApellidos());
                        ps.setString(4, usuario.getCorreo());
                        ps.setString(5, usuario.getRol());

                        if (usuario.getRol() != null && (usuario.getRol().equals("residente") || usuario.getRol().equals("administrador"))) {
                                ps.setString(6, usuario.getLote());
                                if (usuario.getNumeroCasa() != null) {
                                        ps.setInt(7, usuario.getNumeroCasa());
                                } else {
                                        ps.setNull(7, Types.INTEGER);
                                }
                        } else {
                                ps.setNull(6, Types.VARCHAR);
                                ps.setNull(7, Types.INTEGER);
                        }

                        ps.setString(8, usuario.getRutaQR()); // puede venir null o nueva ruta
                        ps.setInt(9, usuario.getId());
                        return ps.executeUpdate() > 0;
                }
        }

        // actualizarRutaQR(id, ruta) -> boolean
        public boolean actualizarRutaQR(int id, String rutaQR) throws SQLException {
                String sql = "UPDATE usuarios SET qr_path=? WHERE id=?";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, rutaQR);
                        ps.setInt(2, id);
                        return ps.executeUpdate() > 0;
                }
        }

        // obtenerIdPorDpi(dpi)
        public Integer obtenerIdPorDpi(String dpi) throws SQLException {
                String sql = "SELECT id FROM usuarios WHERE dpi = ?";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, dpi);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? rs.getInt("id") : null;
                        }
                }
        }

        // eliminarUsuarioPorDpi(dpi) -> boolean
        public boolean eliminarUsuarioPorDpi(String dpi) throws SQLException {
                String sql = "UPDATE usuarios "
                        + "SET estado = 'inactivo', eliminado_en = NOW() "
                        + "WHERE dpi = ? AND estado = 'activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, dpi);
                        return ps.executeUpdate() > 0;
                }
        }

        public Usuario obtenerPorCorreoYContrasena(String correo, String contrasena) throws SQLException {
                String sql = "SELECT id, dpi, nombre, apellidos, correo, rol, lote, numero_casa, creado_en, qr_path "
                        + "FROM usuarios WHERE correo = ? AND contraseña = ? AND estado = 'activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, correo);
                        ps.setString(2, contrasena);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? mapRow(rs) : null;
                        }
                }
        }

        public void insertar(Usuario usuario) throws SQLException {
                insertarUsuario(usuario);
        }

        public void actualizar(Usuario usuario) throws SQLException {
                actualizarUsuario(usuario);
        }

        public void eliminar(int id) throws SQLException {
                String sql = "UPDATE usuarios SET estado = 'inactivo', eliminado_en = NOW() "
                        + "WHERE id = ? AND estado = 'activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                }
        }

        //elimina por DPI registrando quién lo hizo
        public boolean eliminarUsuarioPorDpiConActor(String dpi, String actor) throws SQLException {
                String sql = "UPDATE usuarios "
                        + "SET estado='inactivo', eliminado_en=NOW(), eliminado_por=? "
                        + "WHERE dpi=? AND estado='activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, actor);   // puede ser null -> quedará NULL
                        ps.setString(2, dpi);
                        return ps.executeUpdate() > 0;
                }
        }

       //elimina por ID registrando quién lo hizo
        public boolean eliminarPorIdConActor(int id, String actor) throws SQLException {
                String sql = "UPDATE usuarios "
                        + "SET estado='inactivo', eliminado_en=NOW(), eliminado_por=? "
                        + "WHERE id=? AND estado='activo'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, actor);
                        ps.setInt(2, id);
                        return ps.executeUpdate() > 0;
                }
        }

        /* ========== NUEVOS MÉTODOS: correos por rol / administradores ========== */
        public List<String> obtenerCorreosPorRol(String rol) throws SQLException {
                List<String> correos = new ArrayList<>();
                String sql = "SELECT DISTINCT correo FROM usuarios "
                        + "WHERE LOWER(rol) = LOWER(?) "
                        + "AND correo IS NOT NULL "
                        + "AND TRIM(correo) <> ''";

                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, rol);
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        correos.add(rs.getString("correo"));
                                }
                        }
                }
                return correos;
        }

        public List<String> obtenerCorreosAdministradores() throws SQLException {
                return obtenerCorreosPorRol("administrador");
        }

        /* ===================== HELPERS ===================== */
        private Usuario mapRow(ResultSet rs) throws SQLException {
                Usuario u = new Usuario(
                        rs.getInt("id"),
                        rs.getString("dpi"),
                        rs.getString("nombre"),
                        rs.getString("apellidos"),
                        rs.getString("correo"),
                        "", // no exponer contraseña
                        rs.getString("rol"),
                        rs.getString("lote"),
                        rs.getObject("numero_casa") != null ? rs.getInt("numero_casa") : null
                );
                try {
                        Timestamp ts = rs.getTimestamp("creado_en");
                        if (ts != null) {
                                u.setFechaCreacion(ts);
                        }
                } catch (SQLException ignore) {
                }
                try {
                        String qr = rs.getString("qr_path");
                        u.setRutaQR(qr);
                } catch (SQLException ignore) {
                }
                return u;
        }
}
