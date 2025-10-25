package Controlador;

import Servicio.AuthService;
import Servicio.AuthService.AuthUser;
import Servicio.BitacoraService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

        private final AuthService auth = new AuthService();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                // Evita cachear la pantalla de login
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);
                req.setCharacterEncoding("UTF-8");
                resp.setCharacterEncoding("UTF-8");

                HttpSession s = req.getSession(false);
                if (s != null && s.getAttribute("user") != null) {
                        // Ya hay sesión: enruta según rol
                        String roleKey = (String) s.getAttribute("rol");
                        if (roleKey == null) {
                                AuthUser u = (AuthUser) s.getAttribute("user");
                                roleKey = mapRole(u != null ? u.rol : null);
                                s.setAttribute("rol", roleKey);
                        }
                        redirectByRoleKey(roleKey, resp, req);
                        return;
                }

                // Muestra login
                req.getRequestDispatcher("/vistas/login.jsp").forward(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                req.setCharacterEncoding("UTF-8");
                resp.setCharacterEncoding("UTF-8");

                final String nombre = str(req.getParameter("nombre"));
                final String password = str(req.getParameter("password"));

                try {
                        AuthUser user = auth.login(nombre, password);

                        if (user == null) {
                                // Bitácora: login fallido
                                BitacoraService.log(req, "Autenticación", BitacoraService.Accion.LOGIN,
                                        "sesion", nombre, "Inicio de sesión fallido (credenciales inválidas)",
                                        null, null, false, null);

                                req.setAttribute("error", "Usuario o contraseña incorrectos.");
                                req.getRequestDispatcher("/vistas/login.jsp").forward(req, resp);
                                return;
                        }

                        // Session fixation: invalidar y crear nueva
                        HttpSession old = req.getSession(false);
                        if (old != null) {
                                old.invalidate();
                        }

                        HttpSession s = req.getSession(true);
                        s.setMaxInactiveInterval(45 * 60); // 45 min

                        // Rol interno (ADMIN|GUARDIA|RESIDENTE)
                        final String roleKey = mapRole(user.rol);

                        // -------- Atributos de sesión estandarizados --------
                        s.setAttribute("user", user);                 // objeto completo
                        s.setAttribute("rol", roleKey);               // rol normalizado
                        s.setAttribute("usuarioRol", user.rol);       // rol tal como viene de BD
                        s.setAttribute("usuarioNombre", nz(user.nombre));
                        s.setAttribute("usuarioCorreo", user.correo);
                        s.setAttribute("usuarioId", user.id);         // <<-- NECESARIO para /pagos/calcular y registrar

                        // DPI / identificador humano (si no hay, usa id)
                        String dpi = pickField(user, "dpi", "dpiUsuario", "noDpi", "identificacion");
                        if (isEmpty(dpi)) {
                                dpi = String.valueOf(user.id);
                        }
                        s.setAttribute("usuarioDPI", dpi);

                        // Número de casa (si existe en AuthUser)
                        Integer numCasa = parseIntOrNull(pickField(user, "numeroCasa", "numCasa", "casa", "residencia"));
                        if (numCasa != null) {
                                s.setAttribute("usuarioNumeroCasa", numCasa);
                        }

                        // Bitácora: login OK
                        BitacoraService.log(req, "Autenticación", BitacoraService.Accion.LOGIN,
                                "sesion", dpi, "Inicio de sesión exitoso",
                                null, null, true, null);

                        // Redirige por rol
                        redirectByRoleKey(roleKey, resp, req);

                } catch (Exception ex) {
                        // Bitácora: error inesperado
                        BitacoraService.log(req, "Autenticación", BitacoraService.Accion.LOGIN,
                                "sesion", nombre, "Error durante autenticación",
                                null, null, false, ex);

                        req.setAttribute("error", "Error al autenticar.");
                        req.getRequestDispatcher("/vistas/login.jsp").forward(req, resp);
                }
        }

        // ----------------- helpers -----------------
        private static String str(String v) {
                return v == null ? "" : v.trim();
        }

        private static String nz(String v) {
                return v == null ? "" : v;
        }

        private static boolean isEmpty(String s) {
                return s == null || s.trim().isEmpty();
        }

        /**
         * Normaliza cadenas de rol de BD a claves internas.
         */
        private static String mapRole(String rolRaw) {
                String r = (rolRaw == null ? "" : rolRaw).toLowerCase(Locale.ROOT).trim();

                // ADMIN
                if (r.equals("administrador de residencial") || r.equals("administrador")) {
                        return "ADMIN";
                }
                // GUARDIA
                if (r.equals("agente") || r.equals("agente de seguridad de residencial")
                        || r.equals("agente de seguridad") || r.equals("guardia")) {
                        return "GUARDIA";
                }
                // RESIDENTE
                if (r.equals("residente") || r.equals("residencial")) {
                        return "RESIDENTE";
                }

                // Palabras clave de respaldo
                if (r.contains("admin")) {
                        return "ADMIN";
                }
                if (r.contains("agente") || r.contains("seguridad") || r.contains("guardia") || r.contains("garita")) {
                        return "GUARDIA";
                }
                if (r.contains("resident")) {
                        return "RESIDENTE";
                }

                return "RESIDENTE"; // fallback seguro
        }

        /**
         * Redirige al menú según rol interno.
         */
        private void redirectByRoleKey(String roleKey, HttpServletResponse resp, HttpServletRequest req)
                throws IOException {
                String ctx = req.getContextPath();
                if ("ADMIN".equals(roleKey)) {
                        resp.sendRedirect(ctx + "/vistas/menuAdmin.jsp");
                        return;
                }
                if ("GUARDIA".equals(roleKey)) {
                        resp.sendRedirect(ctx + "/vistas/menuGuardia.jsp");
                        return;
                }
                resp.sendRedirect(ctx + "/vistas/menuResidente.jsp");
        }

        /**
         * Intenta leer un campo público por reflexión sin romper compilación si
         * no existe. Útil si en el futuro agregas dpi/numeroCasa/correo como
         * campos públicos en AuthUser.
         */
        private static String pickField(Object obj, String... fields) {
                if (obj == null) {
                        return null;
                }
                for (String f : fields) {
                        try {
                                Field fld = obj.getClass().getField(f);
                                Object val = fld.get(obj);
                                if (val != null) {
                                        return String.valueOf(val);
                                }
                        } catch (NoSuchFieldException ignore) {
                        } catch (Exception e) {
                                // si no se puede leer, sigue
                        }
                }
                return null;
        }

        private static Integer parseIntOrNull(String s) {
                try {
                        return s == null ? null : Integer.valueOf(s.trim());
                } catch (Exception e) {
                        return null;
                }
        }
}
