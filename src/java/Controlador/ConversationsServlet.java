package Controlador;

import Chat.dao.ConversationDAO;
import Chat.dao.UserLookupDAO;
import Chat.model.Conversation;
import Chat.service.ChatService;
import Chat.util.JsonUtil;
import Servicio.AuthService.AuthUser;

import javax.json.JsonObject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet("/api/chat/conversaciones")
public class ConversationsServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();
    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final UserLookupDAO userDAO = new UserLookupDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"error\":\"NO_AUTH\"}");
            return;
        }

        AuthUser u = (AuthUser) s.getAttribute("user");

        // Rol desde BD (no confíes en sesión)
        UserLookupDAO.UserCore yo = userDAO.getById(u.id);
        String R = (yo != null && yo.rol != null) ? yo.rol.trim().toUpperCase() : "";
        boolean esResidente = !("AGENTE".equals(R) || "GUARDIA".equals(R) || "SEGURIDAD".equals(R));
        if (!esResidente) {
            System.out.println("[CONV][403] no residente. userId=" + u.id + " rolBD=" + R);
            resp.setStatus(403);
            resp.getWriter().write("{\"error\":\"Solo residentes pueden crear\"}");
            return;
        }

        String raw = req.getReader().lines().collect(java.util.stream.Collectors.joining());
        javax.json.JsonObject body;
        try {
            body = JsonUtil.parse(raw);
        } catch (Exception ex) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"JSON inválido\"}");
            return;
        }

        if (!body.containsKey("agentId")) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"agentId requerido\"}");
            return;
        }

        int agenteId;
        try {
            agenteId = body.getInt("agentId");
        } catch (Exception ex) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"agentId inválido\"}");
            return;
        }

        System.out.println("[CONV] userId=" + u.id + " rolBD=" + R + " agenteId=" + agenteId);

        // El agente debe existir y ser guardia/agente/seguridad (según tu esquema)
        if (!userDAO.esGuardiaRegistradoActivo(agenteId)) {
            System.out.println("[CONV][400] agente " + agenteId + " no es guardia registrado/activo");
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"El usuario seleccionado no es un guardia activo\"}");
            return;
        }

        // No permitir conversación contigo mismo, por si el id coincide
        if (agenteId == u.id) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"No puedes crear conversación contigo mismo\"}");
            return;
        }

        // Evitar duplicados
        try {
            if (conversationDAO.existeEntre(u.id, agenteId)) {
                System.out.println("[CONV][409] ya existe conversación residente=" + u.id + " agente=" + agenteId);
                resp.setStatus(409);
                resp.getWriter().write("{\"error\":\"Ya existe una conversacion con el usuario seleccionado\"}");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"Error consultando conversaciones existentes\"}");
            return;
        }

        try {
            Conversation c = chatService.abrirConversacion(u.id, agenteId);
            resp.getWriter().write("{\"conversationId\":" + c.id + "}");
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.setStatus(500);
            // devolvemos el mensaje para que lo veas en el front (temporal para depurar)
            String msg = ex.getMessage();
            if (msg == null) {
                msg = "No se pudo abrir la conversación";
            }
            msg = msg.replace("\"", "\\\"");
            resp.getWriter().write("{\"error\":\"" + msg + "\"}");
        }
    }

}
