package Controlador;

import Utils.SerialBridge;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(name = "ArduinoPulseServlet", urlPatterns = {"/arduino/pulse"})
public class ArduinoPulseServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        write(resp, "{\"alive\":true}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        // Modo FAKE opcional para probar sin Arduino
        boolean fake = "1".equals(System.getenv("ARDUINO_FAKE"))
                || "true".equalsIgnoreCase(System.getProperty("arduino.fake", "false"));

        String estado = trim(req.getParameter("estado"));     // "permitido" | "denegado"
        String direccion = trim(req.getParameter("direccion"));  // "entrada"  | "salida"

        if (!"permitido".equalsIgnoreCase(estado)) {
            write(resp, "{\"ok\":false,\"msg\":\"Backend denegó el acceso; no se pulsa.\"}");
            return;
        }
        if (direccion == null || direccion.isEmpty()) {
            write(resp, "{\"ok\":false,\"msg\":\"Falta 'direccion' (entrada|salida)\"}");
            return;
        }

        // Si está en modo FAKE, simula OK y listo
        if (fake) {
            String sent = "entrada".equalsIgnoreCase(direccion) ? "PULSE;E" : "PULSE;S";
            write(resp, "{\"ok\":true,\"fake\":true,\"sent\":\"" + esc(sent) + "\"}");
            return;
        }

        // Lógica real hacia el puerto serie
        try {
            boolean ok;
            String sent;

            if ("entrada".equalsIgnoreCase(direccion)) {
                ok = SerialBridge.pulseEntrada();
                sent = "PULSE;E";
            } else if ("salida".equalsIgnoreCase(direccion)) {
                ok = SerialBridge.pulseSalida();
                sent = "PULSE;S";
            } else {
                write(resp, "{\"ok\":false,\"msg\":\"'direccion' invalida\"}");
                return;
            }

            if (!ok) {
                String why = SerialBridge.getLastError();
                if (why == null || why.isEmpty()) {
                    why = "No se pudo escribir al puerto " + SerialBridge.getPortName();
                }
                write(resp, "{\"ok\":false,\"sent\":\"" + esc(sent) + "\",\"msg\":\"" + esc(why) + "\"}");
                return;
            }

            write(resp, "{\"ok\":true,\"sent\":\"" + esc(sent) + "\"}");

        } catch (Throwable t) { // ← capturamos TODO (incluye NoClassDefFoundError)
            String msg = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
            write(resp, "{\"ok\":false,\"msg\":\"" + esc(msg) + "\"}");
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static void write(HttpServletResponse r, String s) throws IOException {
        try (OutputStream os = r.getOutputStream()) {
            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
