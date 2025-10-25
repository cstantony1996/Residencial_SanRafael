package Controlador;

import Chat.dao.ConversationDAO;
import Chat.dao.MessageDAO;
import Chat.model.Conversation;
import Chat.model.Message;
import Chat.dao.UserLookupDAO;
import Servicio.AuthService.AuthUser;

import javax.json.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/chat/mis-conversaciones")
public class MyConversationsServlet extends HttpServlet {

    private final ConversationDAO dao = new ConversationDAO();
    private final UserLookupDAO userDAO = new UserLookupDAO();
    private final MessageDAO msgDAO = new MessageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        HttpSession s = req.getSession(false);
        AuthUser u = (AuthUser) (s != null ? s.getAttribute("user") : null);
        if (u == null) {
            resp.setStatus(401);
            return;
        }

        boolean esResidente = true;
        String rol = (String) (s.getAttribute("rol"));
        if (rol != null) {
            String R = rol.trim().toUpperCase();
            esResidente = !("AGENTE".equals(R) || "GUARDIA".equals(R) || "SEGURIDAD".equals(R));
        }

        try {
            List<Conversation> list = dao.listarPorUsuario(u.id, esResidente);
            JsonArrayBuilder arr = Json.createArrayBuilder();

            for (Conversation c : list) {
                // Nombre del “otro”
                String peerName;
                if (esResidente) {
                    UserLookupDAO.UserCore ag = userDAO.getById(c.agenteId);
                    peerName = (ag != null && !isBlank(ag.nombre))
                            ? ag.nombre
                            : ("Agente #" + c.agenteId);
                } else {
                    UserLookupDAO.UserCore re = userDAO.getById(c.residenteId);
                    peerName = (re != null && !isBlank(re.nombre))
                            ? re.nombre
                            : ("Residente #" + c.residenteId);
                }

                // Último mensaje (preview en la bandeja)
                Message last = null;
                try {
                    last = msgDAO.ultimoDeConversacion(c.id); // asegúrate de tener este método en MessageDAO
                } catch (Exception ignore) {
                    /* no-op */ }

                String lastText = (last != null && last.texto != null) ? last.texto : "";
                String lastTs = null;
                if (last != null && last.creadoEn != null) {
                    lastTs = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(last.creadoEn);
                }

                arr.add(Json.createObjectBuilder()
                        .add("id", c.id)
                        .add("residenteId", c.residenteId)
                        .add("agenteId", c.agenteId)
                        .add("estado", c.estado == null ? "" : c.estado)
                        .add("peerName", peerName)
                        .add("lastText", lastText)
                        .add("lastTs", lastTs == null ? "" : lastTs)
                );
            }

            Json.createWriter(resp.getWriter()).writeArray(arr.build());
        } catch (Exception e) {
            resp.setStatus(500);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
