package Chat.dao;

import java.sql.*;
import Conexion_DB.Conexion;

public class IncidentDAO {
    
    public long crear(int residenteId, String tipo, Timestamp fechaHora, String descripcion) throws SQLException {
        String sql = "INSERT INTO incidentes(residente_id, tipo, fecha_hora, descripcion) VALUES(?,?,?,?)";
        try (Connection c = Conexion.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, residenteId);
            ps.setString(2, tipo);
            ps.setTimestamp(3, fechaHora);
            ps.setString(4, descripcion);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next())
                    return k.getLong(1);
            }
            throw new SQLException("No se pudo crear incidente");
        }
    }
    
}
