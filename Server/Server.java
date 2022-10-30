import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static FileOutputStream fileOutputStream;
    private static byte[][] chunks;
    private static long size;

    public static void main(String[] args) {
      // get input from user terminal using system.in
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      
        try(ServerSocket serverSocket = new ServerSocket(5000)){
            while(!input.equals("exit")){
              System.out.println("Esperando Conexion...");
              Socket clientSocket = serverSocket.accept();
              dataInputStream = new DataInputStream(clientSocket.getInputStream());
              dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

              System.out.println("Esperando Archivo...");
              receiveData();
              System.out.println("Archivo Recibido");

              System.out.println("Presione Enter para continuar o escriba 'exit' para salir");
              input = bufferedReader.readLine();
              dataInputStream.close();
              dataOutputStream.close();
              clientSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void receiveData() throws Exception{
        // read file name
        String name = dataInputStream.readUTF();
        fileOutputStream = new FileOutputStream(name);

        // read file size
        size = dataInputStream.readLong();   
        long noChunks = (size%((long)516)==0L) ? size/((long)516) : size/((long)516)+1;
        chunks = new byte[(int)noChunks][516];

        // read file chunks
        int bytes = 0;
        int i = 0;
        byte[] buffer = new byte[516];


        for (int l = 0; (float)l < (float)((int)noChunks/2); l++) {
            bytes = dataInputStream.read(buffer);
            chunks[l] = buffer.clone();
            bytes = dataInputStream.read(buffer);
            chunks[chunks.length-1-l] = buffer.clone(); 
            size -= bytes;    
        }
        
        // write the result to the file
        for (int j = 0; j < chunks.length; j++) {
            for (int k = 0; k < chunks[j].length; k++) {
                if (k <= 3) continue;
                if (chunks[j][k] != 0)
                    fileOutputStream.write(chunks[j][k]);
            }
        }

        fileOutputStream.close();
    }
}