package Chat.service;

import Utils.CorreoUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {
    // cooldown 5 minutos por {correo + conversación}

    private static final long COOLDOWN_MS = 5 * 60 * 1000L;
    private static final Map<String, Long> lastByKey = new ConcurrentHashMap<>();

    public void notificarMensaje(String correo, String asunto, String cuerpo, String convKey) {
        if (correo == null || correo.trim().isEmpty()) {
            return;
        }
        String key = correo + "|" + (convKey == null ? "global" : convKey);
        long now = System.currentTimeMillis();
        Long last = lastByKey.get(key);
        if (last != null && (now - last) < COOLDOWN_MS) {
            return; // suprime
        }
        lastByKey.put(key, now);
        CorreoUtil.enviarCorreo(correo, asunto, cuerpo);
    }

    // para compatibilidad con llamadas viejas (sin cooldown por conversación)
    public void notificarMensaje(String correo, String asunto, String cuerpo) {
        notificarMensaje(correo, asunto, cuerpo, null);
    }

    public void notificarIncidente(String correo, String asunto, String cuerpo) {
        if (correo == null || correo.trim().isEmpty()) {
            return;
        }
        CorreoUtil.enviarCorreo(correo, asunto, cuerpo);
    }
}
