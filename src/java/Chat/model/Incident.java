package Chat.model;

import java.sql.Timestamp;

public class Incident {
    
    public long id;
    public int residenteId;
    public String tipo; //catalogo Cu
    public Timestamp fechaHora; // cuando ocurrio
    public String descripcion; //<= 200
    public Timestamp creadoEn;
    
}
