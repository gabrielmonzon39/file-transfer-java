import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Fowarding extends Thread{
    private static final String IP = "127.0.0.1";
    private static final int PORT_FORWARDING = 9081;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static DataOutputStream dataOutputStream2 = null;
    private static DataInputStream dataInputStream2 = null;
    private static final int RESPONSEFIELDQUANTITY = 6;
    private static final int PORT_RECEIVER = 6666;

    public static void sendRequest () throws Exception {
        dataOutputStream.writeUTF(makeRequest());
        dataOutputStream.flush();
    }

    public static String makeRequest () {
        return Messages.makeRequest("127.0.0.1", "127.0.0.1", "prueba.txt", 150);
    }

    public void run(){  
        try(ServerSocket serverSocket = new ServerSocket(PORT_RECEIVER)){
            while(true){
              Socket clientSocket = serverSocket.accept();
              dataInputStream2 = new DataInputStream(clientSocket.getInputStream());
              dataOutputStream2 = new DataOutputStream(clientSocket.getOutputStream());
              String[] params = decodeResponse2();
              System.out.println("DDDDDDDDD");
              for (String string : params) {
                System.out.println("EEEEEEEEEEEEEEE");
                System.out.println(string);
              }
              dataInputStream2.close();
              dataOutputStream2.close();
              clientSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }  
    }  

    public static String[] decodeResponse2 () throws Exception {
        String[] responseDecoded = new String[RESPONSEFIELDQUANTITY];
        String responseEncoded = dataInputStream2.readUTF();
        int from = responseEncoded.indexOf("From: ");
        int to = responseEncoded.indexOf("To: ");
        int name = responseEncoded.indexOf("Name: ");
        int data = responseEncoded.indexOf("Data: ");
        int frag = responseEncoded.indexOf("Frag: ");
        int size = responseEncoded.indexOf("Size: ");
        responseDecoded[0] = responseEncoded.substring(from + 6, to).trim();
        responseDecoded[1] = responseEncoded.substring(to + 4, name).trim();
        responseDecoded[2] = responseEncoded.substring(name + 6, data).trim();
        responseDecoded[3] = responseEncoded.substring(data + 6, frag-1);
        responseDecoded[4] = responseEncoded.substring(frag + 6, size).trim();
        responseDecoded[5] = responseEncoded.substring(size + 6).trim();

        int pos = Integer.parseInt(responseDecoded[4]);

        System.out.println(responseDecoded[0]);
        System.out.println(responseDecoded[1]);
        System.out.println(responseDecoded[2]);
        System.out.println(responseDecoded[3]);
        System.out.println(responseDecoded[4]);
        System.out.println(responseDecoded[5]);
        System.out.println("************ " + pos);
        return responseDecoded;
    }

    public static String[] decodeResponse () throws Exception {
        String[] responseDecoded = new String[RESPONSEFIELDQUANTITY];
        String responseEncoded = dataInputStream.readUTF();
        int from = responseEncoded.indexOf("From: ");
        int to = responseEncoded.indexOf("To: ");
        int name = responseEncoded.indexOf("Name: ");
        int data = responseEncoded.indexOf("Data: ");
        int frag = responseEncoded.indexOf("Frag: ");
        int size = responseEncoded.indexOf("Size: ");
        responseDecoded[0] = responseEncoded.substring(from + 6, to).trim();
        responseDecoded[1] = responseEncoded.substring(to + 4, name).trim();
        responseDecoded[2] = responseEncoded.substring(name + 6, data).trim();
        responseDecoded[3] = responseEncoded.substring(data + 6, frag-1);
        responseDecoded[4] = responseEncoded.substring(frag + 6, size).trim();
        responseDecoded[5] = responseEncoded.substring(size + 6).trim();

        int pos = Integer.parseInt(responseDecoded[4]);

        System.out.println("************ " + pos + responseDecoded[3]);
        return responseDecoded;
    }

    public static void main(String[] args) {
        
        Fowarding t1=new Fowarding();  
        t1.start();  
        /*try(Socket socket = new Socket(IP, PORT_FORWARDING)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            sendRequest();  
            decodeResponse();    
            dataInputStream.close();
            dataOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }
}
