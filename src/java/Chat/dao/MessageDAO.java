package Chat.dao;

import Chat.model.Message;
import Chat.model.UserRole;
import Conexion_DB.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageDAO {

       
        
         // Inserta mensaje SIN soporte de hilos.
         
        public Message insertar(long convId, UserRole fromRole, String texto) throws SQLException {
                final String sql = "INSERT INTO mensajes "
                        + "(conversacion_id, remitente_rol, texto, creado_en) "
                        + "VALUES (?, ?, ?, NOW())";

                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                        ps.setLong(1, convId);
                        ps.setString(2, fromRole.name());
                        ps.setString(3, texto);
                        ps.executeUpdate();

                        try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) {
                                        Message m = new Message();
                                        m.id = rs.getLong(1);
                                        m.conversacionId = convId;
                                        m.fromRole = fromRole;
                                        m.texto = texto;
                                        m.tsIso = isoNow();
                                        return m;
                                }
                        }
                }
                throw new SQLException("No se pudo insertar mensaje");
        }

        /**
         * Compatibilidad con código legado que aún pase threadId. Se ignora el
         * parámetro y se delega al método sin hilos.
         */
        @Deprecated
        public Message insertar(long convId, UserRole fromRole, String texto, int threadId) throws SQLException {
                return insertar(convId, fromRole, texto);
        }

        /**
         * Compatibilidad con código legado (threadId nullable). Se ignora el
         * parámetro y se delega al método sin hilos.
         */
        @Deprecated
        public Message insertar(long convId, UserRole fromRole, String texto, Integer threadId) throws SQLException {
                return insertar(convId, fromRole, texto);
        }

     
         //Historial por conversación (sin hilos).
         
        public List<Message> listarPorConversacion(long convId, int limit) {
                final String sql = "SELECT id, remitente_rol, texto, creado_en "
                        + "FROM mensajes WHERE conversacion_id=? "
                        + "ORDER BY id DESC LIMIT ?";

                List<Message> out = new ArrayList<>();
                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql)) {

                        ps.setLong(1, convId);
                        ps.setInt(2, limit);

                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        Message m = new Message();
                                        m.id = rs.getLong(1);
                                        m.conversacionId = convId;
                                        m.fromRole = UserRole.valueOf(rs.getString(2).toUpperCase());
                                        m.texto = rs.getString(3);
                                        Timestamp ts = rs.getTimestamp(4);
                                        m.tsIso = (ts != null ? toIso(ts) : null);
                                        out.add(m);
                                }
                        }
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                Collections.reverse(out); // devolver en orden ascendente
                return out;
        }

        /**
         * Compatibilidad con firma antigua que aceptaba threadId. Se ignora el
         * parámetro y se delega.
         */
        @Deprecated
        public List<Message> listarPorConversacion(long convId, Integer threadId, int limit) {
                return listarPorConversacion(convId, limit);
        }

        // ========= ÚLTIMO MENSAJE (preview lista) =========
        public Message ultimoDeConversacion(long convId) {
                final String sql = "SELECT id, texto, creado_en "
                        + "FROM mensajes WHERE conversacion_id=? ORDER BY id DESC LIMIT 1";
                try (Connection c = Conexion.getConnection();
                        PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setLong(1, convId);
                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        Message m = new Message();
                                        m.id = rs.getLong(1);
                                        m.conversacionId = convId;
                                        m.texto = rs.getString(2);
                                        m.creadoEn = rs.getTimestamp(3);
                                        return m;
                                }
                        }
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                return null;
        }

        // ========= Helpers =========
        private static String isoNow() {
                return toIso(new Timestamp(System.currentTimeMillis()));
        }

        private static String toIso(Timestamp ts) {
                return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(ts);
        }
}
