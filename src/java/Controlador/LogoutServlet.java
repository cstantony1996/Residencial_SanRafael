package Controlador;

import Servicio.BitacoraService;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                HttpSession s = req.getSession(false);
                String dpi = null;
                String nombre = null;

                if (s != null) {
                        Object oDpi = s.getAttribute("usuarioDPI");
                        Object oNom = s.getAttribute("usuarioNombre");
                        dpi = oDpi == null ? null : oDpi.toString();
                        nombre = oNom == null ? null : oNom.toString();

                        // Bitácora antes de invalidar la sesión
                        BitacoraService.log(req, "Autenticación", BitacoraService.Accion.LOGOUT,
                                "sesion", dpi != null ? dpi : nombre, "Cierre de sesión",
                                null, null, true, null);

                        s.invalidate();
                }

                resp.sendRedirect(req.getContextPath() + "/login");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
                doGet(req, resp);
        }
}
