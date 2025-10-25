package Chat.util;

import javax.json.*;
import java.io.*;
import java.math.BigDecimal;

public class JsonUtil {
    public static JsonObject parse(String raw) {
        try ( JsonReader r = Json.createReader(new StringReader(raw))) {
            return r.readObject();
        }
    }
    
    public static String stringify(JsonObject obj) {
        StringWriter sw = new StringWriter();
        try (JsonWriter w = Json.createWriter(sw)) {
            w.write(obj);
        }
        return sw.toString();
    }
    
    public static JsonObject msg(String type) {
        return Json.createObjectBuilder().add("type", type).build();
    }
}
