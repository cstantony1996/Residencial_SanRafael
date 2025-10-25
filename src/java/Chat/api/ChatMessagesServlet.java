package Chat.api;

import Chat.dao.ConversationDAO;
import Chat.dao.MessageDAO;
import Chat.model.Message;
import Servicio.AuthService.AuthUser;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;


// mapea la ruta
@WebServlet("/api/chat/mensajes") //registra el servelet dentro de esa ruta || EXTENDS hereda los doGet y doPost
public class ChatMessagesServlet extends HttpServlet {

        private final ConversationDAO conversationDAO = new ConversationDAO();
        private final MessageDAO messageDAO = new MessageDAO();

        @Override //sobreescribe el metodo de la superclase(HttpServlet)
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                AuthUser u = (AuthUser) req.getSession().getAttribute("user");
                if (u == null) {
                        resp.setStatus(401); // no autenticado
                        resp.setContentType("application/json; charset=UTF-8");
                        resp.getWriter().write("{\"error\":\"No autenticado\"}");
                        return;
                }

                long convId;
                try {
                        convId = Long.parseLong(req.getParameter("conversationId"));
                } catch (Exception e) {
                        resp.setStatus(400); // falto o fue invalido es decir, no hay conversacion
                        resp.setContentType("application/json; charset=UTF-8");
                        resp.getWriter().write("{\"error\":\"conversationId requerido\"}");
                        return;
                }

                int limit = 200;
                try {
                        String l = req.getParameter("limit");
                        if (l != null) {
                                int n = Integer.parseInt(l);
                                limit = Math.min(500, Math.max(1, n));
                        }
                } catch (Exception ignore) {
                }

                if (!conversationDAO.usuarioPertenece(convId, u.id)) {
                        resp.setStatus(403); // autenticado, pero sin permisos para esa conversacion, es decir, que no pertenece a esa conversacion
                        resp.setContentType("application/json; charset=UTF-8");
                        resp.getWriter().write("{\"error\":\"No pertenece a la conversación\"}");
                        return;
                }

                
                List<Message> list = messageDAO.listarPorConversacion(convId, limit);

                JsonArrayBuilder arr = Json.createArrayBuilder();
                for (Message m : list) {
                        JsonObjectBuilder o = Json.createObjectBuilder()
                                .add("id", m.id)
                                .add("conversationId", m.conversacionId)
                                .add("fromRole", m.fromRole.name())
                                .add("text", m.texto)
                                .add("ts", m.tsIso == null ? "" : m.tsIso); // cliente ya soporta ts/tsIso
                        arr.add(o);
                }

                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write(arr.build().toString());
        }
}
