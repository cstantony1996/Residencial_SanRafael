package Controlador;

import DirectorioDAO.DirectorioDAO;
import Servicio.DirectorioService;
import modelo.DirectorioFiltro;
import modelo.DirectorioItem;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/directorio")
public class DirectorioController extends HttpServlet {

        private DirectorioService service;

        @Override
        public void init() {
                service = new DirectorioService(new DirectorioDAO());
        }

        private Integer toIntOrNull(String s) {
                try {
                        return (s == null || s.trim().isEmpty() ? null : Integer.valueOf(s.trim()));
                } catch (Exception e) {
                        return null;
                }
        }

        private int toPositiveOrDefault(String s, int def) {
                try {
                        int v = Integer.parseInt(s);
                        return v > 0 ? v : def;
                } catch (Exception e) {
                        return def;
                }
        }

        private boolean hasText(String s) {
                return s != null && !s.trim().isEmpty();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                req.setCharacterEncoding("UTF-8");

                String nombre = req.getParameter("nombre");
                String apellidos = req.getParameter("apellidos");
                String lote = req.getParameter("lote");
                Integer numero = toIntOrNull(req.getParameter("numero_casa"));
                int page = toPositiveOrDefault(req.getParameter("page"), 1);
                int size = toPositiveOrDefault(req.getParameter("size"), 10);

                DirectorioFiltro f = new DirectorioFiltro();
                f.setNombre(nombre);
                f.setApellidos(apellidos);
                f.setLote(lote);
                f.setNumeroCasa(numero);
                f.setPage(page);
                f.setSize(size);

                // Bandera para mostrar/ocultar columna "Correo"
                boolean isSearch = hasText(nombre) || hasText(apellidos) || hasText(lote) || numero != null;
                req.setAttribute("isSearch", isSearch);

                String error = Servicio.DirectorioService.validar(f);
                req.setAttribute("f", f);

                if (error != null) {
                        req.setAttribute("error", error);
                        req.getRequestDispatcher("/vistas/directorio.jsp").forward(req, resp);
                        return;
                }

                try {
                        // Siempre listamos (con o sin filtros)
                        List<DirectorioItem> resultados = service.buscar(f);
                        int total = service.contar(f);

                        req.setAttribute("resultados", resultados);
                        req.setAttribute("total", total);

                        // 🔹 Nuevo: mensaje cuando no hay resultados
                        if (total == 0) {
                                req.setAttribute("dirEmptyMsg", "No se encontró ningún usuario con los datos ingresados.");
                        }

                        req.getRequestDispatcher("/vistas/directorio.jsp").forward(req, resp);

                } catch (SQLException e) {
                        req.setAttribute("error", "Ocurrió un error al consultar el directorio.");
                        req.getRequestDispatcher("/vistas/directorio.jsp").forward(req, resp);
                }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet(req, resp);
        }
}
