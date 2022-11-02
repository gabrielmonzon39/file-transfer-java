import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;  
public class Routing {

    static final int PORT = 9080;
    static final int infinity = 99;

    static final int defaultT = 5; // 30
    static final int defaultU = 30; // 90
    static int timerT = defaultT;
    static int timerU = defaultU;

    static HashMap<String, Costo> DvReceived;
    static HashMap<String, Costo> changed = new HashMap<>();
    public static HashMap<String, Integer> timeRemaining = new HashMap<>();

    public static ArrayList<String> vecinos = new ArrayList<>();

    static String from;

    public static void main(String[] args) throws Exception {
        //***********    Leer tiempos T y U    ***********// 
        try {
            timerT = Integer.parseInt(args[0]);
        } catch (Exception e) {}
        try {
            timerU = Integer.parseInt(args[1]);
        } catch (Exception e) {}

        //***********    Realizar inicializaciones    ***********// 
        BufferedReader br = new BufferedReader(new FileReader("config.txt"));
        String line = null;
        HashMap<String, Costo> Dv = new HashMap<String, Costo>();
        FileWriter hostsWriter = new FileWriter("./hosts.txt", true);
        hostsWriter.write("\n");
        
        //***********    Leer el archivo    ***********// 
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            for (String str : values) {
                String[] dir = str.split(":");
                Dv.put(dir[0].trim(),new Costo(Integer.parseInt(dir[1].trim()),dir[0].trim()));
                vecinos.add(dir[0].trim());
                timeRemaining.put(dir[0].trim(), timerU);
                hostsWriter.write(dir[0].trim()+"\n");
            }
        }
        br.close();
        hostsWriter.close();

        //***********    Leemos nuestra dirección    ***********// 
        Hosts myHost = new Hosts();

        //***********    Creamos la primera instancia del archivo    ***********// 
        writeToFile(Dv);
        
        DatagramSocket ds = new DatagramSocket();

        //***********    Mandamos la tabla    ***********// 
        Set<String> keys = Dv.keySet();
        for (String key : keys) {
            send(ds, Messages.makeDvSend(myHost.getMyAddress(), Dv),key);
            sendHello(ds, key, myHost);
        }

        
        //***********    Crea sockets para recibir mensajes    ***********// 
        DatagramSocket dr = new DatagramSocket(PORT);
        byte[] receive = new byte[65535];
        MyTimer.init(timerT, timerU);
        
        //***********    Esperamos las llamadas    ***********// 
        DatagramPacket DpReceive = null;
        boolean hasChange = false;
        while (true) {
            DpReceive = new DatagramPacket(receive, receive.length);
            dr.receive(DpReceive);
            String data = data(receive).toString();
            System.out.println(data);

            if (data.toLowerCase().contains("hello")) {
                sendWelcome(ds, data, myHost);
            } else {
                decode(data);
                resetTimeExceeded(from);
            }

            //***********    Hacemos el algoritmo    ***********// 
            if (DvReceived == null) continue;
            Set<String> receiveKeys = DvReceived.keySet();
            for (String key : receiveKeys) {
                if (Dv.get(key) == null) {
                    Dv.put(key, new Costo(Dv.get(from).costo + DvReceived.get(key).costo, from));
                    changed.put(key, new Costo(Dv.get(from).costo + DvReceived.get(key).costo, from));
                    hostsWriter = new FileWriter("./hosts.txt", true);
                    hostsWriter.write(key+"\n");
                    hostsWriter.close();
                    hasChange = true;
                    continue;
                }
                if (DvReceived.get(key).costo + Dv.get(from).costo < Dv.get(key).costo) {
                    hasChange = true;
                    Dv.replace(key, new Costo(DvReceived.get(key).costo + Dv.get(from).costo, from));
                    changed.put(key, new Costo(DvReceived.get(key).costo + Dv.get(from).costo, from));
                }
            }

            //***********    Escribe en el archivo el nuevo vector    ***********// 
            if (hasChange) {
                writeToFile(Dv);
                hasChange = false;
            }
            
        }
    }

    public static void resetTimeExceeded (String vecino) {
        timeRemaining.replace(vecino, timerU);
    }
 
    public static void sendTimeoutMessage () throws SocketException {
        DatagramSocket ds = new DatagramSocket();
        Hosts myHost = new Hosts();
        for (String vecino : vecinos) {
            if (changed.size() == 0) {
                send(ds, Messages.makeKeepAlive(myHost.getMyAddress()), vecino);
            } else {
                send(ds, Messages.makeDvSend(myHost.getMyAddress(), changed), vecino);
            }
        }  
        changed = new HashMap<>();
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
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 1234);
            ds.send(DpSend);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void sendHello(DatagramSocket ds, String Ip, Hosts host)  {
        byte buf[] = Messages.makeInitialMessage(host.getMyAddress()).getBytes();
        try {
            //InetAddress ip = InetAddress.getLocalHost();
            InetAddress ip = InetAddress.getByName(Ip);
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 1234);
            ds.send(DpSend);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void sendWelcome(DatagramSocket ds, String data, Hosts host)  {
        String Ip = data.split("\n")[0].split(":")[1].trim();
        byte buf[] = Messages.ResponseInitialMessage(host.getMyAddress()).getBytes();
        try {
            //InetAddress ip = InetAddress.getLocalHost();
            InetAddress ip = InetAddress.getByName(Ip);
            DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 1234);
            ds.send(DpSend);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}