public class ConsoleLog {
    public static final String separator = "--------------------------------------------------------------------";
    public static final boolean RECEIVED = true;
    public static final boolean SENT = false;

    public static void printRoutingMessage (String from) {
        printBegin();
        System.out.println(Time.now() + "\tDe: " + from);
        System.out.println("Mensaje \"Hello\" recibido.");
        printEnd();
    }

    public static void printMessage (String from, String to, String file, int size, int chunk, boolean action) {
        printBegin();
        System.out.println(Time.now() + "\tDe: " + from + "   Para: " + to);
        System.out.println("Archivo: " + file);
        System.out.println("Tamaño: " + size + " bytes");
        System.out.println("Chunk " + ((action) ? "recibido" : "enviado") + ": " + (chunk+1));
        printEnd();
    }

    public static void printComplete (String from, String to, String file, int size, boolean action) {
        printBegin();
        System.out.println(Time.now() + "\tDe: " + from + "   Para: " + to);
        System.out.println("Archivo: " + file + " " + ((action) ? "recibido" : "enviado") + " correctamente");
        System.out.println("Tamaño: " + size + " bytes");
        printEnd();
    }

    public static void printBegin() {
        System.out.println();
        System.out.println(separator);
    }

    public static void printEnd() {
        System.out.println(separator);
        System.out.println();
    }

    public static void printError() {
        printBegin();
        System.out.println("Ha ocurrido un error.");
        printEnd();
    }
}
