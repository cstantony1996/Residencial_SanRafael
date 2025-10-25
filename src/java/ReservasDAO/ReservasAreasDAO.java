package ReservasDAO;

import Conexion_DB.Conexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ReservasAreasDAO {

        private static volatile boolean SCHEMA_READY = false;

        public ReservasAreasDAO() {
                ensureSchema();
        }

        private void ensureSchema() {
                if (SCHEMA_READY) {
                        return;
                }
                synchronized (ReservasAreasDAO.class) {
                        if (SCHEMA_READY) {
                                return;
                        }
                        try (Connection con = Conexion.getConnection();
                                Statement st = con.createStatement()) {

                                // ===== Catálogo de áreas =====
                                st.execute(
                                        "CREATE TABLE IF NOT EXISTS areas ("
                                        + "  id INT PRIMARY KEY,"
                                        + "  nombre VARCHAR(100) NOT NULL"
                                        + ")"
                                );

                                // Insertar/actualizar Piscina(1) y Salón(2)
                                try {
                                        st.execute(
                                                "INSERT INTO areas (id,nombre) VALUES (1,'Piscina'),(2,'Salón') "
                                                + "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre)"
                                        );
                                } catch (SQLException ignore) {
                                        try {
                                                st.execute("INSERT INTO areas (id,nombre) VALUES (1,'Piscina')");
                                        } catch (SQLException e1) {
                                        }
                                        try {
                                                st.execute("INSERT INTO areas (id,nombre) VALUES (2,'Salón')");
                                        } catch (SQLException e2) {
                                        }
                                }

                                // ===== Tabla unificada de reservas =====
                                st.execute(
                                        "CREATE TABLE IF NOT EXISTS reservas ("
                                        + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                                        + "  area_id INT NOT NULL,"
                                        + "  fecha DATE NOT NULL,"
                                        + "  desde_hora TINYINT NOT NULL,"
                                        + // 0..23
                                        "  hasta_hora TINYINT NOT NULL,"
                                        + // 1..24 (exclusivo)
                                        "  reservado_por VARCHAR(150) NOT NULL,"
                                        + "  correo_residente VARCHAR(150) NOT NULL,"
                                        + "  estado ENUM('ACTIVA','CANCELADA') NOT NULL DEFAULT 'ACTIVA',"
                                        + "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                        + "  KEY idx_area (area_id),"
                                        + "  KEY idx_fecha (fecha),"
                                        + "  KEY idx_estado (estado),"
                                        + "  KEY idx_correo (correo_residente),"
                                        + "  CONSTRAINT fk_reservas_area FOREIGN KEY (area_id) REFERENCES areas(id) "
                                        + "    ON UPDATE CASCADE ON DELETE RESTRICT"
                                        + ")"
                                );

                                SCHEMA_READY = true;
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }

        public Map<Integer, String> listarAreas() {
                ensureSchema();
                LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
                String sql = "SELECT id,nombre FROM areas ORDER BY id";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                                map.put(rs.getInt("id"), rs.getString("nombre"));
                        }
                } catch (Exception e) {
                        // fallback
                }
                if (map.isEmpty()) {
                        map.put(1, "Piscina");
                        map.put(2, "Salón");
                }
                return map;
        }

        /**
         * Devuelve un vector de 24 posiciones (true = libre) para una fecha y
         * área.
         */
        public boolean[] disponibilidadPorDia(int areaId, LocalDate fecha) throws SQLException {
                ensureSchema();
                boolean[] horas = new boolean[24];
                Arrays.fill(horas, true);

                String sql = "SELECT desde_hora,hasta_hora FROM reservas "
                        + "WHERE area_id=? AND fecha=? AND estado='ACTIVA'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, areaId);
                        ps.setDate(2, java.sql.Date.valueOf(fecha));  // <<--- ¡IMPORTANTE!
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        int d = rs.getInt("desde_hora");
                                        int h = rs.getInt("hasta_hora");
                                        if (d < 0) {
                                                d = 0;
                                        }
                                        if (h > 24) {
                                                h = 24;
                                        }
                                        for (int k = d; k < h; k++) {
                                                if (k >= 0 && k < 24) {
                                                        horas[k] = false;
                                                }
                                        }
                                }
                        }
                }
                return horas;
        }

        /**
         * Comprueba si existe cruce real de reservas (para mensajes más
         * claros).
         */
        public boolean existeCruce(int areaId, LocalDate fecha, int desde, int hasta) throws SQLException {
                ensureSchema();
                String sql = "SELECT 1 FROM reservas "
                        + "WHERE area_id=? AND fecha=? AND estado='ACTIVA' "
                        + "AND (? < hasta_hora) AND (? > desde_hora) LIMIT 1";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, areaId);
                        ps.setDate(2, java.sql.Date.valueOf(fecha));   // <<--- ¡IMPORTANTE!
                        ps.setInt(3, desde);
                        ps.setInt(4, hasta);
                        try (ResultSet rs = ps.executeQuery()) {
                                return rs.next();
                        }
                }
        }

        /**
         * Inserta una reserva si no hay choque. (Compat)
         */
        public boolean reservar(int areaId, LocalDate fecha, int desde, int hasta,
                String reservadoPor, String correoResidente) throws SQLException {
                return reservarRetId(areaId, fecha, desde, hasta, reservadoPor, correoResidente) != null;
        }

        /**
         * Inserta y devuelve el ID generado, o null si no se pudo.
         */
        public Integer reservarRetId(int areaId, LocalDate fecha, int desde, int hasta,
                String reservadoPor, String correoResidente) throws SQLException {
                ensureSchema();
                if (desde < 0 || desde > 23 || hasta < 1 || hasta > 24 || desde >= hasta) {
                        return null;
                }

                try (Connection con = Conexion.getConnection()) {
                        con.setAutoCommit(false);
                        try {
                                // choque
                                if (existeCruce(areaId, fecha, desde, hasta)) {
                                        con.rollback();
                                        return null;
                                }
                                String ins = "INSERT INTO reservas "
                                        + "(area_id, fecha, desde_hora, hasta_hora, reservado_por, correo_residente, estado) "
                                        + "VALUES (?,?,?,?,?,?,'ACTIVA')";
                                try (PreparedStatement ps = con.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                                        ps.setInt(1, areaId);
                                        ps.setDate(2, java.sql.Date.valueOf(fecha));  // <<--- ¡IMPORTANTE!
                                        ps.setInt(3, desde);
                                        ps.setInt(4, hasta);
                                        ps.setString(5, (reservadoPor == null || reservadoPor.trim().isEmpty())
                                                ? "Usuario del sistema" : reservadoPor);
                                        ps.setString(6, correoResidente == null ? "" : correoResidente);
                                        ps.executeUpdate();

                                        try (ResultSet gk = ps.getGeneratedKeys()) {
                                                if (gk.next()) {
                                                        int id = gk.getInt(1);
                                                        con.commit();
                                                        return id;
                                                }
                                        }
                                }
                                con.commit();
                                return null;
                        } catch (Exception e) {
                                con.rollback();
                                throw e;
                        } finally {
                                con.setAutoCommit(true);
                        }
                }
        }

        /**
         * DTO de reserva (para listado/cancelación).
         */
        public static class ReservaItem {

                public int id;
                public int areaId;
                public LocalDate fecha;
                public int desdeHora;
                public int hastaHora;
                public String reservadoPor;
                public String correoResidente;
                public String estado;
                public Timestamp createdAt;
        }

        /**
         * Lista reservas ACTIVAS del usuario (por correo) desde una fecha y
         * área.
         */
        public List<ReservaItem> listarReservasActivasUsuario(int areaId, String correo, LocalDate desdeFecha) throws SQLException {
                ensureSchema();
                String sql = "SELECT id,area_id,fecha,desde_hora,hasta_hora,reservado_por,correo_residente,estado,created_at "
                        + "FROM reservas WHERE estado='ACTIVA' AND correo_residente=? AND area_id=? "
                        + (desdeFecha != null ? "AND fecha>=? " : "")
                        + "ORDER BY fecha,desde_hora";
                List<ReservaItem> out = new ArrayList<>();
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, correo);
                        ps.setInt(2, areaId);
                        if (desdeFecha != null) {
                                ps.setDate(3, java.sql.Date.valueOf(desdeFecha));  // <<--- ¡IMPORTANTE!
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                        ReservaItem r = new ReservaItem();
                                        r.id = rs.getInt("id");
                                        r.areaId = rs.getInt("area_id");
                                        r.fecha = rs.getDate("fecha").toLocalDate();
                                        r.desdeHora = rs.getInt("desde_hora");
                                        r.hastaHora = rs.getInt("hasta_hora");
                                        r.reservadoPor = rs.getString("reservado_por");
                                        r.correoResidente = rs.getString("correo_residente");
                                        r.estado = rs.getString("estado");
                                        r.createdAt = rs.getTimestamp("created_at");
                                        out.add(r);
                                }
                        }
                }
                return out;
        }

        /**
         * Obtiene una reserva por id (opcionalmente filtrando por correo).
         */
        public ReservaItem obtenerReservaPorId(int areaId, int reservaId, String correoOpt) throws SQLException {
                ensureSchema();
                String sql = "SELECT id,area_id,fecha,desde_hora,hasta_hora,reservado_por,correo_residente,estado,created_at "
                        + "FROM reservas WHERE id=? "
                        + (correoOpt != null ? "AND correo_residente=? " : "")
                        + "LIMIT 1";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, reservaId);
                        if (correoOpt != null) {
                                ps.setString(2, correoOpt);
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        ReservaItem r = new ReservaItem();
                                        r.id = rs.getInt("id");
                                        r.areaId = rs.getInt("area_id");
                                        r.fecha = rs.getDate("fecha").toLocalDate();
                                        r.desdeHora = rs.getInt("desde_hora");
                                        r.hastaHora = rs.getInt("hasta_hora");
                                        r.reservadoPor = rs.getString("reservado_por");
                                        r.correoResidente = rs.getString("correo_residente");
                                        r.estado = rs.getString("estado");
                                        r.createdAt = rs.getTimestamp("created_at");
                                        // Si te pasan areaId y quieres validar:
                                        if (areaId != 0 && r.areaId != areaId) {
                                                return null;
                                        }
                                        return r;
                                }
                        }
                }
                return null;
        }

        /**
         * Cancela (marca CANCELADA) si pertenece al correo.
         */
        public boolean cancelarReserva(int areaId, int reservaId, String correoResidente) throws SQLException {
                ensureSchema();
                // Filtrar por área opcionalmente; si quieres forzar área, añade "AND area_id=?"
                String sql = "UPDATE reservas SET estado='CANCELADA' "
                        + "WHERE id=? AND correo_residente=? AND estado='ACTIVA'";
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, reservaId);
                        ps.setString(2, correoResidente);
                        return ps.executeUpdate() > 0;
                }
        }
}
