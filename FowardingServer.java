import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class FowardingServer extends Thread {
    private static final int PORT_FORWARDING = 9081;
    private static final String PATH = "./RoutingTable.txt";
    private static final String[] params = {"From", "To", "Name", "Size"};

    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;

    static HashMap<String, Costo> table;

    public static void readTable() {
        try {  
            FileInputStream fis = new FileInputStream(PATH);       
            Scanner scanLine = new Scanner(fis);
            table = new HashMap<>();
            while (scanLine.hasNextLine()) {  
                String[] values = scanLine.nextLine().split(";");
                table.put(values[0].trim(), 
                new Costo(Routing.getIpFromLetter(values[0].trim()), Integer.parseInt(values[1].trim()), values[2].trim()));
            }  
            scanLine.close(); 
        } catch(IOException e) {  
            e.printStackTrace();  
        }
    }

    public static void eval(String request, HashMap<String, String> requestDecoded, String myHost) {
        if (requestDecoded.get("To").equals(myHost)) {
            doFileRequest(request, requestDecoded);
        } else {
            doRedirect(request, requestDecoded);
        }
    }

    public static void doFileRequest (String request, HashMap<String, String> requestDecoded) {
        System.out.println("Es transferencia de archivos.");
    }

    public static void doRedirect (String request, HashMap<String, String> requestDecoded) {
        String toHost = table.get(requestDecoded.get("To")).link;
        System.out.println("Es redirección");
        Sender sender = new Sender(toHost, PORT_FORWARDING, request);
        sender.send();
    }
    
    public static HashMap<String, String> decodeRequest (String request) throws Exception {
        HashMap<String, String> requestDecoded = new HashMap<>();
        String[] paramsDecoded = new String[4];
        paramsDecoded =  request.split("\n");
        for (int i = 0; i < paramsDecoded.length; i++) {
            requestDecoded.put(params[i], paramsDecoded[i].split(":")[1].trim());
        }
        return requestDecoded;
    }
    
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PORT_FORWARDING)){
            //// INIT
            readTable();
            
            //// OBTENER MI DIRECCIÓN
            Hosts hosts = new Hosts();
            String myHost = hosts.getMyAddress();
            
            Socket clientSocket = serverSocket.accept();
            while(true){
                dataInputStream = new DataInputStream(clientSocket.getInputStream());
                dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                
                //// OBTENER LA REQUEST
                String request = dataInputStream.readUTF();

                //// DECODIFICAR LA REQUEST
                HashMap<String, String> requestDecoded = decodeRequest(request);

                //// EVALUAR LA REQUEST
                eval(request, requestDecoded, myHost);             
                
                dataInputStream.close();
                dataOutputStream.close();
            }
            //clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        
    }

}
