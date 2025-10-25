package Chat.service;

import Chat.dao.ConversationDAO;
import Chat.dao.MessageDAO;
import Chat.model.*;

public class ChatService {

        private final ConversationDAO conversationDAO = new ConversationDAO();
        private final MessageDAO messageDAO = new MessageDAO();

        public Conversation abrirConversacion(int residenteId, int agenteId) throws RuntimeException {
                if (conversationDAO.existeEntre(residenteId, agenteId)) {
                        throw new RuntimeException("Ya existe una conversacion con el usuario seleccionado");
                }
                return conversationDAO.crear(residenteId, agenteId);
        }

        /**
         * Guarda un mensaje SIN soporte de hilos. Valida que el usuario
         * pertenezca a la conversación y aplica reglas básicas.
         */
        public Message guardarMensaje(long convId,
                int senderId,
                UserRole fromRole,
                String texto) throws Exception {

                // Validaciones básicas
                if (!conversationDAO.usuarioPertenece(convId, senderId)) {
                        throw new RuntimeException("No pertenece a la conversación");
                }
                if (texto == null || (texto = texto.trim()).isEmpty()) {
                        throw new RuntimeException("Mensaje vacío");
                }
                if (texto.length() > 1000) {
                        throw new RuntimeException("Mensaje demasiado largo (max 1000)");
                }

                // Guardar mensaje (sin hilos)
                return messageDAO.insertar(convId, fromRole, texto);
        }

        /**
         * Compatibilidad con código legado que aún pase threadId. Se ignora el
         * parámetro de hilo y se delega al método sin hilos.
         */
        @Deprecated
        public Message guardarMensaje(long convId,
                int senderId,
                UserRole fromRole,
                String texto,
                Integer threadId) throws Exception {
                return guardarMensaje(convId, senderId, fromRole, texto);
        }
}
