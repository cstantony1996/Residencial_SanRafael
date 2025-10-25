package Chat.model;

import java.sql.Timestamp;

public class Message {

    public long id;
    public long conversacionId;

    public UserRole fromRole;   
    public String tsIso;        
    public UserRole remitenteRol; 
    public String texto;
    public Timestamp creadoEn;  
}
