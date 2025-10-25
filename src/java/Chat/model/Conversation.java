package Chat.model;

import java.sql.Timestamp;

public class Conversation {

    public long id;
    public int residenteId;
    public int agenteId;
    public String estado; //abierta | cerrada
    public Timestamp creadoEn;
    
}
