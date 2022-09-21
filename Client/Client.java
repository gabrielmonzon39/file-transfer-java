import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Client {
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static byte[][] chunks;
    private static long size;

    public static void main(String[] args) {
        try(Socket socket = new Socket("localhost",5000)) {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            sendFile("../prueba.txt");
            
            dataInputStream.close();
            dataOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void sendFile(String path) throws Exception{
       
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        
        // send file name
        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.flush();

        // send file size
        dataOutputStream.writeLong(file.length());
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

        // send chunks to server
        for (int i = 0; (float)i < (float)((int)noChunks/2); i++) {
            byte[] id = ByteBuffer.allocate(4).putInt(i+1).array();
            byte[] result = new byte[id.length + chunks[i].length];
            System.arraycopy(id, 0, result, 0, id.length);  
            System.arraycopy(chunks[i], 0, result, id.length, chunks[i].length);  
            dataOutputStream.write(result);
            dataOutputStream.flush();
            id = ByteBuffer.allocate(4).putInt(chunks.length-i).array();
            result = new byte[id.length + chunks[chunks.length-1-i].length];
            System.arraycopy(id, 0, result, 0, id.length);  
            System.arraycopy(chunks[chunks.length-1-i], 0, result, id.length, chunks[chunks.length-1-i].length);
            dataOutputStream.write(result);
            dataOutputStream.flush();
        }

        fileInputStream.close();
    }
}