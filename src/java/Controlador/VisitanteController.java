package Controlador;

import Servicio.VisitaService;
import modelo.Visita;
import Servicio.AuthService.AuthUser;
import Conexion_DB.Conexion;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;

// ZXing para servir el PNG directamente
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

@WebServlet(urlPatterns = {"/visitante", "/visitante/registrar", "/visitante/cancelar", "/visitante/qr"})
public class VisitanteController extends HttpServlet {

    private final VisitaService svc = new VisitaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/visitante/qr".equals(path)) {
            servirQR(req, resp);
            return;
        }
        req.getRequestDispatcher("/vistas/registrar_visitante.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/visitante/registrar".equals(path)) {
            registrar(req, resp);
            return;
        }
        if ("/visitante/cancelar".equals(path)) {
            cancelar(req, resp);
            return;
        }
        resp.sendError(404);
    }

    private void registrar(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");

        HttpSession ses = req.getSession(false);
        if (ses == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // 1) Obtener datos del residente desde la sesión (soporta ambos esquemas)
        Integer residenteId = (Integer) ses.getAttribute("usuarioId");
        String correoResidente = (String) ses.getAttribute("usuarioCorreo");
        String nombreResidente = (String) ses.getAttribute("usuarioNombre");

        if (residenteId == null) {
            AuthUser u = (AuthUser) ses.getAttribute("user");
            if (u != null) {
                residenteId = u.id;
                if (nombreResidente == null) {
                    nombreResidente = u.nombre;
                }
            }
        }
        if (residenteId == null) {  // sigue sin sesión válida
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Si no tenemos correo/nombre, sácalos de BD
        if (correoResidente == null || nombreResidente == null) {
            try (Connection cn = Conexion.getConnection();
                    PreparedStatement ps = cn.prepareStatement(
                            "SELECT correo, TRIM(CONCAT(COALESCE(nombre,''),' ',COALESCE(apellidos,''))) AS nom "
                            + "FROM usuarios WHERE id=?")) {
                ps.setInt(1, residenteId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (correoResidente == null) {
                            correoResidente = nvlTrim(rs.getString("correo"));
                        }
                        if (nombreResidente == null) {
                            nombreResidente = nvlTrim(rs.getString("nom"));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // 2) Parámetros del formulario
        String nombre = trim(req.getParameter("nombre"));
        String dpi = trim(req.getParameter("dpi"));
        String correoVis = trim(req.getParameter("correo"));
        String tipoQr = trim(req.getParameter("tipoVisita"));       // "intentos" | "tiempo"
        String sIntentos = trim(req.getParameter("intentos"));
        String sExpira = trim(req.getParameter("expiraEn"));

        // 3) Validaciones
        if (!emailOK(correoVis)) {
            req.setAttribute("error", "El correo del visitante es obligatorio y debe ser válido.");
            forward(req, resp);
            return;
        }
        if (!emailOK(correoResidente)) {
            req.setAttribute("error", "Tu perfil no tiene correo válido. Actualízalo en tu cuenta.");
            forward(req, resp);
            return;
        }

        Integer intentos = null;
        Timestamp expira = null;
        if (tipoQr == null || "intentos".equalsIgnoreCase(tipoQr)) {
            intentos = parseInt(sIntentos);
            if (intentos == null || intentos < 2) {
                req.setAttribute("error", "Intentos debe ser ≥ 2");
                forward(req, resp);
                return;
            }
            tipoQr = "intentos";
        } else {
            if (sExpira == null) {
                req.setAttribute("error", "Debe indicar la fecha/hora de expiración");
                forward(req, resp);
                return;
            }
            expira = Timestamp.valueOf(sExpira.replace("T", " ") + ":00");
            if (expira.toLocalDateTime().isBefore(LocalDateTime.now())) {
                req.setAttribute("error", "La fecha/hora no puede ser pasada");
                forward(req, resp);
                return;
            }
            tipoQr = "tiempo";
        }

        // 4) Crear visita + correos + URLs de Ver/Descargar
        try {
            String baseAppUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath();

            Visita v = svc.crearVisitaYNotificar(
                    residenteId, nombre, dpi, correoVis,
                    tipoQr, intentos, expira,
                    baseAppUrl, nombreResidente, correoResidente
            );

            String tokenEnc = URLEncoder.encode(v.getToken(), StandardCharsets.UTF_8.name());
            String viewUrl = req.getContextPath() + "/visitante/qr?token=" + tokenEnc;
            String downUrl = viewUrl + "&download=1";

            req.setAttribute("ok", "QR de visitante generado y enviado por correo.");
            req.setAttribute("qrViewUrl", viewUrl);
            req.setAttribute("qrDownloadUrl", downUrl);

            forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Error registrando visita: " + e.getMessage());
            forward(req, resp);
        }
    }

    private void cancelar(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession ses = req.getSession(false);
        if (ses == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        Integer residenteId = (Integer) ses.getAttribute("usuarioId");
        if (residenteId == null) {
            AuthUser u = (AuthUser) ses.getAttribute("user");
            if (u != null) {
                residenteId = u.id;
            }
        }
        if (residenteId == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        long id = Long.parseLong(req.getParameter("id"));
        try {
            boolean ok = svc.cancelarVisita(id, residenteId);
            req.setAttribute(ok ? "ok" : "error", ok ? "Visita cancelada" : "No se pudo cancelar");
            forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("error", "Error cancelando visita");
            forward(req, resp);
        }
    }

    /* ======== Sirve PNG del QR (inline o attachment) ======== */
    private void servirQR(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = trim(req.getParameter("token"));
        if (token == null) {
            token = trim(req.getParameter("tk"));
        }
        if (token == null) {
            resp.sendError(400, "token requerido");
            return;
        }

        boolean download = "1".equals(req.getParameter("download"));

        resp.setContentType("image/png");
        resp.setHeader("Content-Disposition", (download ? "attachment" : "inline") + "; filename=\"QR_visita.png\"");

        try {
            QRCodeWriter qr = new QRCodeWriter();
            BitMatrix matrix = qr.encode(token, BarcodeFormat.QR_CODE, 512, 512);
            MatrixToImageWriter.writeToStream(matrix, "PNG", resp.getOutputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.sendError(500, "No se pudo generar la imagen PNG.");
        }
    }

    /* ========== Helpers ========== */
    private void forward(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/vistas/registrar_visitante.jsp").forward(req, resp);
    }

    private static boolean emailOK(String s) {
        return s != null && s.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    private static String trim(String s) {
        return s == null ? null : (s.trim().isEmpty() ? null : s.trim());
    }

    private static Integer parseInt(String s) {
        try {
            return s == null ? null : Integer.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String nvlTrim(String s) {
        return s == null ? null : (s.trim().isEmpty() ? null : s.trim());
    }
}
