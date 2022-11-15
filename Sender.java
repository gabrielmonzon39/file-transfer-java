import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
  
public class Sender {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;

    String ip;
    int port;
    String message;

    public Sender (String ip, int port, String message) {
        this.ip = ip;
        this.port = port;
        this.message = message;
    }

    public void send() {
        try(Socket socket = new Socket(ip, port)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            
            // MANDAR MENSAJE
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();

            dataInputStream.close();
            dataOutputStream.close();
        }catch (Exception e){
            //e.printStackTrace();
        }
    }
}