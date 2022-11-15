import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class App {

    private static final String PATH = "./Send/";
    //private static final String IP = "127.0.0.1";
    //private static final int PORT_FORWARDING = 9081;
    //private static final int PORT_SENDER = 7777;
    private static final int PORT_RECEIVER = 6666;
    private static final int CHUNKSIZE = 1460;
    private static final int REQUESTFIELDQUANTITY = 4;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static FileOutputStream fileOutputStream;

    private static byte[][] chunks;
    //private static long size;

    /*public static void clientConnection () {
        Scanner sc = new Scanner(System.in);

        Information filesInfo = new Information();

        Hosts hosts = new Hosts();
        String myHost = hosts.getMyAddress();

        try(Socket socket = new Socket("localhost", 9081)) {
            while(true){ 

                if (!filesInfo.hasRemainingFiles()) {
                    break;
                }

                FileData file = filesInfo.getFile();
                if (file == null) break;

                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("From:" + myHost + "\n" + "To:" + "Z" +
                                            "\nName:" + file.fileName + "\nSize:" + file.size + "\nEOF");
                
                System.out.print("Presione ENTER para solicitar otro archivo...");
                sc.nextLine();
                
            }
        }catch (Exception e){
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
    }*/

    public static void clientConnection () {
        Scanner sc = new Scanner(System.in);
        Hosts hosts = new Hosts();
        String myHost = hosts.getMyAddress();
        System.out.println("Ingrese IP: ");
        String ip = sc.nextLine();
        try(Socket socket = new Socket(ip, 4500)) {
            while(true){ 
                // Realizar Peticion  
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                System.out.println("Ingrese el destino: ");
                String To = sc.nextLine();
                System.out.println("Ingrese nombre del archivo: ");
                String Name = sc.nextLine();
                System.out.println("Ingrese tamaño del archivo: ");
                String size = sc.nextLine();
                dataOutputStream.writeUTF("From:" + myHost + "\n"+"To:"+To+"\nName:"+Name+"\nSize:"+size+ "\nEOF");

                /*if(dataInputStream.readUTF() == null){
                    break;
                }else{
                    String[] paramsRes = decodeRequest();
                    makeResponse(paramsRes);
                }*/
            }
        }catch (Exception e){
            e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
    }//*/

    public static void receiveRequest () {
        try(ServerSocket serverSocket = new ServerSocket(PORT_RECEIVER)){
            while(true){
              Socket clientSocket = serverSocket.accept();
              dataInputStream = new DataInputStream(clientSocket.getInputStream());
              dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
              String[] params = decodeRequest();
              makeResponse(params);
              //System.out.println(response);
              //dataOutputStream.writeUTF(makeResponse(params));
              dataInputStream.close();
              dataOutputStream.close();
              clientSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String[] decodeRequest () throws Exception {
        String[] paramsDecoded = new String[REQUESTFIELDQUANTITY];
        String paramsEncoded = dataInputStream.readUTF();
        paramsDecoded =  paramsEncoded.split("\n");
        for (int i = 0; i < paramsDecoded.length; i++) {
            paramsDecoded[i] = paramsDecoded[i].split(":")[1].trim();
        }
        return paramsDecoded;
    }

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
            for (int i = 0; i < (int)noChunks; i++) {
                dataOutputStream.writeUTF(Messages.makeResponse(from, to, name, chunks[i].clone(), frag, size));
                dataOutputStream.flush();
            }

            //dataOutputStream.writeUTF(Messages.makeResponse(from, to, name, chunks[frag].clone(), frag, size));
            //dataOutputStream.flush();

            ConsoleLog.printMessage(from, to, name, size, frag, ConsoleLog.SENT);
            Log.makeLog(from, to, name, size, frag, ConsoleLog.SENT, !Log.END);

            fileOutputStream.close();           
            fileInputStream.close();
        } catch (Exception e) {
            return Messages.makeError(from, to, 0);
        }
        return Messages.makeResponse(from, to, name, chunks[frag], frag, size);
    } 

    /*public static String makeResponse (String[] params) {
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

            for (int i = 0; i < (int)noChunks; i++) {
                dataOutputStream.writeUTF(Messages.makeResponse(from, to, name, chunks[i].clone(), i, size));
                dataOutputStream.flush();

                // LOGS
                ConsoleLog.printMessage(from, to, name, size, i, ConsoleLog.SENT);
                Log.makeLog(from, to, name, size, i, ConsoleLog.SENT, !Log.END);

                //System.out.println(Messages.makeResponse(from, to, name, chunks[i], i, size));
                //System.out.println(i);
            }

            fileOutputStream.close();           
            fileInputStream.close();
        } catch (Exception e) {
            return Messages.makeError(from, to, 0);
        }
        return Messages.makeResponse(from, to, name, chunks[frag], frag, size);
    } */

    public static void main(String[] args) {
        App.clientConnection();
        //App.receiveRequest();

    }
    
}

