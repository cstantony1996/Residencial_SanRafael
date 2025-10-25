package Controlador;

import Servicio.PagoService;
import modelo.dto.CalculoPagoDTO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@WebServlet("/pagos/calcular")
public class CalcularPagoServlet extends HttpServlet {

        private final PagoService pagoService = new PagoService();

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                req.setCharacterEncoding("UTF-8");
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("application/json");

                try {
                        int usuarioId = Integer.parseInt(n(req.getParameter("usuarioId")));
                        int tipoPagoId = Integer.parseInt(n(req.getParameter("tipoPagoId")));
                        String nombreUsuario = n(req.getParameter("nombreUsuario"));

                        CalculoPagoDTO dto = pagoService.armarCalculo(tipoPagoId, usuarioId, nombreUsuario);

                        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US));

                        String json = String.format(Locale.US,
                                "{"
                                + "\"usuarioId\":%d,"
                                + "\"tipo\":\"%s\","
                                + "\"montoBase\":%.2f,"
                                + "\"mora\":%.2f,"
                                + "\"total\":%.2f,"
                                + "\"mesAnio\":%s,"
                                + "\"fechaPago\":\"%s\""
                                + "}",
                                dto.usuarioId,
                                esc(dto.tipo),
                                dto.montoBase,
                                dto.mora,
                                dto.total,
                                (dto.mesAnio == null ? "null" : ("\"" + esc(dto.mesAnio) + "\"")),
                                now
                        );
                        resp.getWriter().write(json);
                } catch (Exception e) {
                        e.printStackTrace();
                        resp.getWriter().write("{\"error\":\"Error al calcular\"}");
                }
        }

        private static String n(String s) {
                return s == null ? "" : s.trim();
        }

        private static String esc(String s) {
                return s == null ? "" : s.replace("\"", "\\\"");
        }
}
