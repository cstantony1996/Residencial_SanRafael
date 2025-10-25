package Chat.endpoint;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.*;
        
public class HttpSessionConfigurator extends ServerEndpointConfig.Configurator { //una clase de extension del API JSR 356 WEBSOCKET
    
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
    }
    
}
