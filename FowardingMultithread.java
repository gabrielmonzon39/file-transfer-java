import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

public class FowardingMultithread {
    private final static int PORT = 6666;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static DataOutputStream dataOutputStream2 = null;
    private static DataInputStream dataInputStream2 = null;

    public static void main(String[] args){
        ServerSocket server = null;

        try{
            server = new ServerSocket(6666);
            server.setReuseAddress(true);

            while(true){
                Socket client =  server.accept();
                System.out.println("New client connected " + client.getInetAddress().getHostAddress());
                
                ClientHandler clientSock = new ClientHandler(client);
                new Thread(clientSock).start();
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }        

    private static class ClientHandler implements Runnable{
        private final Socket socketClient;

        //Constructor para el cliente
        public ClientHandler(Socket socket){
            this.socketClient = socket;
        }

        public void run(){
                System.out.println("Hola");

                try{
                    /*InetAddress ip = InetAddress.getByName(new URL("https://c874-190-148-53-185.ngrok.io/").getHost());
                    server = new Socket(ip, PORT);
                    server.setReuseAddress(true);
                    System.out.println(ip.toString());*/

                    while(true){
                        dataInputStream = new DataInputStream(socketClient.getInputStream());
                        dataOutputStream = new DataOutputStream(socketClient.getOutputStream());
                        System.out.println("antes de ..");
                        //dataOutputStream.writeUTF("success");
                        System.out.println(dataInputStream.readUTF());
                        String destino = dataInputStream.readUTF();
                            
                        /*while ((line = br.readLine()) != null) {
                            String[] values = line.split(";");
                            if(partes[3].equals("192.128.67")){
                                System.out.println("Si jalo");
                                dataOutputStream.writeUTF("success");
                            }else{  
                                if(partes[3].equals(values[2])){
                                    Messages.makeResponse("192.128.67", values[2], partes[5], partes[7].getBytes(), Integer.parseInt(partes[9]), Integer.parseInt(partes[11]));
                                }
                            }
                            
                          }*/
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                    finally{
                        try {
                           if (dataOutputStream != null) {
                                dataOutputStream.close();
                            }
                            if (dataInputStream != null) {
                                dataInputStream.close();
                                socketClient.close();
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
        }
    }
}
