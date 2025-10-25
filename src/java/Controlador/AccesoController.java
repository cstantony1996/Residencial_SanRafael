package Controlador;

import AccesoDAO.AccesoDAO;
import Servicio.AccesoService;
import VehiculoDAO.VehiculoDAO;
import PuntosControlDAO.PuntosControlDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;

@WebServlet("/acceso/validar")
public class AccesoController extends HttpServlet {

    private AccesoService service;
    private AccesoDAO dao;

    @Override
    public void init() {
        service = new AccesoService();
        dao = new AccesoDAO();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        // ===== 1) Leer parámetros =====
        String modo = trimOrNull(req.getParameter("modo"));        // "token" | "texto"
        String tipoIn = trimOrNull(req.getParameter("tipo"));        // "peaton" | "vehiculo"
        String pStr = trimOrNull(req.getParameter("puntoId"));
        String datosQR = req.getParameter("datosQR");
        String direccion = trimOrNull(req.getParameter("direccion"));   // "entrada" | "salida"
        Integer guardiaId = (Integer) req.getSession().getAttribute("usuarioId");

        // Normalizar/validar básicos
        Integer puntoId = parseIntOrNull(pStr);
        String tipoAcceso = ("vehiculo".equalsIgnoreCase(tipoIn) ? "vehiculo" : "peaton");
        String dir = ("salida".equalsIgnoreCase(direccion) ? "salida" : "entrada"); // default entrada

        if (modo == null || puntoId == null) {
            writeJson(resp, "{\"resultado\":\"denegado\",\"motivo\":\"Parámetros inválidos (modo/punto).\"}");
            return;
        }

        // Valida que el punto exista
        try {
            if (!new PuntosControlDAO().existe(puntoId)) {
                writeJson(resp, "{\"resultado\":\"denegado\",\"motivo\":\"Punto de control inválido\"}");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            writeJson(resp, "{\"resultado\":\"denegado\",\"motivo\":\"Error verificando punto de control\"}");
            return;
        }

        Integer usuarioId = null;
        String resultado = "denegado";
        String motivo = null;
        String tokenUsado = null;

        try {
            // ===== 2) Validación por token o por texto =====
            if ("token".equalsIgnoreCase(modo)) {
                String tk = trimOrNull(req.getParameter("tk"));
                tokenUsado = tk;
                if (tk == null) {
                    motivo = "Token ausente";
                } else {
                    usuarioId = service.validarPorToken(tk); // null => inválido/expirado/sin usos
                    resultado = (usuarioId != null) ? "permitido" : "denegado";
                    if (usuarioId == null) {
                        motivo = "Token inválido o expirado";
                    }
                }
            } else if ("texto".equalsIgnoreCase(modo)) {
                String correo = trimOrNull(req.getParameter("correo"));
                String lote = trimOrNull(req.getParameter("lote"));
                Integer casa = parseIntOrNull(req.getParameter("numeroCasa"));
                if (correo == null || lote == null || casa == null) {
                    motivo = "Contenido del QR incompleto";
                } else {
                    usuarioId = service.validarPorTexto(correo, lote, casa);
                    resultado = (usuarioId != null) ? "permitido" : "denegado";
                    if (usuarioId == null) {
                        motivo = "Datos no coinciden";
                    }
                }
            } else {
                motivo = "Modo no soportado";
            }

            // ===== 3) Reglas para acceso VEHICULAR =====
            if ("vehiculo".equals(tipoAcceso) && "permitido".equals(resultado)) {
                VehiculoDAO vdao = new VehiculoDAO();
                int cuantos = vdao.contarActivosPorUsuario(usuarioId);
                if (cuantos <= 0) {
                    resultado = "denegado";
                    motivo = "El residente no cuenta con vehículos registrados.";
                }
            }

            // ===== 4) Registrar en bitácora =====
            try {
                dao.registrarAcceso(
                        usuarioId, // usuario_id (puede ser null si token inválido)
                        tipoAcceso, // "peaton" | "vehiculo"
                        puntoId, // punto_control_id
                        resultado, // "permitido" | "denegado"
                        motivo, // motivo_denegacion (nullable)
                        dir, // direccion: "entrada" | "salida"
                        datosQR, // datos_qr (crudo leído)
                        guardiaId, // guardia_usuario_id (nullable)
                        clientIp(req), // ip_origen
                        req.getHeader("User-Agent") // user_agent
                );
            } catch (Exception e) {
                // No interrumpir el flujo si el log falla
                e.printStackTrace();
            }

            // ===== 5) Notificar acceso permitido (RN2) =====
            if ("permitido".equals(resultado) && usuarioId != null) {
                service.notificarAccesoAutorizado(usuarioId, tokenUsado);
            }

            // ===== 6) Responder JSON =====
            writeJson(resp, "{\"resultado\":\"" + resultado + "\",\"motivo\":\"" + esc(motivo) + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            writeJson(resp, "{\"resultado\":\"denegado\",\"motivo\":\"Error inesperado en validación\"}");
        }
    }

    // ===== Helpers =====
    private static Integer parseIntOrNull(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimOrNull(String s) {
        return (s == null) ? null : (s.trim().isEmpty() ? null : s.trim());
    }

    private static String esc(String s) {
        return (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""));
    }

    private static void writeJson(HttpServletResponse resp, String json) throws IOException {
        resp.getWriter().write(json);
    }

    private static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        String xr = req.getHeader("X-Real-IP");
        return (xr != null && !xr.isEmpty()) ? xr : req.getRemoteAddr();
    }
}
