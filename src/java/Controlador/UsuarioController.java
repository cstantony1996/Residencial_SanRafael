package Controlador;

import Usuario.Usuario;
import UsuarioDAO.UsuarioDAO;

import Utils.QRCodeUtil;
import Utils.CorreoUtil;
import Utils.TokenQRUtil;

import QRDAO.QrTokenDAO;
import Servicio.BitacoraService; // <-- bitácora

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/UsuarioController")
public class UsuarioController extends HttpServlet {

        private UsuarioDAO usuarioDAO;
        private QrTokenDAO qrTokenDAO;

        @Override
        public void init() {
                usuarioDAO = new UsuarioDAO();
                qrTokenDAO = new QrTokenDAO();
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

                request.setCharacterEncoding("UTF-8");
                response.setCharacterEncoding("UTF-8");

                String accion = request.getParameter("accion");
                accion = (accion == null) ? "listar" : accion;

                try {
                        switch (accion) {
                                case "nuevo":
                                        mostrarFormulario(request, response);
                                        break;
                                case "editar":
                                        editarUsuario(request, response);
                                        break;
                                case "eliminar":
                                        eliminarUsuario(request, response);
                                        break;
                                default:
                                        listarUsuarios(request, response);
                        }
                } catch (SQLException e) {
                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.OTRO,
                                "usuarios", null, "Error en doGet: " + e.getMessage(),
                                null, null, false, e);
                        throw new ServletException("Error al procesar la solicitud", e);
                }
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

                request.setCharacterEncoding("UTF-8");
                response.setCharacterEncoding("UTF-8");

                String accion = request.getParameter("accion");
                accion = (accion == null) ? "listar" : accion;

                try {
                        switch (accion) {
                                case "insertar":
                                        insertarUsuario(request, response);
                                        break;
                                case "actualizar":
                                        actualizarUsuario(request, response);
                                        break;
                                default:
                                        listarUsuarios(request, response);
                        }
                } catch (SQLException e) {
                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.OTRO,
                                "usuarios", null, "Error en doPost: " + e.getMessage(),
                                null, null, false, e);
                        throw new ServletException("Error al procesar la solicitud", e);
                }
        }

        private void listarUsuarios(HttpServletRequest request, HttpServletResponse response)
                throws SQLException, ServletException, IOException {
                List<Usuario> usuarios = usuarioDAO.obtenerTodos();
                request.setAttribute("usuarios", usuarios);
                request.getRequestDispatcher("listaUsuarios.jsp").forward(request, response);
        }

        private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
        }

        private void insertarUsuario(HttpServletRequest request, HttpServletResponse response)
                throws SQLException, IOException, ServletException {

                String dpi = trimOrNull(request.getParameter("dpi"));
                String nombre = trimOrNull(request.getParameter("nombre"));
                String apellidos = trimOrNull(request.getParameter("apellidos"));
                String correo = trimOrNull(request.getParameter("correo"));
                String contraseña = trimOrNull(request.getParameter("password"));
                String rol = trimOrNull(request.getParameter("rol"));
                String lote = trimOrNull(request.getParameter("lote"));

                if (dpi == null || !dpi.matches("\\d{13}")) {
                        request.setAttribute("error", "El DPI debe contener exactamente 13 dígitos.");
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (nombre == null || !nombre.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$")) {
                        request.setAttribute("error", "El nombre solo puede contener letras y espacios.");
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (apellidos == null || !apellidos.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$")) {
                        request.setAttribute("error", "Los apellidos solo pueden contener letras y espacios.");
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (correo == null || !emailOK(correo)) {
                        request.setAttribute("error", "El correo es obligatorio y debe ser válido.");
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (contraseña == null || contraseña.isEmpty()) {
                        request.setAttribute("error", "La contraseña es obligatoria.");
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }

                Integer numeroCasa = null;
                String numeroCasaParam = trimOrNull(request.getParameter("numero_casa"));

                if ("residente".equals(rol) || "administrador".equals(rol)) {
                        if (lote == null || numeroCasaParam == null) {
                                request.setAttribute("error", "El lote y número de casa son obligatorios para residentes y administradores.");
                                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                                return;
                        }
                        try {
                                numeroCasa = Integer.valueOf(numeroCasaParam);
                        } catch (NumberFormatException e) {
                                request.setAttribute("error", "Número de casa inválido.");
                                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                                return;
                        }
                } else {
                        lote = null;
                        numeroCasa = null;
                }

                if (usuarioDAO.existeDpiOCorreo(dpi, correo)) {
                        request.setAttribute("error", "El DPI o el correo ya están registrados.");
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }

                Usuario usuario = new Usuario(dpi, nombre, apellidos, correo, contraseña, rol, lote, numeroCasa);

                try {
                        if (usuarioDAO.insertarUsuario(usuario)) {
                                Usuario creado = usuarioDAO.obtenerUsuario(dpi);
                                String entidadId = creado != null ? String.valueOf(creado.getId()) : dpi;

                                try {
                                        if (creado == null) {
                                                throw new IllegalStateException("No se pudo recuperar el usuario recién creado.");
                                        }

                                        String token = TokenQRUtil.generarTokenResidente(creado.getId());
                                        qrTokenDAO.insertResidentToken(token, creado.getId());

                                        String rutaQR = QRCodeUtil.generarQRDesdeToken(token);
                                        usuarioDAO.actualizarRutaQR(creado.getId(), rutaQR);

                                        final String nombreCompleto = (nombre == null ? "" : nombre) + " " + (apellidos == null ? "" : apellidos);
                                        CorreoUtil.enviarCorreoConQR(correo, rutaQR, nombreCompleto);

                                        flashAndRedirect(request, response, "success",
                                                "Usuario creado correctamente. Se envió el código QR a su correo.");

                                } catch (Exception e) {
                                        e.printStackTrace();
                                        flashAndRedirect(request, response, "warning",
                                                "Usuario creado correctamente, pero no se pudo generar/enviar el código QR.");
                                }

                                // Bitácora: CREAR OK
                                BitacoraService.log(request, "Usuarios", BitacoraService.Accion.CREAR,
                                        "usuarios", entidadId,
                                        "Alta de usuario",
                                        null, toJsonUsuario(creado != null ? creado : usuario),
                                        true, null);

                        } else {
                                // Bitácora: CREAR ERROR
                                BitacoraService.log(request, "Usuarios", BitacoraService.Accion.CREAR,
                                        "usuarios", dpi, "Error al crear el usuario",
                                        null, toJsonUsuario(usuario), false, null);

                                request.setAttribute("error", "Error al crear el usuario.");
                                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        }
                } catch (SQLException e) {
                        // Bitácora: CREAR ERROR BD
                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.CREAR,
                                "usuarios", dpi, "Error de base de datos al crear usuario",
                                null, toJsonUsuario(usuario), false, e);

                        e.printStackTrace();
                        request.setAttribute("error", "Error de base de datos: " + e.getMessage());
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                }
        }

        private void editarUsuario(HttpServletRequest request, HttpServletResponse response)
                throws SQLException, ServletException, IOException {
                String dpi = request.getParameter("dpi");
                Usuario usuario = usuarioDAO.obtenerUsuario(dpi);
                request.setAttribute("usuario", usuario);
                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
        }

        private void actualizarUsuario(HttpServletRequest request, HttpServletResponse response)
                throws SQLException, IOException, ServletException {

                int id = Integer.parseInt(request.getParameter("id"));
                String dpi = trimOrNull(request.getParameter("dpi"));
                String nombre = trimOrNull(request.getParameter("nombre"));
                String apellidos = trimOrNull(request.getParameter("apellidos"));
                String correo = trimOrNull(request.getParameter("correo"));
                String rol = trimOrNull(request.getParameter("rol"));
                String lote = trimOrNull(request.getParameter("lote"));

                if (dpi == null || !dpi.matches("\\d{13}")) {
                        request.setAttribute("error", "El DPI debe contener exactamente 13 dígitos.");
                        request.setAttribute("usuario", new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, null));
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (nombre == null || !nombre.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$")) {
                        request.setAttribute("error", "El nombre solo puede contener letras y espacios.");
                        request.setAttribute("usuario", new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, null));
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (apellidos == null || !apellidos.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+$")) {
                        request.setAttribute("error", "Los apellidos solo pueden contener letras y espacios.");
                        request.setAttribute("usuario", new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, null));
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }
                if (correo == null || !emailOK(correo)) {
                        request.setAttribute("error", "El correo es obligatorio y debe ser válido.");
                        request.setAttribute("usuario", new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, null));
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                        return;
                }

                Integer numeroCasa = null;
                String numeroCasaParam = trimOrNull(request.getParameter("numero_casa"));

                if ("residente".equals(rol) || "administrador".equals(rol)) {
                        if (lote == null || numeroCasaParam == null) {
                                request.setAttribute("error", "El lote y número de casa son obligatorios para residentes y administradores.");
                                request.setAttribute("usuario", new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, null));
                                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                                return;
                        }
                        try {
                                numeroCasa = Integer.valueOf(numeroCasaParam);
                        } catch (NumberFormatException e) {
                                request.setAttribute("error", "Número de casa inválido.");
                                request.setAttribute("usuario", new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, null));
                                request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                                return;
                        }
                } else {
                        lote = null;
                        numeroCasa = null;
                }

                Usuario usuarioAnterior = usuarioDAO.obtenerPorId(id);
                Usuario usuario = new Usuario(id, dpi, nombre, apellidos, correo, "", rol, lote, numeroCasa);

                if (usuarioDAO.actualizarUsuario(usuario)) {
                        boolean requiereNuevoQR
                                = cambiado(nombre, usuarioAnterior != null ? usuarioAnterior.getNombre() : null)
                                || cambiado(apellidos, usuarioAnterior != null ? usuarioAnterior.getApellidos() : null)
                                || cambiado(correo, usuarioAnterior != null ? usuarioAnterior.getCorreo() : null)
                                || cambiado(lote, usuarioAnterior != null ? usuarioAnterior.getLote() : null)
                                || cambiado(numeroCasa, usuarioAnterior != null ? usuarioAnterior.getNumeroCasa() : null);

                        if (requiereNuevoQR) {
                                try {
                                        String token = qrTokenDAO.getActiveResidentTokenByUser(id);
                                        if (token == null) {
                                                token = TokenQRUtil.generarTokenResidente(id);
                                                qrTokenDAO.insertResidentToken(token, id);
                                        }

                                        String rutaNueva = QRCodeUtil.generarQRDesdeToken(token);
                                        usuarioDAO.actualizarRutaQR(id, rutaNueva);

                                        String rutaAnterior = (usuarioAnterior != null) ? usuarioAnterior.getRutaQR() : null;
                                        if (rutaAnterior != null && !rutaAnterior.equals(rutaNueva)) {
                                                try {
                                                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(rutaAnterior));
                                                } catch (Exception exDel) {
                                                        System.err.println("Aviso: no se pudo borrar QR anterior: " + exDel.getMessage());
                                                }
                                        }

                                        CorreoUtil.enviarCorreoQRActualizado(correo, rutaNueva);

                                        flashAndRedirect(request, response, "success",
                                                "Usuario actualizado correctamente. Se envió el nuevo código QR a su correo.");
                                } catch (Exception e) {
                                        e.printStackTrace();
                                        flashAndRedirect(request, response, "warning",
                                                "Usuario actualizado. No se pudo regenerar/enviar el nuevo QR.");
                                }
                        } else {
                                flashAndRedirect(request, response, "success", "Usuario actualizado correctamente.");
                        }

                        // Bitácora: EDITAR OK
                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.EDITAR,
                                "usuarios", String.valueOf(id),
                                "Edición de usuario",
                                toJsonUsuario(usuarioAnterior), toJsonUsuario(usuario),
                                true, null);

                } else {
                        // Bitácora: EDITAR ERROR (no actualizó filas)
                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.EDITAR,
                                "usuarios", String.valueOf(id),
                                "No se actualizó el usuario (0 filas)",
                                toJsonUsuario(usuarioAnterior), toJsonUsuario(usuario),
                                false, null);

                        request.setAttribute("error", "Error al actualizar el usuario.");
                        request.setAttribute("usuario", usuario);
                        request.getRequestDispatcher("formUsuario.jsp").forward(request, response);
                }
        }

        private void eliminarUsuario(HttpServletRequest request, HttpServletResponse response)
                throws SQLException, IOException {
                String dpi = request.getParameter("dpi");

                // Obtener “antes” para la bitácora
                Usuario antes = usuarioDAO.obtenerUsuario(dpi);
                Integer userId = usuarioDAO.obtenerIdPorDpi(dpi);

                // QUIÉN elimina (desde sesión)
                HttpSession ses = request.getSession(false);
                String actorNombre = (ses != null && ses.getAttribute("usuarioNombre") != null)
                        ? String.valueOf(ses.getAttribute("usuarioNombre"))
                        : "desconocido";
                String actorId = (ses != null && ses.getAttribute("usuarioId") != null)
                        ? String.valueOf(ses.getAttribute("usuarioId"))
                        : "?";
                String actor = actorNombre + " (id: " + actorId + ")";

                if (userId != null) {
                        try {
                                qrTokenDAO.revokeResidentTokens(userId);
                        } catch (Exception ignored) {
                        }
                }

                boolean ok;
                try {
                        ok = usuarioDAO.eliminarUsuarioPorDpiConActor(dpi, actor);
                } catch (SQLException e) {
                        ok = usuarioDAO.eliminarUsuarioPorDpi(dpi);
                }

                if (ok) {
                        flashAndRedirect(request, response, "success", "Usuario eliminado correctamente.");

                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.ELIMINAR,
                                "usuarios", userId != null ? String.valueOf(userId) : dpi,
                                "Eliminación de usuario por " + actor, toJsonUsuario(antes), null, true, null);
                } else {
                        flashAndRedirect(request, response, "warning", "No se encontró el usuario a eliminar.");

                        BitacoraService.log(request, "Usuarios", BitacoraService.Accion.ELIMINAR,
                                "usuarios", dpi, "Intento de eliminación: no encontrado",
                                toJsonUsuario(antes), null, false, null);
                }
        }


        /* ================== Helpers ================== */
        private void flashAndRedirect(HttpServletRequest req, HttpServletResponse resp,
                String tipo, String msg) throws IOException {
                HttpSession s = req.getSession();
                s.setAttribute("flash_type", (tipo == null || tipo.trim().isEmpty()) ? "info" : tipo);
                s.setAttribute("flash_msg", msg == null ? "" : msg);
                resp.sendRedirect("UsuarioController");
        }

        private static boolean cambiado(String a, String b) {
                if (a == null && b == null) {
                        return false;
                }
                if (a == null || b == null) {
                        return true;
                }
                return !a.equals(b);
        }

        private static boolean cambiado(Integer a, Integer b) {
                if (a == null && b == null) {
                        return false;
                }
                if (a == null || b == null) {
                        return true;
                }
                return !a.equals(b);
        }

        private static String trimOrNull(String s) {
                return (s == null) ? null : (s.trim().isEmpty() ? null : s.trim());
        }

        private static boolean emailOK(String s) {
                return s != null && s.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
        }

        // -------- Bitácora helpers (JSON simple) --------
        private static String toJsonUsuario(Usuario u) {
                if (u == null) {
                        return null;
                }
                StringBuilder sb = new StringBuilder(200);
                sb.append("{")
                        .append("\"id\":").append(u.getId()).append(',')
                        .append("\"dpi\":\"").append(esc(u.getDpi())).append("\",")
                        .append("\"nombre\":\"").append(esc(u.getNombre())).append("\",")
                        .append("\"apellidos\":\"").append(esc(u.getApellidos())).append("\",")
                        .append("\"correo\":\"").append(esc(u.getCorreo())).append("\",")
                        .append("\"rol\":\"").append(esc(u.getRol())).append("\",")
                        .append("\"lote\":").append(u.getLote() == null ? "null" : "\"" + esc(u.getLote()) + "\"").append(',')
                        .append("\"numeroCasa\":").append(u.getNumeroCasa() == null ? "null" : u.getNumeroCasa())
                        .append("}");
                return sb.toString();
        }

        private static String esc(String s) {
                if (s == null) {
                        return "";
                }
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
