mport java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class FowardingServer_Receiver extends Thread {
    private static final int PORT_FORWARDING = 9081;
    private static final int PORT_APP = 6666;
    private static final String PATH = "./RoutingTable.txt";
    private static final String[] params = {"From", "To", "Name", "Size"};

    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;

    static HashMap<String, Costo> table;
    private Socket newClient;
    private String myHost;

    public FowardingServer(Socket newClient, String myHost){
        this.newClient = newClient;
        this.myHost = myHost;
    }

    //@Override
    public void run(){
        try {
            while(true){
                dataInputStream = new DataInputStream(newClient.getInputStream());
                dataOutputStream = new DataOutputStream(newClient.getOutputStream());
                
                //Leer tabla de ruteo
                readTable();
                //// OBTENER LA REQUEST
                String request = dataInputStream.readUTF();  
                System.out.println(request);

                //// DECODIFICAR LA REQUEST
                HashMap<String, String> requestDecoded = decodeRequest(request);

                //// EVALUAR LA REQUEST
                eval(request, requestDecoded, myHost);             
                
                //dataInputStream.close();
                //dataOutputStream.close();
            }            
        } catch (Exception e) {
            e.printStackTrace();
        }

        finally{ 
                try { 
                    if (dataOutputStream != null) { 
                        dataOutputStream.close(); 
                    } 
                    if (dataInputStream != null) { 
                        dataInputStream.close(); 
                        newClient.close(); 
                    } 
                } 
                catch (Exception e) { 
                    e.printStackTrace(); 
                } 
            }
    }

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
            doFileRequest(request, requestDecoded, myHost);
        } else {
            doRedirect(request, requestDecoded);
        }
    }

    public static void doFileRequest (String request, HashMap<String, String> requestDecoded, String local) {
        System.out.println("Es transferencia de archivos.");
        try(Socket socket = new Socket("localhost", PORT_APP)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            dataOutputStream.writeUTF(request);
            System.out.println("Archivo entregado a: " + local);
            
            //dataInputStream.close();
            //dataOutputStream.close();
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void doRedirect (String request, HashMap<String, String> requestDecoded) {
        String toHost = table.get(requestDecoded.get("To")).link;
        System.out.println("Es redirección");
        Sender sender = new Sender(toHost, PORT_FORWARDING, request);
        sender.send();
        System.out.println("Mensaje reenviado a: " + toHost);
    }
    
    public static HashMap<String, String> decodeRequest (String request) throws Exception {
        HashMap<String, String> requestDecoded = new HashMap<>();
        String[] paramsDecoded = new String[4];
        paramsDecoded =  request.split("\n");
        System.out.println(paramsDecoded.length);
        for (int i = 0; i < paramsDecoded.length; i++) {
            requestDecoded.put(params[i], paramsDecoded[i].split(":")[1].trim());
        }
        return requestDecoded;
    }
    
    public static void main(String[] args) {
        ServerSocket server = null;
        //// INIT
        System.out.println("Fowarding");
        System.out.println("Escuchando en puerto: " + PORT_FORWARDING);
        //readTable();
            
        //// OBTENER MI DIRECCIÓN
        Hosts hosts = new Hosts();
        String myHost = hosts.getMyAddress();

        try{
            server = new ServerSocket(PORT_FORWARDING);
            server.setReuseAddress(true);

            while(true){
                Socket client =  server.accept();
                System.out.println("Nuevo clienta conectado: " + client.getInetAddress().getHostAddress());
                
                FowardingServer clientSock = new FowardingServer(client, myHost);
                new Thread(clientSock).start();
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        /*try(ServerSocket serverSocket = new ServerSocket(PORT_FORWARDING)){
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
        }*/
        
    }

}