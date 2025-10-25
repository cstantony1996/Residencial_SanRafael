package Controlador;

import Chat.service.IncidentService;
import Chat.dao.UserLookupDAO;
import Chat.util.JsonUtil;
import Servicio.AuthService.AuthUser;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.stream.Collectors;

@WebServlet("/api/chat/incidentes")
public class IncidentsServlet extends HttpServlet {

    private final IncidentService incidentService = new IncidentService();
    private final UserLookupDAO userDAO = new UserLookupDAO();
    private final Chat.service.NotificationService notifier = new Chat.service.NotificationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"error\":\"NO_AUTH\"}");
            return;
        }
        AuthUser user = (AuthUser) s.getAttribute("user");

        String raw = req.getReader().lines().collect(Collectors.joining());
        JsonObject body = JsonUtil.parse(raw);

        String tipo = body.getString("incidentType", null);
        String descripcion = body.getString("description", "");
        String when = body.getString("occuredAt", null);

        if (tipo == null || tipo.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Tipo requerido\"}");
            return;
        }
        if (when == null || when.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Fecha/hora requerida\"}");
            return;
        }

        Timestamp ts = Timestamp.valueOf((when.length() == 16 ? when + ":00" : when).replace('T', ' '));

        long id;
        try {
            id = incidentService.reportar(user.id, tipo, ts, descripcion);
        } catch (IllegalArgumentException ex) {
            // Validaciones de negocio del servicio (descripcion vacía, >200, etc.)
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"" + ex.getMessage().replace("\"", "\\\"") + "\"}");
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"No se pudo crear el incidente\"}");
            return;
        }

        JsonObject res = Json.createObjectBuilder()
                .add("ok", true)
                .add("incidentId", id)
                .build();
        resp.getWriter().write(JsonUtil.stringify(res));

        // notificar guardias (formato del CU)
        UserLookupDAO.UserCore residente = userDAO.getById(user.id);
        String asunto = "Reporte de incidente";
        String cuerpo = "Se le informa que el residente " + (residente != null ? residente.nombre : "(desconocido)")
                + ", que vive en " + (residente != null ? residente.lote : "?") + ", "
                + (residente != null ? residente.numeroCasa : "?")
                + ", ha reportado un incidente, a continuacion los detalles :\n"
                + tipo + "\n" + ts + "\n" + descripcion + "\n\n"
                + "Por favor, tomar las acciones correspondientes.";

        for (UserLookupDAO.UserCore g : userDAO.listarGuardiasActivos()) {
            notifier.notificarIncidente(g.correo, asunto, cuerpo);
        }
    }
}

//eramirezc30@miumg.edu.gt
