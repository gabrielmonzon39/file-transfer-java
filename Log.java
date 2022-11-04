import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

public class Log {
    private static final String PATH = "./Logs/";
    private static final String EXT = ".txt";
    public static final boolean END = true;

    public static void makeLogRouting (String from) {
        try {
            File folder = new File(PATH+"Routing");
            folder.mkdir();
            FileWriter writer = new FileWriter(PATH+"Routing"+"/"+"Log"+EXT, true);
            writer.write(routingMessage(from));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            ConsoleLog.printError();
        }
    }
    
    public static void makeLog (String from, String to, String file, int size, int chunk, boolean action, boolean end) {
        try {
            File folder = new File(PATH+to);
            folder.mkdir();
            FileWriter writer = new FileWriter(PATH+to+"/"+getAction(action)+EXT, true);
            if (end) {
                writer.write(complete(from, to, file, size, action));
            } else {
                writer.write(message(from, to, file, size, chunk, action));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            ConsoleLog.printError();
        }
    }

    public static String getAction (boolean action) {
        return (action) ? "Recibidos" : "Enviados";
    }

    public static String routingMessage (String from) {
        return begin() + Time.now() + "\tDe: " + from + "\n" + 
        "Mensaje \"Hello\" recibido." + "\n" + end();
    }

    public static String message (String from, String to, String file, int size, int chunk, boolean action) {
        return begin() + Time.now() + "\tDe: " + from + "   Para: " + to + "\n" + 
        "Archivo: " + file + "\n" +
        "Tamaño: " + size + " bytes\n" + 
        "Chunk " + ((action) ? "recibido" : "enviado") + ": " + (chunk+1) + "\n" + end();
    }
 
     public static String complete (String from, String to, String file, int size, boolean action) {
        return begin() + Time.now() + "\tDe: " + from + "   Para: " + to + "\n" + 
        "Archivo: " + file + " " + ((action) ? "recibido" : "enviado") + " correctamente\n" +
        "Tamaño: " + size + " bytes\n" + end();
    }

    public static String begin() {
        return "\n" + ConsoleLog.separator + "\n";
    }

    public static String end() {
        return ConsoleLog.separator +  "\n\n";
    }
}
