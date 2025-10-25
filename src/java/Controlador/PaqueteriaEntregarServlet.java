package Controlador;

import Servicio.PaqueteriaService;
import Servicio.PaqueteriaService.DatosCorreoEntrega;
import Utils.CorreoUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebServlet(name = "PaqueteriaEntregarServlet", urlPatterns = "/paqueteria/entregar")
public class PaqueteriaEntregarServlet extends HttpServlet {

        private final PaqueteriaService servicio = new PaqueteriaService();

        // Pool liviano para correos (1 hilo en background, daemon)
        private static final ExecutorService MAIL_EXEC = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "mail-notify");
                t.setDaemon(true);
                return t;
        });

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json; charset=UTF-8");
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);

                try {
                        String raw = req.getParameter("idPaquete");
                        if (raw == null) {
                                resp.setStatus(400);
                                resp.getWriter().write("{\"ok\":false,\"error\":\"Falta idPaquete\"}");
                                return;
                        }
                        int idPaquete = Integer.parseInt(raw);

                        // reemplaza la llamada actual:
                        java.time.LocalDateTime fh = servicio.entregarPaqueteYFecha(req.getSession(false), idPaquete);
                        boolean ok = (fh != null);

                        if (ok) {
                                DatosCorreoEntrega d = servicio.datosCorreoEntrega(idPaquete); // guía + correo
                                if (d != null && d.correoResidente != null && !d.correoResidente.trim().isEmpty()) {
                                        java.time.LocalDateTime fechaParaCorreo = fh; // usa la devuelta por el DAO
                                        MAIL_EXEC.submit(() -> {
                                                try {
                                                        CorreoUtil.enviarNotificacionEntregaPaquete(
                                                                d.correoResidente, d.numeroGuia, fechaParaCorreo
                                                        );
                                                } catch (Exception ex) {
                                                        ex.printStackTrace();
                                                }
                                        });
                                }
                        }

                        resp.setContentType("application/json; charset=UTF-8");
                        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                        resp.setHeader("Pragma", "no-cache");
                        resp.setDateHeader("Expires", 0);

                        resp.getWriter().write(ok
                                ? "{\"ok\":true,\"mensaje\":\"Paquete ENTREGADO\"}"
                                : "{\"ok\":false,\"mensaje\":\"Ya estaba entregado o no se actualizó\"}");

                } catch (NumberFormatException e) {
                        resp.setStatus(400);
                        resp.getWriter().write("{\"ok\":false,\"error\":\"idPaquete inválido\"}");
                } catch (Exception e) {
                        resp.setStatus(400);
                        resp.getWriter().write("{\"ok\":false,\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
                }
        }
}
