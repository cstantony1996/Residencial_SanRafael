package Controlador;

import PuntosControlDAO.PuntosControlDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/garita")
public class GaritaController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("puntos", new PuntosControlDAO().listarActivos());
            req.getRequestDispatcher("/vistas/garita.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException("No se pudieron cargar los puntos de control", e);
        }
    }
}
