package Servicio;

import PaqueteDAO.PaqueteDAO;
import UsuarioDAO.UsuarioDAO;
import modelo.entidad.Paquete;
import Conexion_DB.Conexion;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.sql.*;
import java.time.LocalDateTime;

public class PaqueteriaService {

        private final PaqueteDAO paqueteDAO = new PaqueteDAO();
        private final UsuarioDAO usuarioDAO = new UsuarioDAO();

        private int obtenerIdUsuarioSesion(HttpSession s) {
                Integer id = (Integer) s.getAttribute("usuarioId");
                if (id != null) {
                        return id;
                }
                String dpi = (String) s.getAttribute("usuarioDPI");
                if (dpi == null || dpi.trim().isEmpty()) {
                        throw new SecurityException("No hay usuarioId ni usuarioDPI en sesión.");
                }
                try {
                        Integer encontrado = usuarioDAO.obtenerIdPorDpi(dpi);
                        if (encontrado == null) {
                                throw new SecurityException("No se encontró el usuario por DPI.");
                        }
                        s.setAttribute("usuarioId", encontrado);
                        return encontrado;
                } catch (Exception e) {
                        throw new RuntimeException("Error resolviendo ID de usuario: " + e.getMessage(), e);
                }
        }

        private void validarAgente(HttpSession s) {
                String rolSesion = (String) s.getAttribute("usuarioRol"); // p.ej. "agente"
                if (rolSesion == null || !rolSesion.equalsIgnoreCase("agente")) {
                        throw new SecurityException("Acceso denegado: se requiere rol agente.");
                }
                int id = obtenerIdUsuarioSesion(s);
                if (!usuarioDAO.esAgentePorId(id)) {
                        throw new SecurityException("El usuario en sesión no es agente.");
                }
        }

        public int registrarRecepcion(HttpSession s, String numeroGuia, int idUsuarioDest, String observaciones) {
                validarAgente(s);
                if (numeroGuia == null || numeroGuia.trim().isEmpty()) {
                        throw new IllegalArgumentException("El número de guía es obligatorio.");
                }

                int idAgente = obtenerIdUsuarioSesion(s);

                Paquete p = new Paquete();
                p.setNumeroGuia(numeroGuia.trim());
                p.setIdUsuarioDest(idUsuarioDest);
                p.setIdAgenteReceptor(idAgente);
                p.setObservaciones(observaciones);

                return paqueteDAO.crearRecepcion(p);
        }

        public List<Paquete> listarPendientes(HttpSession s, String filtro) {
                validarAgente(s);
                return paqueteDAO.listarPendientes(filtro);
        }

        public boolean entregarPaquete(HttpSession s, int idPaquete) {
                validarAgente(s);
                int idAgente = obtenerIdUsuarioSesion(s);
                return paqueteDAO.marcarEntregado(idPaquete, idAgente);
        }

        public java.time.LocalDateTime entregarPaqueteYFecha(HttpSession s, int idPaquete) {
                validarAgente(s);
                int idAgente = obtenerIdUsuarioSesion(s);
                java.sql.Timestamp ts = paqueteDAO.marcarEntregadoYRetornarFecha(idPaquete, idAgente);
                return (ts == null) ? null : ts.toLocalDateTime();
        }

        public static class DatosCorreoEntrega {

                public String correoResidente;
                public String numeroGuia;
                public LocalDateTime fechaHoraEntrega;
        }

        public DatosCorreoEntrega datosCorreoEntrega(int idPaquete) {
                final String sql
                        = "SELECT p.numero_guia, p.fecha_entrega, u.correo "
                        + "FROM paquetes p "
                        + "JOIN usuarios u ON u.id = p.id_usuario_dest "
                        + // <- PK real en 'usuarios'
                        "WHERE p.id_paquete = ? "
                        + "LIMIT 1";

                try (Connection cn = Conexion.getConnection();
                        PreparedStatement ps = cn.prepareStatement(sql)) {

                        ps.setInt(1, idPaquete);

                        try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                        DatosCorreoEntrega d = new DatosCorreoEntrega();
                                        d.numeroGuia = rs.getString("numero_guia");

                                        Timestamp ts = rs.getTimestamp("fecha_entrega");
                                        d.fechaHoraEntrega = (ts != null)
                                                ? ts.toLocalDateTime()
                                                : java.time.LocalDateTime.now();

                                        d.correoResidente = rs.getString("correo");
                                        return d;
                                }
                        }
                } catch (SQLException e) {
                        e.printStackTrace();
                }
                return null;
        }

}
