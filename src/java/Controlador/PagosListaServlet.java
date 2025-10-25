package Controlador;

import PagosDAO.PagoDAO;
import modelo.Pago;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/pagos")
public class PagosListaServlet extends HttpServlet {

        private final PagoDAO pagoDAO = new PagoDAO();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                req.setCharacterEncoding("UTF-8");

                int page = parseIntOr(req.getParameter("page"), 1);
                int size = parseIntOr(req.getParameter("size"), 10);
                if (size <= 0) {
                        size = 10;
                }
                if (page <= 0) {
                        page = 1;
                }

                int offset = (page - 1) * size;

                try {
                        int totalRows = pagoDAO.countAll();
                        int totalPages = (int) Math.ceil(totalRows / (double) size);
                        if (totalPages == 0) {
                                totalPages = 1; // evita división por cero
                        }
                        if (page > totalPages) {
                                page = totalPages;
                                offset = (page - 1) * size;
                        }

                        List<Pago> pagos = pagoDAO.listPage(size, offset);

                        boolean hasPrev = page > 1;
                        boolean hasNext = page < totalPages;

                        req.setAttribute("pagos", pagos);
                        req.setAttribute("page", page);
                        req.setAttribute("size", size);
                        req.setAttribute("totalPages", totalPages);
                        req.setAttribute("totalRows", totalRows);
                        req.setAttribute("hasPrev", hasPrev);
                        req.setAttribute("hasNext", hasNext);

                        req.getRequestDispatcher("/vistas/pagos/pagosLista.jsp").forward(req, resp);

                } catch (Exception ex) {
                        req.setAttribute("error", "Error al listar pagos: " + ex.getMessage());
                        req.getRequestDispatcher("/vistas/pagos/pagosLista.jsp").forward(req, resp);
                }
        }

        private static int parseIntOr(String s, int def) {
                try {
                        return Integer.parseInt(s);
                } catch (Exception e) {
                        return def;
                }
        }
}
