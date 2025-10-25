package Chat.service;

import Chat.dao.IncidentDAO;
import java.sql.Timestamp;

public class IncidentService {
    
    private final IncidentDAO dao = new IncidentDAO();
    
    public long reportar(int residenteId, String tipo, Timestamp fechaHora, String descripcion) throws Exception {
        
        if (descripcion == null || descripcion.trim().isEmpty()) throw new IllegalArgumentException("Descripción requeria");
        
        if (descripcion.length() > 200) throw new IllegalArgumentException("Máximo 200 caracteres");
        
        if (tipo == null || tipo.trim().isEmpty()) throw new IllegalArgumentException("Tipo requerido");
        
        return dao.crear(residenteId, tipo, fechaHora, descripcion.trim());
    }
    
}
