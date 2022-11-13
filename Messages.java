import java.util.HashMap;
import java.util.Map;

public class Messages {

    public static String makeRequest (String from, String to, String name, int size) {
        return "From: " + from + "\n" +
               "To: " + to + "\n" +
               "Name: " + name + "\n" +
               "Size: " + size;
    }

    public static String makeResponse (String from, String to, String name, byte[] data, int frag, int size) {
        String hex = "";
  
        for (byte i : data) {
            hex += String.format("%02X", i);
        }
  
        return "From: " + to + "\n" +
               "To: " + from + "\n" +
               "Name: " + name + "\n" +
               "Data: " + hex + "\n"  +
               "Frag: " + frag + "\n" +
               "Size: " + size;
    }

    public static String makeError (String from, String to, int option) {
        String[] messages = {"Archivo no encontrado", "Ha ocurrido un error"};
        return "From: " + from + "\n" +
               "To: " + to + "\n" +
               "Msg: " + messages[option] ;
    }

    public static String makeInitialMessage(String from) {
        return "From:" + from + "\n" + 
               "Type:HELLO";
    }

    public static String ResponseInitialMessage(String from) {
        return "From:" + from + "\n" + 
               "Type:WELCOME";
    }

    public static String makeKeepAlive(String from) {
        return "From:" + from + "\n" + 
               "Type:KeepAlive";
    }

    public static String makeDvSend(String from, HashMap<String, Costo> Dv) {
        String ret = "From:" + from + "\n" + "Type:DV\n" + "Len:" + Dv.size() + "\n";
        for (Map.Entry<String, Costo> set : Dv.entrySet()) {
            ret += set.getKey() + ":" + set.getValue().costo + "\n";
        }
        return ret;
    }
    
}

class Costo {
    String ip;
    int costo;
    String link;
    Costo (String ip, int costo, String link) {
        this.ip = ip;
        this.costo = costo;
        this.link = link;
    }
}