import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class App_Server {

    private static final String PATH = "./Send/";
    //private static final String IP = "127.0.0.1";
    //private static final int PORT_FORWARDING = 9081;
    //private static final int PORT_SENDER = 7777;
    private static final int PORT_RECEIVER = 6666;
    private static final int PORT_R = 5000;
    private static final int CHUNKSIZE = 1460;
    private static final int REQUESTFIELDQUANTITY = 4;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static FileOutputStream fileOutputStream;
     public static final String separator = "--------------------------------------------------------------------";

    private static byte[][] chunks;
    //private static long size;

    public static void clientConnection () {

    }

    public static void receiveRequest () {
        try(ServerSocket serverSocket = new ServerSocket(PORT_RECEIVER)){
            while(true){
             //Aceptar Conexion desde el Fowarding
              Socket clientSocket = serverSocket.accept();
              dataInputStream = new DataInputStream(clientSocket.getInputStream());
              dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
              String[] params = decodeRequest();
              if(params.length == 4){
                makeResponse(params);
              }
              //dataOutputStream.writeUTF(makeResponse(params));
              //System.out.println(response);
              dataInputStream.close();
              dataOutputStream.close();
              clientSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
// Obtener los Datos del Mensaje Recibido
    public static String[] decodeRequest () throws Exception {
        String paramsEncoded = dataInputStream.readUTF();
        if(paramsEncoded.split("\n").length == 4){
            String[] paramsDecoded = new String[REQUESTFIELDQUANTITY];
            paramsDecoded =  paramsEncoded.split("\n");
            for (int i = 0; i < paramsDecoded.length; i++) {
                paramsDecoded[i] = paramsDecoded[i].split(":")[1].trim();
            }
            return paramsDecoded;    
        }else{
            String[] paramsDecoded = new String[6];
            paramsDecoded =  paramsEncoded.split("\n");
            for (int i = 0; i < paramsDecoded.length; i++) {
                paramsDecoded[i] = paramsDecoded[i].split(":")[1].trim();
            }
            try(Socket socket = new Socket("localhost", PORT_R)) {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
    
                dataOutputStream.writeUTF(paramsEncoded);
                System.out.println(separator);
                System.out.println();
                
                //dataInputStream.close();
                //dataOutputStream.close();
                //socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
            return paramsDecoded;
        }
    }
//  Responder con el Formato de Mensaje
    public static String makeResponse (String[] params) {
        String from = params[0];
        String to = params[1];
        String name = params[2];
        int size = Integer.parseInt(params[3]);
        //byte[] result;

        long noChunks = (size%((long)CHUNKSIZE)==0L) ? size/((long)CHUNKSIZE) : size/((long)CHUNKSIZE)+1;
        chunks = new byte[(int)noChunks][CHUNKSIZE];

        Random random = new Random();  
        int frag = random.nextInt((int) noChunks);   

        try {
            File file = new File(PATH+name);
            FileInputStream fileInputStream = new FileInputStream(file);

            // break file into chunks
            byte[] buffer;
            for (int i = 0; i < (int)noChunks; i++) {
                buffer = new byte[CHUNKSIZE];
                fileInputStream.read(buffer);
                chunks[i] = buffer;
            }

            dataOutputStream.writeUTF(Messages.makeResponse(from, to, name, chunks[frag].clone(), frag, size));
            dataOutputStream.flush();

            // LOGS
            ConsoleLog.printMessage(from, to, name, size, frag, ConsoleLog.SENT);
            Log.makeLog(from, to, name, size, frag, ConsoleLog.SENT, !Log.END);

            fileOutputStream.close();           
            fileInputStream.close();
        } catch (Exception e) {
            return Messages.makeError(from, to, 0);
        }
        return Messages.makeResponse(from, to, name, chunks[frag], frag, size);
    }

    public static void main(String[] args) {
        App_Server.receiveRequest();

    }
    
}