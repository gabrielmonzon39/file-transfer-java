import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;  
public class Routing {

    static HashMap<String, Costo> DvReceived;
    static String from;
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

        //***********    Leemos nuestra dirección    ***********// 
        Hosts myHost = new Hosts();

        //***********    Creamos la primera instancia del archivo    ***********// 
        writeToFile(Dv);
        
        DatagramSocket ds = new DatagramSocket();

        //***********    Mandamos la tabla    ***********// 
        Set<String> keys = Dv.keySet();
        for (String key : keys) {
            send(ds, Messages.makeDvSend(myHost.getMyAddress(), Dv),key);
        }

        
        //***********    Crea sockets para recibir mensajes    ***********// 
        DatagramSocket dr = new DatagramSocket(9080);
        byte[] receive = new byte[65535];
        
        //***********    Esperamos las llamadas    ***********// 
        DatagramPacket DpReceive = null;
        boolean hasChange = false;
        while (true) {
            DpReceive = new DatagramPacket(receive, receive.length);
            dr.receive(DpReceive);
            String data = data(receive).toString();
            decode(data);
            //***********    Hacemos el algoritmo    ***********// 
            Set<String> receiveKeys = DvReceived.keySet();
            for (String key : receiveKeys) {
                try {
                    Dv.get(key);
                } catch (Exception e) {
                    Dv.put(key, DvReceived.get(key));
                    hasChange = true;
                    continue;
                }
                if (DvReceived.get(key).costo < Dv.get(key).costo) {
                    hasChange = true;
                    Dv.replace(key, new Costo(DvReceived.get(key).costo, from));
                }
            }
            if (hasChange) {
                writeToFile(Dv);
                Set<String> DvKeys = Dv.keySet();
                for (String key : DvKeys) {
                    send(ds, Messages.makeDvSend(myHost.getMyAddress(), Dv),key);
                }
                hasChange = false;
            }
        }
    }

    public static void writeToFile (HashMap<String, Costo> Dv) {
        String outputFilePath = "./RoutingTable.txt";
        File file = new File(outputFilePath);

        if (file.exists()) {
            file.delete();
        }
        try {
            FileWriter writer = new FileWriter(outputFilePath, true);
            Set<String> keys = Dv.keySet();
            for (String key : keys) {
                writer.write(key + ";" + Dv.get(key).costo + ";" + Dv.get(key).link + "\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void decode (String data) {
        String[] dataDecoded = data.split("\n");
        from = dataDecoded[0].split(":")[1].trim();
        int size = Integer.parseInt(dataDecoded[2].split(":")[1].trim());
        DvReceived = new HashMap<>();

        int offset = 3;
        for (int i = 0; i < size; i++) {
            String[] entry = dataDecoded[i+offset].split(":");
            DvReceived.put(entry[0].trim(), new Costo(Integer.parseInt(entry[1].trim()), null));
        }
    }

    public static StringBuilder data(byte[] a) {
        if (a == null) return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0) {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

    static void send(DatagramSocket ds, String dv, String Ip)  {
        byte buf[] = dv.getBytes();
        try {
            //InetAddress ip = InetAddress.getLocalHost();
            InetAddress ip = InetAddress.getByName(Ip);
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 9080);
            ds.send(DpSend);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}