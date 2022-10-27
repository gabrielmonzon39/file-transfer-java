import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.net.Socket;
import java.util.*;  
import java.io.*;
public class Routing {
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader("pruba.txt"));
    String line = null;
    HashMap<String, Costo> Dv = new HashMap<String, Costo>();
    

    while ((line = br.readLine()) != null) {
      String[] values = line.split(",");
      
      for (String str : values) {
        String[] dir = str.split(":");
        Dv.put(dir[0].trim(),new Costo(Integer.parseInt(dir[1].trim()),dir[0].trim()));
      }
      
    }
    
    br.close();
    Set<String> keys = Dv.keySet();
        for (String key : keys) {
             Messages.makeKevin("nosotros", Dv);
            broadcast( Messages.makeKevin("nosotros", Dv),key);
            
        }
  }

  static void broadcast(String dv,String Ip)  {
     DataOutputStream dataOutputStream = null;
     DataInputStream dataInputStream = null;

    try( 
        Socket socket = new Socket("localhost",9080)){
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());  
        dataOutputStream.writeUTF(dv);
        System.out.println("Se envio el Dv : " + dv);
            
        dataInputStream.close();
        dataOutputStream.close();
    }catch (Exception e){
            e.printStackTrace();
        }
        }


}



 class Messages {

    public static String makeRequest (String from, String to, String name, int size) {
        return "From: " + from + "\n" +
               "To: " + to + "\n" +
               "Name: " + name + "\n" +
               "Size: " + size;
    }

    public static String makeResponse (String from, String to, String name, byte[] data, int frag, int size) {
        String hex = "";
  
        for (byte i : data) {
            hex += String.format("%02X", i);
        }
  
        return "From: " + from + "\n" +
               "To: " + to + "\n" +
               "Name: " + name + "\n" +
               "Data: " + hex +
               "Frag: " + frag + "\n" +
               "Size: " + size;
    }

    public static String makeError (String from, String to, int option) {
        String[] messages = {"Archivo no encontrado", "Ha ocurrido un error"};
        return "From: " + from + "\n" +
               "To: " + to + "\n" +
               "Msg: " + messages[option] ;
    }

    public static String makeInitialMessage(String from) {
        return "From: " + from + "\n" + 
               "Type: HELLO";
    }

    public static String ResponseInitialMessage(String from) {
        return "From: " + from + "\n" + 
               "Type: WELCOME";
    }

    public static String makeKeepAlive(String from) {
        return "From: " + from + "\n" + 
               "Type: KeepAlive";
    }

    public static String makeKevin(String from, HashMap<String, Costo> Dv) {
        String ret = "";
        ret += from + ";";
        for (Map.Entry<String, Costo> set : Dv.entrySet()) {
            ret += " " + set.getKey() + ": " + set.getValue().costo + ",";
        }
        return ret.substring(0, ret.length()-2);
    }
    
}

class Costo {
    int costo;
    String link;
    Costo (int costo, String link) {
        this.costo = costo;
        this.link = link;
    }
}





   /*  Socket serverSocket = new Socket("localhost",9080);
     Socket clientSocket = Socket.accept();

    BufferedReader FromServer =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

while (true) {  
    // Check if there's anything to receive
    while (FromServer.ready()) {
        // receive from server
        System.out.println(FromServer.readLine());
    }
    if (inFromUser.ready()) {
        int ch = inFromUser.read();

        // write to server
        outToServer.writeChar(dv);
    }


}*/


