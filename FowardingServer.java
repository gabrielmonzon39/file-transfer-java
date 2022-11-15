import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.EOFException;

public class FowardingServer extends Thread {
    private static final int PORT_FORWARDING = 9081;
    private static final int PORT_APP = 6666;
    private static final int PORT_RECEIVER = 5000;
    private static final int CHUNKSIZE = 1460;
    private static final String PATH = "./RoutingTable.txt";
    private static final String[] params = {"From", "To", "Name", "Size", "EOF"};
    private static final String[] paramsFile = {"From", "To", "Name", "Data", "Frag", "Size", "EOF"};

    private static DataOutputStream dataOutputStream = null;
    public static final String separator = "--------------------------------------------------------------------";
    private static DataInputStream dataInputStream = null;
    private static long noChunks;

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
            //dataInputStream = new DataInputStream(this.newClient.getInputStream());
            //dataOutputStream = new DataOutputStream(this.newClient.getOutputStream());
            while(true){
                dataInputStream = new DataInputStream(newClient.getInputStream());
                dataOutputStream = new DataOutputStream(newClient.getOutputStream());
                
                //Leer tabla de ruteo
                readTable();
                //// OBTENER LA REQUEST
                String request;
                //try {
                    request = dataInputStream.readUTF();
                //} catch (EOFException e) {
                  //  continue;
                //}
                //System.out.println(request);
                //System.out.println(myHost);

                //// DECODIFICAR LA REQUEST
                HashMap<String, String> requestDecoded = decodeRequest(request);

                //// EVALUAR LA REQUEST
                eval(request, requestDecoded, myHost);             
                
                //dataInputStream.close();
                //dataOutputStream.close();
            }            
        } catch (EOFException e) {
            
        } catch( Exception e){
            //e.printStackTrace();
        } 
        
        finally{ 
            try { 
                if (dataOutputStream != null) { 
                    dataOutputStream.close(); 
                } 
                if (dataInputStream != null) { 
                    dataInputStream.close(); 
                    //newClient.close(); 
                } 
            } 
            catch (Exception e) { 
                //e.printStackTrace(); 
            } 
        }
    }

    public static void readTable() {
        try {  

            // Leer RoutingTable.txt
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
            //e.printStackTrace();  
        }
    }

    public static void eval(String request, HashMap<String, String> requestDecoded, String myHost) {
        //Determinar si es para nosotros
        if (requestDecoded.get("To").equals(myHost)) {
            doFileRequest(request, requestDecoded, myHost);
        } else {
            doRedirect(request, requestDecoded);
        }
    }

    public static void doFileRequest (String request, HashMap<String, String> requestDecoded, String local) {
        //System.out.println("Es transferencia de archivos.");
        String prueba;
        String Size;
        Pattern verifi = Pattern.compile("[a-z][A-Z]*:[0-9]");

        //Envio a Receiver
        try(
            Socket socket = new Socket("localhost", PORT_APP)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            dataOutputStream.writeUTF(request);
            //System.out.println("Archivo entregado a: " + local);
            
            Matcher m = verifi.matcher(requestDecoded.get("Size"));
            if(m.find()){
                Size = requestDecoded.get("Size").split(":")[1].trim();
            }else{
                Size = requestDecoded.get("Size");
            }
            
            noChunks = (Integer.parseInt(Size)%((long)CHUNKSIZE)==0L) ? Integer.parseInt(Size)/((long)CHUNKSIZE) : Integer.parseInt(Size)/((long)CHUNKSIZE)+1;
            for(int i = 0; i<(int)noChunks; i++){
                prueba = dataInputStream.readUTF();
                doRedirect2(prueba,requestDecoded.get("From"));

                //System.out.println(separator);
                //System.out.println();
            }
            
            //dataInputStream.close();
            //dataOutputStream.close();
            //socket.close();
        }
        catch (EOFException e) {
            
        } catch (Exception e){
            //e.printStackTrace();
        }
     
        finally{
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
            }
            catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    public static void doRedirect (String request, HashMap<String, String> requestDecoded) {
        String toHost = table.get(requestDecoded.get("To")).link;
        //System.out.println("Es redireccion " + toHost);
        Sender sender = new Sender(Routing.getIpFromLetter(toHost), PORT_FORWARDING, request);
        //System.out.println(toHost);
        sender.send();
        //System.out.println("Mensaje reenviado a: " + toHost);
    }

    public static void doRedirect2 (String response, String to) {
        //System.out.println(to);
        String toHost = table.get(to).link;
        //System.out.println("Es redireccion " + toHost);
        Sender sender = new Sender(Routing.getIpFromLetter(toHost), PORT_FORWARDING, response);
        //System.out.println(toHost+"ERR");

        /*try {
            dataOutputStream.writeUTF(response);
            dataOutputStream.flush();
        }catch (Exception e){
            //e.printStackTrace();
        }*/
       
        sender.send();
        //System.out.println("Mensaje reenviado a: " + toHost);
    }
    
    public static HashMap<String, String> decodeRequest (String request) throws Exception {
        HashMap<String, String> requestDecoded = new HashMap<>();
        if((request.split("\n")).length == 5){
            String[] paramsDecoded = new String[5];
            paramsDecoded =  request.split("\n");
            for (int i = 0; i < paramsDecoded.length; i++) {
                if(i == paramsDecoded.length-1){
                    requestDecoded.put(params[i], paramsDecoded[i]);
                }else{
                    requestDecoded.put(params[i], paramsDecoded[i].split(":")[1].trim());
                }
            }
            return requestDecoded;
        }else{
            String[] paramsDecoded = new String[7];
            paramsDecoded =  request.split("\n");
            for (int i = 0; i < paramsDecoded.length; i++) {
                if(i == paramsDecoded.length-1){
                    //System.out.println(paramsDecoded[i]);
                    requestDecoded.put(paramsFile[i], paramsDecoded[i]);    
                }else{
                    requestDecoded.put(paramsFile[i], paramsDecoded[i].split(":")[1].trim());
                }
            }
            return requestDecoded;
        }
    }
    
    /*public static HashMap<String, String> decodeRequest (String request) throws Exception {
        HashMap<String, String> requestDecoded = new HashMap<>();
        String[] paramsDecoded = new String[4];
        paramsDecoded =  request.split("\n");
        for (int i = 0; i < paramsDecoded.length; i++) {
            requestDecoded.put(params[i], paramsDecoded[i].split(":")[1].trim());
        }
        return requestDecoded;
    }*/
    
    public static void main(String[] args) {
        ServerSocket server = null;
        //// INIT
        ConsoleLog.printBegin();
        System.out.println("Fowarding");
        System.out.println("Escuchando en puerto: " + PORT_FORWARDING);
        ConsoleLog.printEnd();
        //readTable();
            
        //// OBTENER MI DIRECCIÓN
        Hosts hosts = new Hosts();
        String myHost = hosts.getMyAddress();

        try{
            server = new ServerSocket(PORT_FORWARDING);
            server.setReuseAddress(true);

            while(true){
                Socket client =  server.accept(); 
                //System.out.println("Cliente : " + client.getInetAddress().getHostAddress()); 
                 
                /*FowardingServer clientSock = new FowardingServer(); 
                clientSock.myHost = myHost; 
                clientSock.newClient = client; 
                clientSock.start();
                System.out.println("XD");*/
                //Socket client =  server.accept();
                //System.out.println("Nuevo cliente conectado: " + client.getInetAddress().getHostAddress());
                
                FowardingServer clientSock = new FowardingServer(client, myHost);
                new Thread(clientSock).start();
            }
        }catch(Exception e) {
            //e.printStackTrace();
        }
        finally {
            if (server != null) {
                try {
                    //System.out.println("Murio");
                    server.close();
                }
                catch (Exception e) {
                    //e.printStackTrace();
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
            //e.printStackTrace();
        }*/
        
    }

}
