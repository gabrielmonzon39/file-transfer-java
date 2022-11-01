import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.net.Socket;
import java.util.*;  
import java.io.*;
public class Routing {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("config.txt"));
        String line = null;
        HashMap<String, Costo> Dv = new HashMap<String, Costo>();
        
        //***********    Leer el archivo    ***********// 
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


