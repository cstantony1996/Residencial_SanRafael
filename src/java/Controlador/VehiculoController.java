package Controlador;

import Vehiculo.Vehiculo;
import VehiculoDAO.VehiculoDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/VehiculoController")
public class VehiculoController extends HttpServlet {

    private VehiculoDAO vehiculoDAO;

    @Override
    public void init() {
        vehiculoDAO = new VehiculoDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String accion = req.getParameter("accion");
        if (accion == null) {
            accion = "listar";
        }

        try {
            switch (accion) {
                case "listar":
                    listar(req, resp);
                    break;
                case "desactivar":
                    desactivar(req, resp); // usa POST en prod; aquí GET por rapidez
                    break;
                default:
                    listar(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException("Error en VehiculoController", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String accion = req.getParameter("accion");
        if (accion == null) {
            accion = "listar";
        }

        try {
            switch (accion) {
                case "agregar":
                    agregar(req, resp);
                    break;
                case "actualizar":
                    actualizar(req, resp);
                    break;
                case "desactivar":
                    desactivar(req, resp);
                    break;
                default:
                    listar(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException("Error en VehiculoController", e);
        }
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer usuarioId = parseInt(req.getParameter("usuarioId"));
        if (usuarioId == null) {
            flashAndRedirect(req, resp, "warning", "Falta usuarioId");
            return;
        }
        List<Vehiculo> vehiculos = vehiculoDAO.listarPorUsuario(usuarioId);
        req.setAttribute("usuarioId", usuarioId);
        req.setAttribute("vehiculos", vehiculos);
        req.getRequestDispatcher("vistas/vehiculosUsuario.jsp").forward(req, resp);
    }

    private void agregar(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer usuarioId = parseInt(req.getParameter("usuarioId"));
        String placa = req.getParameter("placa");
        String marca = req.getParameter("marca");
        String modelo = req.getParameter("modelo");
        String color = req.getParameter("color");

        if (usuarioId == null || placa == null || placa.trim().isEmpty()) {
            flashAndRedirect(req, resp, "warning", "Usuario/placa requeridos");
            return;
        }
        if (vehiculoDAO.existePlacaActiva(placa)) {
            flashAndRedirect(req, resp, "warning", "La placa ya está registrada y activa");
            return;
        }
        Vehiculo v = new Vehiculo(usuarioId, placa, marca, modelo, color);
        boolean ok = vehiculoDAO.insertar(v);
        flashAndRedirect(req, resp, ok ? "success" : "danger",
                ok ? "Vehículo agregado" : "No se pudo agregar el vehículo");
        resp.sendRedirect("VehiculoController?accion=listar&usuarioId=" + usuarioId);
    }

    private void actualizar(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        Integer usuarioId = parseInt(req.getParameter("usuarioId"));
        String placa = req.getParameter("placa");
        String marca = req.getParameter("marca");
        String modelo = req.getParameter("modelo");
        String color = req.getParameter("color");

        if (id == null || usuarioId == null) {
            flashAndRedirect(req, resp, "warning", "Faltan datos");
            return;
        }
        Vehiculo v = new Vehiculo(id, usuarioId, placa, marca, modelo, color, true);
        boolean ok = vehiculoDAO.actualizar(v);
        flashAndRedirect(req, resp, ok ? "success" : "danger",
                ok ? "Vehículo actualizado" : "No se pudo actualizar");
        resp.sendRedirect("VehiculoController?accion=listar&usuarioId=" + usuarioId);
    }

    private void desactivar(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        Integer usuarioId = parseInt(req.getParameter("usuarioId"));
        if (id == null || usuarioId == null) {
            flashAndRedirect(req, resp, "warning", "Faltan datos");
            return;
        }
        boolean ok = vehiculoDAO.desactivarPorId(id, usuarioId);
        flashAndRedirect(req, resp, ok ? "success" : "danger",
                ok ? "Vehículo desactivado" : "No se pudo desactivar");
        resp.sendRedirect("VehiculoController?accion=listar&usuarioId=" + usuarioId);
    }

    private static Integer parseInt(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void flashAndRedirect(HttpServletRequest req, HttpServletResponse resp, String tipo, String msg) throws IOException {
        HttpSession s = req.getSession();
        s.setAttribute("flash_type", (tipo == null || tipo.trim().isEmpty()) ? "info" : tipo);
        s.setAttribute("flash_msg", msg == null ? "" : msg);
    }
}
