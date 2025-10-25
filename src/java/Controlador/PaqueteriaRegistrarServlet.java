package Controlador;

import Servicio.PaqueteriaService;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(name = "PaqueteriaRegistrarServlet", urlPatterns = "/paqueteria/registrar")
public class PaqueteriaRegistrarServlet extends HttpServlet {

        private final PaqueteriaService servicio = new PaqueteriaService();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json; charset=UTF-8");
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);

                try {
                        String numeroGuia = req.getParameter("numeroGuia");
                        int idDest = Integer.parseInt(req.getParameter("idUsuarioDest"));
                        String observ = req.getParameter("observaciones");

                        int idGen = servicio.registrarRecepcion(req.getSession(false), numeroGuia, idDest, observ);

                        // OPCIÓN 1: Usando String.format (más legible)
                        String json = String.format(
                                "{\"ok\":true,\"idPaquete\":%d,\"mensaje\":\"Guardado con éxito\"}",
                                idGen
                        );
                        resp.getWriter().write(json);

                        // OPCIÓN 2: Concatenación simple (asegurando la coma)
                        // resp.getWriter().write("{\"ok\":true,\"idPaquete\":" + idGen + ",\"mensaje\":\"Guardado con éxito\"}");
                } catch (Exception e) {
                        resp.setStatus(400);
                        resp.getWriter().write("{\"ok\":false,\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
                }
        }
}
