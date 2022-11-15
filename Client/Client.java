import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;  
public class Client {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static byte[][] chunks;
    private static long size;

    public static void main(String[] args) throws IOException {

      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
      System.out.println("Ingresar la ip del servidor: ");
      String ip = bufferedReader.readLine();

      System.out.println("Ingresar el puerto del servidor: ");
      int port = Integer.parseInt(bufferedReader.readLine());

      Scanner sc= new Scanner(System.in);    //System.in is a standard input stream  
      System.out.print("Ingrese el nombre del archivo:  "); 
      String str= sc.nextLine();



        try(Socket socket = new Socket(ip, port)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            sendFile("../"+str);
            
            dataInputStream.close();
            dataOutputStream.close();
        }catch (Exception e){
            //e.printStackTrace();
        }
    }

    private static void sendFile(String path) throws Exception{
       
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        
        // send file name
        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.flush();
        System.out.println("Primer Paquete: " + file.getName());

        // send file size
        dataOutputStream.writeLong(file.length());
        System.out.println("Segundo Paquete: " + file.length());
        size = file.length();
        long noChunks = (size%((long)512)==0L) ? size/((long)512) : size/((long)512)+1;
        chunks = new byte[(int)noChunks][512];

        // break file into chunks
        byte[] buffer;
        for (int i = 0; i < (int)noChunks; i++) {
            buffer = new byte[512];
            fileInputStream.read(buffer);
            chunks[i] = buffer;
        }
        System.out.println("Chunks: " + noChunks);
        int cont = (int)(noChunks);
        // send chunks to server
        for (int i = 0; (float)i < (float)((int)noChunks/2); i++) {
            
            byte[] id = ByteBuffer.allocate(4).putInt(i+1).array();
            byte[] result = new byte[id.length + chunks[i].length];
            System.arraycopy(id, 0, result, 0, id.length);  
            System.arraycopy(chunks[i], 0, result, id.length, chunks[i].length);  
            System.out.println("Paquete :" + (i+1));
            dataOutputStream.write(result);
            dataOutputStream.flush();
            id = ByteBuffer.allocate(4).putInt(chunks.length-i).array();
            result = new byte[id.length + chunks[chunks.length-1-i].length];
            System.arraycopy(id, 0, result, 0, id.length);  
            System.arraycopy(chunks[chunks.length-1-i], 0, result, id.length, chunks[chunks.length-1-i].length);

            
            System.out.println("Paquete :" + ((cont)-i));
        


            dataOutputStream.write(result);
            dataOutputStream.flush();
        }

        fileInputStream.close();
    }
}