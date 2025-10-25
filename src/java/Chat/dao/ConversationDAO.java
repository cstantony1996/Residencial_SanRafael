package Chat.dao;

import Chat.model.Conversation;
import Conexion_DB.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    public boolean existeEntre(int residenteId, int agenteId) {
        final String sql = "SELECT 1 FROM conversaciones WHERE residente_id=? AND agente_id=? AND estado='ABIERTA'";
        try (Connection c = Conexion.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            ps.setInt(2, agenteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Conversation crear(int residenteId, int agenteId) {
        final String sql = "INSERT INTO conversaciones(residente_id, agente_id, estado, creado_en)" + "VALUES (?,?, 'ABIERTA', NOW())";
        try (Connection c = Conexion.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, residenteId);
            ps.setInt(2, agenteId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Conversation cv = new Conversation();
                    cv.id = rs.getLong(1);
                    cv.residenteId = residenteId;
                    cv.agenteId = agenteId;
                    cv.estado = "ABIERTA";
                    return cv;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("No se pudo crear la conversación");
    }

    public Conversation getById(long id) {
        final String sql = "SELECT id,residente_id,agente_id,estado FROM conversaciones WHERE id=?";
        try (Connection c = Conexion.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Conversation cv = new Conversation();
                    cv.id = rs.getLong(1);
                    cv.residenteId = rs.getInt(2);
                    cv.agenteId = rs.getInt(3);
                    cv.estado = rs.getString(4);
                    return cv;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean usuarioPertenece(long convId, int userId) {
        final String sql = "SELECT 1 FROM conversaciones WHERE id=? AND (residente_id=? OR agente_id=?)";
        try (Connection c = Conexion.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, convId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // opcional para recibos de lectura (si aplicaste la mejora):
    public void updateReadUpTo(long convId, boolean byResidente, long messageId) {
        final String col = byResidente ? "residente_read_upto" : "agente_read_upto";
        final String sql = "UPDATE conversaciones SET " + col + "=? WHERE id=?";
        try (Connection c = Conexion.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setLong(2, convId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // listado por usuario (residente o guardia) 
    public List<Conversation> listarPorUsuario(int userId, boolean esResidente) {
        final String sql = esResidente
                ? "SELECT id,residente_id,agente_id,estado FROM conversaciones WHERE residente_id=? ORDER BY id DESC"
                : "SELECT id,residente_id,agente_id,estado FROM conversaciones WHERE agente_id=? ORDER BY id DESC";

        List<Conversation> out = new ArrayList<>();
        try (Connection c = Conexion.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conversation cv = new Conversation();
                    cv.id = rs.getLong(1);
                    cv.residenteId = rs.getInt(2);
                    cv.agenteId = rs.getInt(3);
                    cv.estado = rs.getString(4);
                    out.add(cv);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
