package Chat.endpoint;

import Chat.model.*;
import Chat.service.*;
import Chat.dao.*;
import Chat.util.JsonUtil;
import Servicio.AuthService.AuthUser;

import javax.json.*;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

//esta clase es un endpoint Web
@ServerEndpoint(value = "/ws/chat", configurator = HttpSessionConfigurator.class) // llama a este configuradro para cada conexion entrante
public class ChatEndpoint {

        // Conexiones online por rol
        private static final Map<Integer, Session> onlineResidentes = new ConcurrentHashMap<>();
        private static final Map<Integer, Session> onlineAgentes = new ConcurrentHashMap<>();

        // ===== Suscripciones de presencia =====
        // userId observado -> sesiones suscritas a su presencia
        private static final Map<Integer, Set<Session>> presenceSubs = new ConcurrentHashMap<>();
        // sesión -> userId al que está suscrita (para limpiar rápido al cerrar)
        private static final Map<Session, Integer> sessionSubPeer = new ConcurrentHashMap<>();

        private final ChatService chatService = new ChatService();
        private final ConversationDAO conversationDAO = new ConversationDAO();
        private final UserLookupDAO userDAO = new UserLookupDAO();
        private final NotificationService notifier = new NotificationService();

        private AuthUser user;
        private UserRole role;

        @OnOpen
        public void onOpen(Session ws, EndpointConfig cfg) throws IOException {
                HttpSession http = (HttpSession) cfg.getUserProperties().get(HttpSession.class.getName());
                if (http == null || http.getAttribute("user") == null) {
                        ws.getBasicRemote().sendText(JsonUtil.stringify(JsonUtil.msg("NO_AUTH")));
                        ws.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "No auth"));
                        return;
                }

                this.user = (AuthUser) http.getAttribute("user");
                String rolSes = (String) http.getAttribute("rol");
                String rolUp = (rolSes == null ? "" : rolSes.trim().toUpperCase());
                this.role = ("AGENTE".equals(rolUp) || "GUARDIA".equals(rolUp) || "SEGURIDAD".equals(rolUp))
                        ? UserRole.AGENTE : UserRole.RESIDENTE;

                if (role == UserRole.AGENTE) {
                        onlineAgentes.put(user.id, ws);
                } else {
                        onlineResidentes.put(user.id, ws);
                }

                JsonObject hello = Json.createObjectBuilder()
                        .add("type", "WELCOME")
                        .add("userId", user.id)
                        .add("role", role.name())
                        .build();
                ws.getBasicRemote().sendText(JsonUtil.stringify(hello));

                // Notificar a los suscriptores que este usuario está online
                broadcastPresence(user.id, role, true);
        }

        @OnMessage
        public void onMessage(Session ws, String raw) throws Exception {
                JsonObject msg = JsonUtil.parse(raw);
                String type = msg.getString("type", "");

                switch (type) {
                        case "OPEN_CONVERSATION": {
                                // Solo RESIDENTE abre conversación con un AGENTE
                                requireRole(UserRole.RESIDENTE);
                                int agenteId = msg.getInt("agentId");

                                Conversation c = chatService.abrirConversacion(user.id, agenteId);

                                JsonObject opened = Json.createObjectBuilder()
                                        .add("type", "CONVERSATION_OPENED")
                                        .add("conversationId", c.id)
                                        .build();

                                // confirma al residente
                                send(ws, opened);

                                // avisa al agente si está online
                                Session peer = onlineAgentes.get(agenteId);
                                if (peer != null && peer.isOpen()) {
                                        peer.getBasicRemote().sendText(JsonUtil.stringify(opened));
                                }
                                break;
                        }

                        case "SEND_MESSAGE": {
                                long convId = msg.containsKey("conversationId") ? msg.getJsonNumber("conversationId").longValue() : 0L;
                                String texto = msg.getString("text", "");

                                if (convId <= 0) {
                                        throw new IllegalArgumentException("conversationId requerido");
                                }
                                // valida pertenencia
                                if (!conversationDAO.usuarioPertenece(convId, user.id)) {
                                        throw new IllegalArgumentException("No pertenece a la conversación");
                                }

                                // Guardar mensaje (sin hilos)
                                Message m = chatService.guardarMensaje(convId, user.id, role, texto);

                                // despachar al peer o enviar correo si está offline
                                dispatchToPeer(convId, m, texto);
                                break;
                        }

                        case "SUBSCRIBE_PRESENCE": {
                                int peerUserId = msg.getInt("peerUserId");

                                // quitar suscripción anterior de esta sesión (si existía)
                                Integer old = sessionSubPeer.put(ws, peerUserId);
                                if (old != null && !Objects.equals(old, peerUserId)) {
                                        Set<Session> oldSet = presenceSubs.get(old);
                                        if (oldSet != null) {
                                                oldSet.remove(ws);
                                        }
                                }

                                // agregar suscripción actual
                                presenceSubs.computeIfAbsent(peerUserId, k -> ConcurrentHashMap.newKeySet())
                                        .add(ws);

                                // enviar estado actual de inmediato
                                UserRole peerRole = (this.role == UserRole.RESIDENTE) ? UserRole.AGENTE : UserRole.RESIDENTE;
                                boolean online = (peerRole == UserRole.AGENTE)
                                        ? isAgentOnline(peerUserId)
                                        : isResidentOnline(peerUserId);
                                sendPresence(ws, peerUserId, peerRole, online);
                                break;
                        }

                        case "REPORT_INCIDENT": {
                                requireRole(UserRole.RESIDENTE);
                                String tipo = msg.getString("incidentType");
                                String desc = msg.getString("description", "");
                                String fechaStr = msg.getString("occuredAt", null);
                                if (fechaStr == null) {
                                        throw new IllegalArgumentException("Fecha/hora requerida");
                                }
                                String iso = (fechaStr.length() == 16 ? fechaStr + ":00" : fechaStr);
                                java.sql.Timestamp ts = java.sql.Timestamp.valueOf(iso.replace('T', ' '));

                                long id = new IncidentService().reportar(user.id, tipo, ts, desc);
                                send(ws, Json.createObjectBuilder().add("type", "INCIDENT_OK").add("incidentId", id).build());

                                UserLookupDAO.UserCore residente = userDAO.getById(user.id);
                                String asunto = "Reporte de incidente";
                                String cuerpo = "Se le informa que el residente " + (residente != null ? residente.nombre : "(desconocido)")
                                        + ", que vive en lote " + (residente != null ? residente.lote : "?") + ", "
                                        + (residente != null ? residente.numeroCasa : "?") + ", ha reportado un incidente, a continuación los detalles :\n"
                                        + tipo + "\n" + ts + "\n" + desc + "\n\n"
                                        + "Por favor, tomar las acciones correspondientes.";

                                for (UserLookupDAO.UserCore g : userDAO.listarGuardiasActivos()) {
                                        notifier.notificarIncidente(g.correo, asunto, cuerpo); // sin cooldown
                                }
                                break;
                        }

                        case "PING": {
                                send(ws, Json.createObjectBuilder().add("type", "PONG").build());
                                break;
                        }

                        default: {
                                send(ws, Json.createObjectBuilder()
                                        .add("type", "ERROR")
                                        .add("message", "Tipo no soportado")
                                        .build());
                                break;
                        }
                }
        }

        @OnClose
        public void onClose(Session ws, CloseReason cr) {
                if (user == null) {
                        return;
                }

                if (role == UserRole.AGENTE) {
                        onlineAgentes.remove(user.id, ws);
                } else {
                        onlineResidentes.remove(user.id, ws);
                }

                // Limpiar suscripción de presencia de esta sesión (si existía)
                Integer peer = sessionSubPeer.remove(ws);
                if (peer != null) {
                        Set<Session> set = presenceSubs.get(peer);
                        if (set != null) {
                                set.remove(ws);
                        }
                }

                // Notificar a suscriptores que este usuario está offline
                broadcastPresence(user.id, role, false);
        }

        @OnError
        public void onError(Session ws, Throwable t) {
                // log opcional
        }

        // ===== Helpers =====
        private void send(Session ws, JsonObject payload) throws IOException {
                ws.getBasicRemote().sendText(JsonUtil.stringify(payload));
        }

        private void requireRole(UserRole required) {
                if (this.role != required) {
                        throw new RuntimeException("Operación no permitida para el rol");
                }
        }

        private void dispatchToPeer(long convId, Message m, String texto) throws Exception {
                Conversation c = conversationDAO.getById(convId);
                if (c == null) {
                        throw new IllegalStateException("Conversación no encontrada");
                }

                int peerId = (role == UserRole.RESIDENTE) ? c.agenteId : c.residenteId;
                Session peer = (role == UserRole.AGENTE) ? onlineResidentes.get(peerId) : onlineAgentes.get(peerId);

                String tsIso = (m.tsIso != null && !m.tsIso.isEmpty())
                        ? m.tsIso
                        : new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date());

                JsonObject payload = Json.createObjectBuilder()
                        .add("type", "MESSAGE")
                        .add("conversationId", convId)
                        .add("fromRole", role.name())
                        .add("text", texto)
                        .add("messageId", m.id)
                        .add("ts", tsIso)
                        .build();

                // eco al remitente
                Session self = (role == UserRole.AGENTE) ? onlineAgentes.get(user.id) : onlineResidentes.get(user.id);
                if (self != null && self.isOpen()) {
                        self.getBasicRemote().sendText(JsonUtil.stringify(payload));
                }

                if (peer != null && peer.isOpen()) {
                        peer.getBasicRemote().sendText(JsonUtil.stringify(payload));
                } else {
                        // Peer offline -> correo (con cooldown por conversación)
                        UserLookupDAO.UserCore sender = userDAO.getById(user.id);
                        UserLookupDAO.UserCore destinatario = userDAO.getById(peerId);
                        String asunto = "Notificacion de mensaje";
                        String cuerpo = "El usuario " + (sender != null ? sender.nombre : ("id=" + user.id))
                                + " le ha enviado un mensaje. Favor de ingresar al apartado de consulta general "
                                + "para revisar su conversacion con este usuario.";
                        String convKey = "conv:" + convId; // clave de cooldown
                        notifier.notificarMensaje(destinatario != null ? destinatario.correo : null, asunto, cuerpo, convKey);
                }
        }

        // ===== Presencia =====
        private static boolean isAgentOnline(int id) {
                return onlineAgentes.containsKey(id);
        }

        private static boolean isResidentOnline(int id) {
                return onlineResidentes.containsKey(id);
        }

        private void sendPresence(Session s, int userId, UserRole userRole, boolean online) {
                try {
                        JsonObject payload = Json.createObjectBuilder()
                                .add("type", "PRESENCE")
                                .add("userId", userId)
                                .add("role", userRole.name())
                                .add("online", online)
                                .build();
                        s.getBasicRemote().sendText(JsonUtil.stringify(payload));
                } catch (Exception ignore) {
                }
        }

        private void broadcastPresence(int userId, UserRole userRole, boolean online) {
                Set<Session> subs = presenceSubs.get(userId);
                if (subs == null) {
                        return;
                }
                for (Session s : subs) {
                        if (s != null && s.isOpen()) {
                                sendPresence(s, userId, userRole, online);
                        }
                }
        }

        // util público (ya lo usabas)
        public static java.util.Set<Integer> onlineAgentIds() {
                return new java.util.HashSet<>(onlineAgentes.keySet());
        }
}
