package DirectorioDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Conexion_DB.Conexion;
import modelo.DirectorioFiltro;
import modelo.DirectorioItem;

public class DirectorioDAO {

        public List<DirectorioItem> buscar(DirectorioFiltro f) throws SQLException {
                StringBuilder sql = new StringBuilder(
                        "SELECT id, dpi, nombre, apellidos, correo, rol, lote, numero_casa "
                        + "FROM usuarios WHERE estado = 'activo'" // <— solo activos
                );
                List<Object> params = new ArrayList<>();

                // Coincidencias parciales, case-insensitive
                if (noVacio(f.getNombre())) {
                        sql.append(" AND UPPER(nombre) LIKE UPPER(?)");
                        params.add("%" + f.getNombre().trim() + "%");
                }
                if (noVacio(f.getApellidos())) {
                        sql.append(" AND UPPER(apellidos) LIKE UPPER(?)");
                        params.add("%" + f.getApellidos().trim() + "%");
                }

                // Búsqueda por casa (lote + número juntos)
                if (noVacio(f.getLote()) && f.getNumeroCasa() != null) {
                        sql.append(" AND lote = ? AND numero_casa = ?");
                        params.add(f.getLote().trim());
                        params.add(f.getNumeroCasa());
                }

                sql.append(" ORDER BY apellidos, nombre");
                sql.append(" LIMIT ? OFFSET ?");

                int limit = f.getSize();
                int offset = (f.getPage() - 1) * f.getSize();

                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql.toString())) {

                        int idx = 1;
                        for (Object p : params) {
                                if (p instanceof Integer) {
                                        ps.setInt(idx++, (Integer) p);
                                } else {
                                        ps.setString(idx++, String.valueOf(p));
                                }
                        }
                        ps.setInt(idx++, limit);
                        ps.setInt(idx, offset);

                        List<DirectorioItem> out = new ArrayList<>();
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        DirectorioItem d = new DirectorioItem();
                                        d.setId(rs.getInt("id"));
                                        d.setDpi(rs.getString("dpi"));
                                        d.setNombre(rs.getString("nombre"));
                                        d.setApellidos(rs.getString("apellidos"));
                                        d.setCorreo(rs.getString("correo"));
                                        d.setRol(rs.getString("rol"));
                                        d.setLote(rs.getString("lote"));
                                        d.setNumeroCasa(rs.getObject("numero_casa") != null ? rs.getInt("numero_casa") : null);
                                        out.add(d);
                                }
                        }
                        return out;
                }
        }

        public int contar(DirectorioFiltro f) throws SQLException {
                StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM usuarios WHERE estado = 'activo'"); // <— solo activos
                List<Object> params = new ArrayList<>();

                if (noVacio(f.getNombre())) {
                        sql.append(" AND UPPER(nombre) LIKE UPPER(?)");
                        params.add("%" + f.getNombre().trim() + "%");
                }
                if (noVacio(f.getApellidos())) {
                        sql.append(" AND UPPER(apellidos) LIKE UPPER(?)");
                        params.add("%" + f.getApellidos().trim() + "%");
                }
                if (noVacio(f.getLote()) && f.getNumeroCasa() != null) {
                        sql.append(" AND lote = ? AND numero_casa = ?");
                        params.add(f.getLote().trim());
                        params.add(f.getNumeroCasa());
                }

                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql.toString())) {

                        int idx = 1;
                        for (Object p : params) {
                                if (p instanceof Integer) {
                                        ps.setInt(idx++, (Integer) p);
                                } else {
                                        ps.setString(idx++, String.valueOf(p));
                                }
                        }

                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next() ? rs.getInt(1) : 0;
                        }
                }
        }

        private boolean noVacio(String s) {
                return s != null && !s.trim().isEmpty();
        }
}
