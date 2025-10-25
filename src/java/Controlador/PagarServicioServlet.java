package Controlador;

import PagosDAO.TipoPagosDAO;
import modelo.TipoPago;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * Carga la vista pagarServicio.jsp con el catálogo de tipos de pago y datos
 * mínimos del usuario en sesión (usuarioId, usuarioNombre).
 */
@WebServlet("/pagos/pagar")
public class PagarServicioServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                // === Seguridad de sesión (igual que tu JSP) ===
                HttpSession ses = req.getSession(false);
                String rol = (ses == null) ? null : (String) ses.getAttribute("usuarioRol");
                Integer userId = (ses == null) ? null : (Integer) ses.getAttribute("usuarioId");
                String userNombre = (ses == null) ? null : (String) ses.getAttribute("usuarioNombre");

                if (rol == null || userId == null) {
                        resp.sendRedirect(req.getContextPath() + "/vistas/login.jsp");
                        return;
                }

                try {
                        // === Catálogo de tipos de pago (solo activos) ===
                        List<TipoPago> tipos = new TipoPagosDAO().listarActivos();
                        req.setAttribute("tiposPago", tipos);

                        // === Atributos que tu JSP ya espera/usa ===
                        req.setAttribute("usuarioId", userId);
                        req.setAttribute("nombreUsuario", userNombre);

                        // === Forward a la vista ===
                        req.getRequestDispatcher("/vistas/pagarServicio.jsp").forward(req, resp);

                } catch (Exception e) {
                        // Fallback sencillo si algo raro pasa
                        req.setAttribute("error", "No se pudo cargar el catálogo de pagos.");
                        req.getRequestDispatcher("/vistas/pagarServicio.jsp").forward(req, resp);
                }
        }

        // Opcional: redirige POST a GET si alguien entra por POST
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
                doGet(req, resp);
        }
}
