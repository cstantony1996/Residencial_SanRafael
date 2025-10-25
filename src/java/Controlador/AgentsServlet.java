package Controlador;

import Chat.dao.UserLookupDAO;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@WebServlet("/api/chat/agentes")
public class AgentsServlet extends HttpServlet {

    private final UserLookupDAO userDAO = new UserLookupDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        boolean onlyOnline = "1".equals(req.getParameter("online"));

        // 1) Lista todos los guardias/agentes (según tu esquema real de BD)
        List<UserLookupDAO.UserCore> list = userDAO.listarGuardiasActivos();

        // 2) Si piden solo "en línea", intenta filtrar usando el WebSocket
        if (onlyOnline) {
            Set<Integer> onlineIds = null;
            try {
                // Llamada por reflexión para no romper compilación si aún no existe el método
                Class<?> clazz = Class.forName("Chat.endpoint.ChatEndpoint");
                java.lang.reflect.Method m = clazz.getMethod("onlineAgentIds");
                Object r = m.invoke(null);
                if (r instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<Integer> casted = (Set<Integer>) r;
                    onlineIds = casted;
                }
            } catch (Throwable ignore) {
                // Si no existe el método o falla, simplemente no filtramos
            }

            if (onlineIds != null) {
                Iterator<UserLookupDAO.UserCore> it = list.iterator();
                while (it.hasNext()) {
                    if (!onlineIds.contains(it.next().id)) {
                        it.remove();
                    }
                }
            }
        }

        // 3) Respuesta JSON
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (UserLookupDAO.UserCore u : list) {
            arr.add(Json.createObjectBuilder()
                    .add("id", u.id)
                    .add("nombre", u.nombre));
        }
        resp.getWriter().write(arr.build().toString());
    }
}
