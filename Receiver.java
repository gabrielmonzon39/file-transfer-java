import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.io.EOFException;


public class Receiver {
    private static final String PATH = "./Received/";
    private static final String IP = "127.0.0.1";
    private static final int PORT_RECEIVER = 5000;
    private static final int CHUNKSIZE = 1460;
    private static final int RESPONSEFIELDQUANTITY = 6;

    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static FileOutputStream fileOutputStream;

    private static byte[][] chunks;
    private static boolean[] readyChunks;
    private static long noChunks;
    
    private static boolean firstTime = true;
    private static boolean complete = false;

    public static void reset () {
        complete = false;
        firstTime = true;
    }

    public static void sendRequest (String from, String to, String file, int size) throws Exception {
        dataOutputStream.writeUTF(makeRequest(from, to, file, size));
        dataOutputStream.flush();
    }

    public static String makeRequest (String from, String to, String file, int size) {
        return Messages.makeRequest(from, to, file, size);
    }

    public static byte[] parseByte(String data) {
        byte[] ans = new byte[data.length() / 2];
        for (int i = 0; i < ans.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(data.substring(index, index + 2), 16);
            ans[i] = (byte)val;
        }
        return ans;
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
        responseDecoded[3] = responseEncoded.substring(data + 6, frag);
        responseDecoded[4] = responseEncoded.substring(frag + 6, size).trim();
        responseDecoded[5] = responseEncoded.substring(size + 6).trim(); 
        int pos = Integer.parseInt(responseDecoded[4]);
        int _size = Integer.parseInt(responseDecoded[5]);


        //System.out.println("**** " + responseDecoded[3] + " ****");
        //System.out.println("--->" + responseDecoded[3].getBytes().length + "<---");
        //System.out.println("--->" + new String(responseDecoded[3].getBytes()) + "<---");
        //System.out.println("-----------------------------------------");

        if (firstTime) {
            firstTime = !firstTime;
            noChunks = (_size%((long)CHUNKSIZE)==0L) ? _size/((long)CHUNKSIZE) : _size/((long)CHUNKSIZE)+1;
            chunks = new byte[(int)noChunks][CHUNKSIZE];
            readyChunks = new boolean[(int) noChunks];

            //System.out.println("------->  " + _size + " / " + CHUNKSIZE);
            //System.out.println("--->  " + noChunks);
            //System.out.println("¿¿¿¿¿¿¿¿¿" + responseDecoded[3] + "?????");

            for (int i = 0; i < readyChunks.length; i++) {
                readyChunks[i] = false;
            }

            readyChunks[pos] = true;
            chunks[pos] = parseByte(responseDecoded[3]);
        }

        if (!readyChunks[pos]) {
            readyChunks[pos] = true;
            chunks[pos] = parseByte(responseDecoded[3]);
        }

        // LOGS
        ConsoleLog.printMessage(responseDecoded[0], responseDecoded[1], responseDecoded[2], _size, pos, ConsoleLog.RECEIVED);
        Log.makeLog(responseDecoded[0], responseDecoded[1], responseDecoded[2], _size, pos, ConsoleLog.RECEIVED, !Log.END);

        for (int i = 0; i < readyChunks.length; i++) {
            if (!readyChunks[i]) return null;
        }

        fileOutputStream = new FileOutputStream(PATH + responseDecoded[2]);

        // FINAL LOG
        ConsoleLog.printComplete(responseDecoded[0], responseDecoded[1], responseDecoded[2], _size, ConsoleLog.RECEIVED);
        Log.makeLog(responseDecoded[0], responseDecoded[1], responseDecoded[2], _size, pos, ConsoleLog.RECEIVED, Log.END);
        
        // write the result to the file
        int co = 0;
        boolean stop = false;
        for (int j = 0; j < chunks.length; j++) {;
            //System.out.println(chunks[j].length);
            for (int k = 0; k < chunks[j].length; k++) {
                if (co >= _size) {
                    stop = true;
                    break;
                }
                fileOutputStream.write(chunks[j][k]);
                co++;
            }
            if (stop) break;
        }


        fileOutputStream.close();

        complete = true;
        return responseDecoded;
    }

    public static void main(String[] args) {
         try(ServerSocket socket = new ServerSocket(PORT_RECEIVER)) {
                while(true){ 
                    Socket clientsocket = socket.accept();
                    dataInputStream = new DataInputStream(clientsocket.getInputStream());
                    dataOutputStream = new DataOutputStream(clientsocket.getOutputStream());
                    decodeResponse();
             
                    //dataInputStream.close();
                    //dataOutputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
       

    }

    
}
