package Controlador;

import Servicio.PagoService;
import Servicio.BitacoraService;
import Servicio.BitacoraService.Accion;
import modelo.Pago;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@WebServlet("/pagos/registrar")
public class RegistrarPagoServlet extends HttpServlet {

        private final PagoService pagoService = new PagoService();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                req.setCharacterEncoding("UTF-8");
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("application/json;charset=UTF-8");

                try {
                        // ---- Lee parámetros con tolerancia ----
                        String sUsuarioId = trimOrNull(req.getParameter("usuarioId"));
                        String sNombre = trimOrEmpty(req.getParameter("nombreUsuario"));
                        String sTipoPagoId = trimOrNull(req.getParameter("tipoPagoId"));
                        String sMesAnio = trimOrNull(req.getParameter("mesAnio"));          // puede ir null
                        String observaciones = trimOrEmpty(req.getParameter("observaciones"));
                        String pan = trimOrEmpty(req.getParameter("tarjetaPan"));

                        // Fallback: si no vino usuarioId en el form, intenta tomarlo de sesión
                        if (sUsuarioId == null) {
                                Object sid = req.getSession(false) != null ? req.getSession(false).getAttribute("usuarioId") : null;
                                if (sid != null) {
                                        sUsuarioId = sid.toString();
                                }
                        }

                        Integer usuarioId = parseIntSafe(sUsuarioId);
                        Integer tipoPagoId = parseIntSafe(sTipoPagoId);

                        // Validaciones de presencia
                        if (usuarioId == null) {
                                badRequest(resp, "usuarioId inválido o ausente.");
                                return;
                        }
                        if (tipoPagoId == null) {
                                badRequest(resp, "tipoPagoId inválido o ausente.");
                                return;
                        }
                        if (observaciones.length() < 5) {
                                badRequest(resp, "Las observaciones son obligatorias (mínimo 5 caracteres).");
                                return;
                        }
                        // PAN opcional, pero si viene, que sea 16 dígitos
                        if (!pan.isEmpty() && !pan.matches("\\d{16}")) {
                                badRequest(resp, "Número de tarjeta inválido.");
                                return;
                        }

                        LocalDateTime ahora = LocalDateTime.now();

                        // Normaliza mes/año vacío -> null
                        String mesAnio = (sMesAnio == null || sMesAnio.isEmpty()) ? null : sMesAnio;

                        Pago p = pagoService.registrarPago(
                                usuarioId,
                                sNombre,
                                tipoPagoId,
                                mesAnio,
                                observaciones,
                                pan,
                                ahora
                        );

                        // ==== Bitácora (OK) ====
                        String detalle = String.format(Locale.US, "Pago aprobado ID=%d total=Q%.2f", p.getId(), p.getTotal());
                        BitacoraService.logOk(req, "Pagos", Accion.PAGO, "pago", p.getId(), detalle);

                        // Respuesta JSON para el front (overlay de éxito)
                        String json = String.format(Locale.US,
                                "{\"ok\":true,\"pagoId\":%d,\"total\":%.2f}",
                                p.getId(), p.getTotal());
                        resp.getWriter().write(json);

                } catch (Exception e) {
                        e.printStackTrace();
                        // ==== Bitácora (ERROR) ====
                        BitacoraService.logError(req, "Pagos", Accion.PAGO, "pago", null, "Error registrando pago", e);

                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("{\"ok\":false,\"error\":\"No se pudo registrar\"}");
                }
        }

        // ---------- Helpers ----------
        private static String trimOrNull(String s) {
                if (s == null) {
                        return null;
                }
                s = s.trim();
                return s.isEmpty() ? null : s;
        }

        private static String trimOrEmpty(String s) {
                return (s == null) ? "" : s.trim();
        }

        private static Integer parseIntSafe(String v) {
                if (v == null) {
                        return null;
                }
                try {
                        return Integer.valueOf(v);
                } catch (NumberFormatException e) {
                        return null;
                }
        }

        // JSON 400
        private static void badRequest(HttpServletResponse resp, String msg) throws IOException {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"ok\":false,\"error\":\"" + escapeJson(msg) + "\"}");
        }

        private static String escapeJson(String s) {
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
