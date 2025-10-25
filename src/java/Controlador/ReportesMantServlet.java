package Controlador;

import Utils.CorreoUtil;
import UsuarioDAO.UsuarioDAO;
import Conexion_DB.Conexion;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.List;

@WebServlet(
        name = "ReportesMantServlet",
        urlPatterns = {"/reporteDeMantenimiento", "/vistas/reporteDeMantenimiento"}
)
public class ReportesMantServlet extends HttpServlet {

        private UsuarioDAO usuarioDAO;

        // Constantes de bitácora
        private static final String MODULO = "REPORTES_MANT";
        private static final String ENTIDAD = "REPORTE_MANTENIMIENTO";
        private static final String OPERACION = "CREAR_Y_ENVIAR";

        // Schema bitácora (auto-setup)
        private static volatile boolean BITACORA_READY = false;

        @Override
        public void init() throws ServletException {
                usuarioDAO = new UsuarioDAO();
                ensureBitacoraSchema();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
                req.getRequestDispatcher("/vistas/reportesMant.jsp").forward(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

                req.setCharacterEncoding("UTF-8");
                resp.setCharacterEncoding("UTF-8");

                String tipo = n(req.getParameter("tipo"));
                String otrosDetalle = n(req.getParameter("otrosDetalle"));
                String descripcion = n(req.getParameter("descripcion"));
                String fechaHora = n(req.getParameter("fechaHora"));

                if (tipo.isEmpty() || descripcion.isEmpty() || fechaHora.isEmpty()) {
                        // Bitácora: intento inválido
                        logBitacora(req, getUserId(req), getUserEmail(req), false,
                                "Campos requeridos vacíos",
                                json(
                                        "tipo", tipo,
                                        "otrosDetalle_len", descripcion.length(),
                                        "descripcion_len", descripcion.length(),
                                        "fechaHora", fechaHora
                                ));
                        resp.sendRedirect(req.getContextPath() + "/vistas/reportesMant.jsp?status=err");
                        return;
                }

                String tipoMostrado = "otros".equalsIgnoreCase(tipo) && !otrosDetalle.isEmpty()
                        ? "Otros: " + otrosDetalle
                        : tipo;

                // ===== Nombre del residente desde sesión =====
                String residenteNombre = "Usuario del sistema";
                Object userObj = req.getSession().getAttribute("user");
                if (userObj != null) {
                        String nom = firstNonEmpty(tryGetter(userObj, "getNombre"), tryField(userObj, "nombre"));
                        String ape = firstNonEmpty(tryGetter(userObj, "getApellidos"), tryField(userObj, "apellidos"));
                        String full = (n(nom) + " " + n(ape)).trim();
                        if (!full.isEmpty()) {
                                residenteNombre = full;
                        }
                }

                // ===== Cuerpo del correo =====
                String asunto = "Reporte de Mantenimiento - " + tipoMostrado;

                String html
                        = "<p><b>Nuevo reporte de mantenimiento</b></p>"
                        + "<p>El residente <b>" + esc(residenteNombre) + "</b> ha ingresado un reporte del sistema. El detalle del reporte es:</p>"
                        + "<ul style='margin-left:18px'>"
                        + "<li><b>Tipo de inconveniente:</b> " + esc(tipoMostrado) + "</li>"
                        + "<li><b>Descripción:</b> " + esc(descripcion).replace("\n", "<br>") + "</li>"
                        + "<li><b>Fecha y hora:</b> " + esc(fechaHora) + "</li>"
                        + "</ul>"
                        + "<p>Por favor, tomar las acciones correspondientes.</p>";

                String plano
                        = "Nuevo reporte de mantenimiento\n"
                        + "El residente: " + residenteNombre + " ha ingresado un reporte del sistema. El detalle del reporte es:\n"
                        + "Tipo de inconveniente: " + tipoMostrado + "\n"
                        + "Descripción: " + descripcion + "\n"
                        + "Fecha y hora: " + fechaHora + "\n\n"
                        + "Por favor, tomar las acciones correspondientes.";

                // ===== Envío =====
                boolean enviado = false;
                int adminCount = 0;
                try {
                        List<String> admins = usuarioDAO.obtenerCorreosAdministradores();
                        adminCount = (admins == null) ? 0 : admins.size();
                        if (admins != null && !admins.isEmpty()) {
                                String[] tos = admins.toArray(new String[0]);
                                CorreoUtil.enviarSoporteReporteMantenimiento(tos, asunto, html, plano);
                                enviado = true;
                        }
                } catch (Exception ex) {
                        ex.printStackTrace();
                }

                // ===== Guardar datos en sesión para el PDF de descarga =====
                if (enviado) {
                        HttpSession s = req.getSession();
                        s.setAttribute("rm_nombre", residenteNombre);
                        s.setAttribute("rm_tipo", tipoMostrado);
                        s.setAttribute("rm_desc", descripcion);
                        s.setAttribute("rm_fecha", fechaHora);
                }

                // ===== Bitácora del evento =====
                String detalle = enviado ? "Reporte enviado a administración" : "Fallo al enviar reporte";
                logBitacora(req, getUserId(req), getUserEmail(req), enviado, detalle,
                        json(
                                "tipo", tipoMostrado,
                                "desc_len", descripcion.length(),
                                "fechaHora", fechaHora,
                                "admin_recipients", adminCount,
                                "asunto", asunto
                        ));

                // Redirigir con status para que el JSP muestre aviso y botón Descargar
                String target = req.getContextPath() + "/vistas/reportesMant.jsp?status=" + (enviado ? "ok" : "err");
                resp.sendRedirect(target);
        }

        /* ================= bitácora: schema & write ================= */
        private static void ensureBitacoraSchema() {
                if (BITACORA_READY) {
                        return;
                }
                synchronized (ReportesMantServlet.class) {
                        if (BITACORA_READY) {
                                return;
                        }
                        try (Connection con = Conexion.getConnection();
                                Statement st = con.createStatement()) {
                                st.execute(
                                        "CREATE TABLE IF NOT EXISTS bitacora_eventos ("
                                        + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                                        + "  momento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                        + "  actor_user_id INT NULL,"
                                        + "  actor_email VARCHAR(150) NULL,"
                                        + "  ip VARCHAR(45) NULL,"
                                        + "  modulo VARCHAR(60) NOT NULL,"
                                        + "  entidad VARCHAR(60) NOT NULL,"
                                        + "  operacion VARCHAR(40) NOT NULL,"
                                        + "  entidad_id VARCHAR(64) NULL,"
                                        + "  exito TINYINT(1) NOT NULL,"
                                        + "  detalle TEXT NULL,"
                                        + "  metadata LONGTEXT NULL,"
                                        + "  KEY idx_modulo (modulo),"
                                        + "  KEY idx_entidad_op (entidad, operacion),"
                                        + "  KEY idx_actor (actor_user_id),"
                                        + "  KEY idx_momento (momento)"
                                        + ")"
                                );
                                BITACORA_READY = true;
                        } catch (Exception e) {
                                System.err.println("[WARN] bitacora schema: " + e.getMessage());
                        }
                }
        }

        private static void logBitacora(HttpServletRequest req,
                Integer actorUserId,
                String actorEmail,
                boolean exito,
                String detalle,
                String metadataJson) {
                ensureBitacoraSchema();
                final String sql = "INSERT INTO bitacora_eventos "
                        + "(actor_user_id, actor_email, ip, modulo, entidad, operacion, entidad_id, exito, detalle, metadata) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)";
                String ip = clientIp(req);
                try (Connection con = Conexion.getConnection();
                        PreparedStatement ps = con.prepareStatement(sql)) {
                        if (actorUserId == null) {
                                ps.setNull(1, Types.INTEGER);
                        } else {
                                ps.setInt(1, actorUserId);
                        }
                        ps.setString(2, trimLen(actorEmail, 150));
                        ps.setString(3, trimLen(ip, 45));
                        ps.setString(4, MODULO);
                        ps.setString(5, ENTIDAD);
                        ps.setString(6, OPERACION);
                        ps.setNull(7, Types.VARCHAR); // entidad_id (no hay id persistido para este caso)
                        ps.setInt(8, exito ? 1 : 0);
                        if (detalle == null) {
                                ps.setNull(9, Types.LONGVARCHAR);
                        } else {
                                ps.setString(9, detalle);
                        }
                        if (metadataJson == null) {
                                ps.setNull(10, Types.LONGVARCHAR);
                        } else {
                                ps.setString(10, metadataJson);
                        }
                        ps.executeUpdate();
                } catch (Exception e) {
                        System.err.println("[WARN] bitacora insert: " + e.getMessage());
                }
        }

        /* ================= helpers ================= */
        private static String n(String s) {
                return (s == null) ? "" : s.trim();
        }

        private static String esc(String s) {
                if (s == null) {
                        return "";
                }
                return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        private static String tryGetter(Object o, String method) {
                try {
                        java.lang.reflect.Method m = o.getClass().getMethod(method);
                        Object v = m.invoke(o);
                        return v == null ? "" : String.valueOf(v).trim();
                } catch (Exception e) {
                        return "";
                }
        }

        private static String tryField(Object o, String field) {
                try {
                        java.lang.reflect.Field f = o.getClass().getField(field);
                        Object v = f.get(o);
                        return v == null ? "" : String.valueOf(v).trim();
                } catch (Exception e) {
                        return "";
                }
        }

        private static String firstNonEmpty(String... vals) {
                if (vals == null) {
                        return "";
                }
                for (String s : vals) {
                        if (s != null && !s.trim().isEmpty()) {
                                return s.trim();
                        }
                }
                return "";
        }

        private static Integer getUserId(HttpServletRequest req) {
                Object u = (req == null) ? null : req.getSession().getAttribute("user");
                if (u == null) {
                        return null;
                }
                String v = firstNonEmpty(tryGetter(u, "getId"), tryField(u, "id"));
                try {
                        return v.isEmpty() ? null : Integer.valueOf(v);
                } catch (Exception e) {
                        return null;
                }
        }

        private static String getUserEmail(HttpServletRequest req) {
                Object u = (req == null) ? null : req.getSession().getAttribute("user");
                if (u == null) {
                        return null;
                }
                String v = firstNonEmpty(tryGetter(u, "getCorreo"), tryField(u, "correo"), tryGetter(u, "getEmail"), tryField(u, "email"));
                return v == null ? null : v;
        }

        private static String clientIp(HttpServletRequest req) {
                if (req == null) {
                        return null;
                }
                String xf = req.getHeader("X-Forwarded-For");
                if (xf != null && !xf.trim().isEmpty()) {
                        // El primero de la lista suele ser el IP original
                        int c = xf.indexOf(',');
                        return (c > 0 ? xf.substring(0, c) : xf).trim();
                }
                return req.getRemoteAddr();
        }

        // JSON mínimo (sin libs)
        private static String jstr(String s) {
                if (s == null) {
                        return "null";
                }
                return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        private static String jsonKV(String k, Object v) {
                String key = jstr(k);
                String val;
                if (v == null) {
                        val = "null";
                } else if (v instanceof Number || v instanceof Boolean) {
                        val = String.valueOf(v);
                } else {
                        val = jstr(String.valueOf(v));
                }
                return key + ":" + val;
        }

        private static String json(Object... kvPairs) {
                try {
                        StringBuilder sb = new StringBuilder("{");
                        for (int i = 0; i < kvPairs.length; i += 2) {
                                if (i > 0) {
                                        sb.append(',');
                                }
                                sb.append(jsonKV(String.valueOf(kvPairs[i]),
                                        (i + 1 < kvPairs.length) ? kvPairs[i + 1] : null));
                        }
                        return sb.append('}').toString();
                } catch (Exception e) {
                        return null;
                }
        }

        private static String trimLen(String s, int max) {
                if (s == null) {
                        return null;
                }
                if (s.length() <= max) {
                        return s;
                }
                return s.substring(0, Math.max(0, max));
        }
}
