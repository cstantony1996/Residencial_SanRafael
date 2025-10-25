package Controlador;

import UsuarioDAO.UsuarioDAO;
import Usuario.Usuario;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "PaqueteriaViewServlet", urlPatterns = {"/paqueteria"})
public class PaqueteriaViewServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                // (Opcional) validar sesión/rol aquí si no lo hace tu filtro
                HttpSession s = req.getSession(false);
                if (s == null || s.getAttribute("user") == null) {
                        resp.sendRedirect(req.getContextPath() + "/login");
                        return;
                }

                try {
                        // Cargar residentes para el <select>
                        List<Usuario> todos = new UsuarioDAO().obtenerTodos();
                        List<Usuario> residentes = todos.stream()
                                .filter(u -> u.getRol() != null && u.getRol().equalsIgnoreCase("residente"))
                                .collect(Collectors.toList());

                        req.setAttribute("residentes", residentes);
                } catch (Exception e) {
                        req.setAttribute("residentes", java.util.Collections.emptyList());
                }

                // Ruta donde guardaste la JSP
                req.getRequestDispatcher("/vistas/paqueteria.jsp").forward(req, resp);
        }
}
