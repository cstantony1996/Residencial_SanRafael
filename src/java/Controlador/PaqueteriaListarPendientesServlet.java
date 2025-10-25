package Controlador;

import PaqueteDAO.PaqueteDAO;
import modelo.entidad.Paquete;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "PaqueteriaPendientesServlet", urlPatterns = "/paqueteria/pendientes")
public class PaqueteriaListarPendientesServlet extends HttpServlet {

        private final PaqueteDAO paqueteDAO = new PaqueteDAO();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json; charset=UTF-8");
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);

                String q = req.getParameter("q");
                try {
                        List<Paquete> lista = paqueteDAO.listarPendientes(q);
                        // serializa a JSON como sea que lo haces normalmente:
                        StringBuilder sb = new StringBuilder("{\"ok\":true,\"pendientes\":[");
                        for (int i = 0; i < lista.size(); i++) {
                                Paquete p = lista.get(i);
                                if (i > 0) {
                                        sb.append(',');
                                }
                                sb.append("{")
                                        .append("\"idPaquete\":").append(p.getIdPaquete()).append(',')
                                        .append("\"numeroGuia\":").append(j(p.getNumeroGuia())).append(',')
                                        .append("\"idUsuarioDest\":").append(p.getIdUsuarioDest()).append(',')
                                        .append("\"nombre\":").append(j(p.getNombreDestinatario())).append(',')
                                        .append("\"apellidos\":").append(j(p.getApellidosDestinatario())).append(',')
                                        .append("\"lote\":").append(j(p.getLote())).append(',')
                                        .append("\"numeroCasa\":").append(p.getNumeroCasa() == null ? "null" : p.getNumeroCasa().toString()).append(',')
                                        .append("\"fechaRecepcion\":").append(j(String.valueOf(p.getFechaRecepcion())))
                                        .append("}");
                        }
                        sb.append("]}");
                        resp.getWriter().write(sb.toString());
                } catch (Exception e) {
                        resp.getWriter().write("{\"ok\":false,\"error\":" + j(e.getMessage()) + "}");
                }
        }

        private static String j(String s) {
                if (s == null) {
                        return "null";
                }
                return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
}
