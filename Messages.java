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
  
        return "From: " + from + "\n" +
               "To: " + to + "\n" +
               "Name: " + name + "\n" +
               "Data: " + hex +
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
        return "From: " + from + "\n" + 
               "Type: HELLO";
    }

    public static String ResponseInitialMessage(String from) {
        return "From: " + from + "\n" + 
               "Type: WELCOME";
    }

    public static String makeKeepAlive(String from) {
        return "From: " + from + "\n" + 
               "Type: KeepAlive";
    }

    public static String makeKevin(String from, HashMap<String, Costo> Dv) {
        String ret = "";
        ret += from + ";";
        for (Map.Entry<String, Costo> set : Dv.entrySet()) {
            ret += " " + set.getKey() + ": " + set.getValue().costo + ",";
        }
        return ret.substring(0, ret.length()-1);
    }
    
}

class Costo {
    int costo;
    String link;
    Costo (int costo, String link) {
        this.costo = costo;
        this.link = link;
    }
}